package org.udaan.solution;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.problem.api.ProblemFacade;
import org.udaan.problem.api.ProblemRef;
import org.udaan.shared.web.ApiException;
import org.udaan.shared.web.NotFoundException;
import org.udaan.solution.api.SolutionFacade;
import org.udaan.solution.api.SolutionRef;

@Service
class SolutionService implements SolutionFacade {

    static final Set<SolutionStatus> VISIBLE = EnumSet.of(SolutionStatus.PUBLISHED, SolutionStatus.ACCEPTED);
    static final int MAX_PER_USER_PER_DAY = 20;

    private final SolutionRepository solutions;
    private final ProblemFacade problems;
    private final IdentityFacade identity;
    private final JdbcClient jdbc;
    private final org.springframework.context.ApplicationEventPublisher events;

    SolutionService(SolutionRepository solutions, ProblemFacade problems, IdentityFacade identity, JdbcClient jdbc,
                    org.springframework.context.ApplicationEventPublisher events) {
        this.events = events;
        this.solutions = solutions;
        this.problems = problems;
        this.identity = identity;
        this.jdbc = jdbc;
    }

    @Transactional
    SolutionView propose(String problemRef, UUID proposerId, Solution.Draft draft) {
        ProblemRef problem = problems.requirePublic(problemRef);
        if (!problems.acceptsSolutions(problem)) {
            throw new ApiException(HttpStatus.CONFLICT, "PROBLEM_CLOSED_TO_SOLUTIONS",
                    "This problem is no longer taking new solutions");
        }
        long recent = jdbc.sql("SELECT count(*) FROM solutions WHERE proposer_id = :u AND created_at > :since")
                .param("u", proposerId).param("since", java.sql.Timestamp.from(Instant.now().minus(Duration.ofDays(1))))
                .query(Long.class).single();
        if (recent >= MAX_PER_USER_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SOLUTION_RATE_LIMITED",
                    "You've proposed a lot of solutions today. Please try again tomorrow.");
        }
        Solution solution = solutions.save(Solution.propose(problem.id(), proposerId, draft));
        problems.solutionProposed(problem.id());
        events.publishEvent(new org.udaan.solution.api.SolutionProposed(problem.id(), problem.publicCode(), problem.title(),
                problem.submitterId(), solution.getId(), solution.getTitle(), proposerId));
        return view(solution, problems.find(problem.id()).orElse(problem), proposerId);
    }

    @Transactional(readOnly = true)
    List<SolutionView> forProblem(String problemRef, UUID viewerId) {
        ProblemRef problem = problems.requirePublic(problemRef);
        return solutions.findByProblemIdAndStatusInOrderByCreatedAtAsc(problem.id(), VISIBLE).stream()
                .map(s -> view(s, problem, viewerId))
                .sorted((a, b) -> Boolean.compare(b.status() == SolutionStatus.ACCEPTED, a.status() == SolutionStatus.ACCEPTED))
                .toList();
    }

    @Transactional(readOnly = true)
    SolutionView get(UUID id, UUID viewerId) {
        Solution solution = visible(id);
        return view(solution, publicProblemOf(solution), viewerId);
    }

    @Transactional(readOnly = true)
    List<SolutionView> recent(int page, int size, UUID viewerId) {
        return solutions.findByStatusInOrderByCreatedAtDesc(VISIBLE, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50)))
                .stream()
                .flatMap(s -> problems.find(s.getProblemId())
                        .filter(p -> isPublicStatus(p.status()))
                        .map(p -> view(s, p, viewerId)).stream())
                .toList();
    }

    @Transactional(readOnly = true)
    List<SolutionView> mine(UUID userId) {
        return solutions.findByProposerIdOrderByCreatedAtDesc(userId).stream()
                .flatMap(s -> problems.find(s.getProblemId()).map(p -> view(s, p, userId)).stream())
                .toList();
    }

    @Transactional
    SolutionView accept(UUID id, UUID actorId) {
        Solution solution = visible(id);
        ProblemRef problem = publicProblemOf(solution);
        if (!problem.submitterId().equals(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PROBLEM_OWNER", "Only the person who raised the problem can accept a solution");
        }
        if (solution.getStatus() == SolutionStatus.ACCEPTED) {
            return view(solution, problem, actorId);
        }
        if (!canAccept(problem) || solutions.existsByProblemIdAndStatus(problem.id(), SolutionStatus.ACCEPTED)) {
            throw new ApiException(HttpStatus.CONFLICT, "SOLUTION_ALREADY_ACCEPTED", "A solution has already been accepted for this problem");
        }
        solution.accept();
        try {
            solutions.saveAndFlush(solution);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "SOLUTION_ALREADY_ACCEPTED", "A solution has already been accepted for this problem");
        }
        problems.solutionAccepted(problem.id(), actorId, solution.getId(), solution.getProposerId(), solution.getTitle());
        events.publishEvent(new org.udaan.solution.api.SolutionAccepted(problem.id(), problem.publicCode(), problem.title(),
                solution.getId(), solution.getTitle(), solution.getProposerId(), actorId));
        return view(solution, problems.find(problem.id()).orElse(problem), actorId);
    }

    @Transactional
    SolutionView withdraw(UUID id, UUID actorId) {
        Solution solution = visible(id);
        if (!solution.getProposerId().equals(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PROPOSER", "Only the proposer can withdraw this solution");
        }
        if (solution.getStatus() == SolutionStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT, "SOLUTION_ACCEPTED", "An accepted solution can't be withdrawn. Contact the problem owner.");
        }
        solution.withdraw();
        problems.solutionWithdrawn(solution.getProblemId());
        return view(solution, publicProblemOf(solution), actorId);
    }

    record Endorsement(boolean active, int count) {
    }

    @Transactional
    Endorsement endorse(UUID id, UUID userId, boolean add) {
        Solution solution = visible(id);
        if (add && solution.getProposerId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_ENDORSE_OWN", "You can't endorse your own solution");
        }
        String change = add
                ? "INSERT INTO solution_endorsements (solution_id, user_id) VALUES (:s, :u) ON CONFLICT DO NOTHING"
                : "DELETE FROM solution_endorsements WHERE solution_id = :s AND user_id = :u";
        int changed = jdbc.sql(change).param("s", id).param("u", userId).update();
        int count = changed == 1
                ? jdbc.sql("UPDATE solutions SET endorsement_count = GREATEST(0, endorsement_count " + (add ? "+ 1" : "- 1")
                        + ") WHERE id = :s RETURNING endorsement_count").param("s", id).query(Integer.class).single()
                : jdbc.sql("SELECT endorsement_count FROM solutions WHERE id = :s").param("s", id).query(Integer.class).single();
        return new Endorsement(add, count);
    }

    @Override
    @Transactional(readOnly = true)
    public SolutionRef requireVisible(UUID solutionId) {
        Solution s = visible(solutionId);
        publicProblemOf(s);
        return new SolutionRef(s.getId(), s.getProblemId(), s.getProposerId(), s.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolutionRef> find(UUID solutionId) {
        return solutions.findById(solutionId).map(s -> new SolutionRef(s.getId(), s.getProblemId(), s.getProposerId(), s.getTitle()));
    }

    private Solution visible(UUID id) {
        return solutions.findById(id).filter(s -> VISIBLE.contains(s.getStatus()))
                .orElseThrow(() -> new NotFoundException("SOLUTION_NOT_FOUND", "This solution does not exist or was withdrawn"));
    }

    private ProblemRef publicProblemOf(Solution s) {
        return problems.requirePublic(s.getProblemId().toString());
    }

    private static boolean isPublicStatus(String status) {
        return !"SUBMITTED".equals(status) && !"UNDER_REVIEW".equals(status);
    }

    private static boolean canAccept(ProblemRef problem) {
        return "OPEN_FOR_SOLUTIONS".equals(problem.status()) || "SOLUTION_PROPOSED".equals(problem.status())
                || "VERIFIED".equals(problem.status());
    }

    private SolutionView view(Solution s, ProblemRef problem, UUID viewerId) {
        String proposerName = identity.findUser(s.getProposerId()).map(u -> u.displayName()).orElse("Udaan member");
        boolean endorsed = viewerId != null && jdbc.sql(
                        "SELECT EXISTS (SELECT 1 FROM solution_endorsements WHERE solution_id = :s AND user_id = :u)")
                .param("s", s.getId()).param("u", viewerId).query(Boolean.class).single();
        int endorsements = jdbc.sql("SELECT endorsement_count FROM solutions WHERE id = :s").param("s", s.getId())
                .query(Integer.class).optional().orElse(s.getEndorsementCount());
        boolean owner = problem.submitterId().equals(viewerId);
        return new SolutionView(s.getId(),
                new SolutionView.ProblemLink(problem.id(), problem.publicCode(), problem.title(), problem.status()),
                s.getTitle(), s.getSummary(), s.getApproach(), s.getEstimatedCostInr(), s.getTimelineText(),
                s.getResourcesRequired(), s.getSkills(), s.getStatus(),
                new SolutionView.Person(s.getProposerId(), proposerName),
                endorsements, endorsed, s.getProposerId().equals(viewerId), owner,
                owner && s.getStatus() == SolutionStatus.PUBLISHED && canAccept(problem),
                s.getCreatedAt());
    }
}
