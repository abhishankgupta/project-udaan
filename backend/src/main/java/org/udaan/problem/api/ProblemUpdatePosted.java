package org.udaan.problem.api;

import java.util.UUID;

/** Published after a progress update is added to a problem's timeline. */
public record ProblemUpdatePosted(UUID problemId, String publicCode, String title, UUID ownerId, UUID actorId, String body) {
}
