package org.udaan.solution.api;

import java.util.Optional;
import java.util.UUID;

public interface SolutionFacade {

    /** A visible (published or accepted) solution whose problem is public; throws a 404 problem otherwise. */
    SolutionRef requireVisible(UUID solutionId);

    Optional<SolutionRef> find(UUID solutionId);
}
