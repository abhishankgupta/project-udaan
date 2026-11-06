package org.udaan.problem;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.problem.api.ProblemFacade;
import org.udaan.problem.api.ProblemRef;
import org.udaan.shared.web.NotFoundException;

@Service
class ProblemFacadeImpl implements ProblemFacade {

    private static final Set<ProblemStatus> OPEN_TO_SOLUTIONS = EnumSet.of(ProblemStatus.VERIFIED,
            ProblemStatus.OPEN_FOR_SOLUTIONS, ProblemStatus.SOLUTION_PROPOSED, ProblemStatus.SOLUTION_ACCEPTED);

    private final ProblemRepository problems;
    private final ProblemUpdateRepository updates;
    private final ProblemCounters counters;

    ProblemFacadeImpl(ProblemRepository problems, ProblemUpdateRepository updates, ProblemCounters counters) {
        this.problems = problems;
        this.updates = updates;
        this.counters = counters;
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemRef requirePublic(String ref) {
        Optional<Problem> found = ref.startsWith("UDN-") ? problems.findByPublicCode(ref) : parse(ref).flatMap(problems::findById);
        return found.filter(p -> p.getStatus().isPublic()).map(ProblemFacadeImpl::toRef)
                .orElseThrow(() -> new NotFoundException("PROBLEM_NOT_FOUND", "This problem does not exist or is not public yet"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProblemRef> find(UUID problemId) {
        return problems.findById(problemId).map(ProblemFacadeImpl::toRef);
    }

    @Override
    public boolean acceptsSolutions(ProblemRef problem) {
        return OPEN_TO_SOLUTIONS.contains(ProblemStatus.valueOf(problem.status()));
    }

    @Override
    @Transactional
    public void solutionProposed(UUID problemId) {
        counters.adjust(problemId, ProblemCounters.Counter.SOLUTIONS, 1);
        Problem problem = problems.findById(problemId).orElseThrow();
        if (problem.getStatus() == ProblemStatus.OPEN_FOR_SOLUTIONS || problem.getStatus() == ProblemStatus.VERIFIED) {
            move(problem, ProblemStatus.SOLUTION_PROPOSED, null, "A solution has been proposed");
        }
    }

    @Override
    @Transactional
    public void solutionWithdrawn(UUID problemId) {
        counters.adjust(problemId, ProblemCounters.Counter.SOLUTIONS, -1);
    }

    @Override
    @Transactional
    public void solutionAccepted(UUID problemId, UUID ownerId, String solutionTitle) {
        Problem problem = problems.findById(problemId).orElseThrow();
        move(problem, ProblemStatus.SOLUTION_ACCEPTED, ownerId, "The problem owner accepted the solution: " + solutionTitle);
    }

    @Override
    @Transactional
    public void commentCountChanged(UUID problemId, int delta) {
        counters.adjust(problemId, ProblemCounters.Counter.COMMENTS, delta);
    }

    private void move(Problem problem, ProblemStatus to, UUID actorId, String note) {
        ProblemStatus from = problem.getStatus();
        problem.changeStatus(to);
        updates.save(ProblemUpdate.statusChange(problem.getId(), actorId, from, to, note));
    }

    private static ProblemRef toRef(Problem p) {
        return new ProblemRef(p.getId(), p.getPublicCode(), p.getTitle(), p.getSubmitterId(), p.getStatus().name());
    }

    private static Optional<UUID> parse(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
