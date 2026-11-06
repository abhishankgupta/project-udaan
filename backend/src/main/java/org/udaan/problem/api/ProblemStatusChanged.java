package org.udaan.problem.api;

import java.util.UUID;

/** Published after a problem's status changes. {@code actorId} is null for automatic changes. */
public record ProblemStatusChanged(UUID problemId, String publicCode, String title, UUID ownerId, String fromStatus,
                                   String toStatus, UUID actorId, String note) {
}
