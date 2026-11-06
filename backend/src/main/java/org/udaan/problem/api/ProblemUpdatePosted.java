package org.udaan.problem.api;

import java.util.UUID;

/**
 * Published after a progress update is added to a problem's timeline. {@code actorAnonymous} is true when
 * the author is an anonymous submitter, whose name must not appear in notifications.
 */
public record ProblemUpdatePosted(UUID problemId, String publicCode, String title, UUID ownerId, UUID actorId,
                                  boolean actorAnonymous, String body) {
}
