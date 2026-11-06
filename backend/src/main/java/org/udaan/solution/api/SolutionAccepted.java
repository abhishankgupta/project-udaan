package org.udaan.solution.api;

import java.util.UUID;

public record SolutionAccepted(UUID problemId, String problemCode, String problemTitle, UUID solutionId,
                               String solutionTitle, UUID proposerId, UUID acceptedBy) {
}
