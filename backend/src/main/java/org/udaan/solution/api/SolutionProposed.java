package org.udaan.solution.api;

import java.util.UUID;

public record SolutionProposed(UUID problemId, String problemCode, String problemTitle, UUID problemOwnerId,
                               UUID solutionId, String solutionTitle, UUID proposerId) {
}
