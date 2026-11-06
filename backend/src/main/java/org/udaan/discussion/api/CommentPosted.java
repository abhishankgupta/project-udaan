package org.udaan.discussion.api;

import java.util.UUID;

/**
 * Published after a comment is posted. {@code targetOwnerId} is the problem's owner or the solution's
 * proposer; {@code parentAuthorId} is set for replies. {@code link} is the web path of the discussion.
 */
public record CommentPosted(UUID problemId, String targetTitle, String link, UUID targetOwnerId, UUID parentAuthorId,
                            UUID authorId, String excerpt) {
}
