package org.udaan.contribution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.identity.api.UserSummary;
import org.udaan.problem.api.ProblemFacade;
import org.udaan.problem.api.ProblemRef;
import org.udaan.shared.web.ApiException;
import org.udaan.shared.web.NotFoundException;
import org.udaan.solution.api.SolutionFacade;
import org.udaan.solution.api.SolutionRef;

@Service
class ContributionService {

    private final ContributionRepository contributions;
    private final ProblemFacade problems;
    private final SolutionFacade solutions;
    private final IdentityFacade identity;
    private final org.springframework.context.ApplicationEventPublisher events;

    ContributionService(ContributionRepository contributions, ProblemFacade problems, SolutionFacade solutions,
                        IdentityFacade identity, org.springframework.context.ApplicationEventPublisher events) {
        this.events = events;
        this.contributions = contributions;
        this.problems = problems;
        this.solutions = solutions;
        this.identity = identity;
    }

    /** Creates an offer, or updates the person's existing offer on the same problem/solution. */
    @Transactional
    ContributionView offer(String problemRef, UUID userId, UUID solutionId, Set<ContributionType> types, String message,
                           boolean shareContact) {
        ProblemRef problem = problems.requirePublic(problemRef);
        SolutionRef solution = null;
        if (solutionId != null) {
            solution = solutions.requireVisible(solutionId);
            if (!solution.problemId().equals(problem.id())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SOLUTION_NOT_ON_PROBLEM", "That solution belongs to a different problem");
            }
        }
        Optional<Contribution> existing = solutionId == null
                ? contributions.activeOfferOnProblem(problem.id(), userId)
                : contributions.activeOfferOnSolution(problem.id(), userId, solutionId);
        Contribution c = existing.orElseGet(() -> Contribution.offer(problem.id(), solutionId, userId));
        c.update(types, message, shareContact);
        contributions.save(c);
        if (existing.isEmpty()) {
            events.publishEvent(new org.udaan.contribution.api.HelpOffered(problem.id(), problem.publicCode(), problem.title(),
                    problem.submitterId(), solution == null ? null : solution.proposerId(), userId,
                    types.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())));
        }
        return view(c, problem, solution, userId, new HashMap<>());
    }

    @Transactional(readOnly = true)
    List<ContributionView> forProblem(String problemRef, UUID viewerId) {
        ProblemRef problem = problems.requirePublic(problemRef);
        Map<UUID, Optional<UserSummary>> users = new HashMap<>();
        return contributions.activeForProblem(problem.id()).stream()
                .map(c -> view(c, problem, c.getSolutionId() == null ? null : solutions.find(c.getSolutionId()).orElse(null), viewerId, users))
                .toList();
    }

    @Transactional(readOnly = true)
    List<ContributionView> mine(UUID userId) {
        Map<UUID, Optional<UserSummary>> users = new HashMap<>();
        return contributions.activeForUser(userId).stream()
                .flatMap(c -> problems.find(c.getProblemId())
                        .map(p -> view(c, p, c.getSolutionId() == null ? null : solutions.find(c.getSolutionId()).orElse(null), userId, users))
                        .stream())
                .toList();
    }

    @Transactional
    void withdraw(UUID contributionId, UUID userId) {
        Contribution c = contributions.findById(contributionId).filter(Contribution::isActive)
                .orElseThrow(() -> new NotFoundException("CONTRIBUTION_NOT_FOUND", "Offer not found"));
        if (!c.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_CONTRIBUTOR", "You can only withdraw your own offers");
        }
        c.withdraw();
    }

    private ContributionView view(Contribution c, ProblemRef problem, SolutionRef solution, UUID viewerId,
                                  Map<UUID, Optional<UserSummary>> users) {
        Optional<UserSummary> user = users.computeIfAbsent(c.getUserId(), identity::findUser);
        boolean self = c.getUserId().equals(viewerId);
        boolean privileged = problem.submitterId().equals(viewerId)
                || (solution != null && solution.proposerId().equals(viewerId));
        String message = privileged || self ? c.getMessage() : null;
        ContributionView.Contact contact = (privileged && c.isShareContact()) || self
                ? user.map(u -> new ContributionView.Contact(u.email(), u.phone())).orElse(null)
                : null;
        return new ContributionView(c.getId(), c.getUserId(), user.map(UserSummary::displayName).orElse("Udaan member"),
                c.getTypes(), solution == null ? null : solution.id(), solution == null ? null : solution.title(),
                problem.publicCode(), problem.title(), message, contact, self, c.getCreatedAt());
    }
}
