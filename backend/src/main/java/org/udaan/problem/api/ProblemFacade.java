package org.udaan.problem.api;

import java.util.Optional;
import java.util.UUID;

/** The problem module's API for solutions, discussions and contributions. */
public interface ProblemFacade {

    /** A publicly visible problem by code or id; throws a 404 problem otherwise. */
    ProblemRef requirePublic(String ref);

    Optional<ProblemRef> find(UUID problemId);

    /** Whether new solutions may still be proposed (not resolved or closed). */
    boolean acceptsSolutions(ProblemRef problem);

    /** Counts a new solution; the first one moves an open problem to SOLUTION_PROPOSED. */
    void solutionProposed(UUID problemId);

    void solutionWithdrawn(UUID problemId);

    /** Records the owner's choice on the problem's timeline and moves it to SOLUTION_ACCEPTED. */
    void solutionAccepted(UUID problemId, UUID ownerId, String solutionTitle);

    void commentCountChanged(UUID problemId, int delta);
}
