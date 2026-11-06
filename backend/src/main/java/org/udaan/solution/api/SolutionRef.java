package org.udaan.solution.api;

import java.util.UUID;

public record SolutionRef(UUID id, UUID problemId, UUID proposerId, String title) {
}
