package org.udaan.discussion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A comment and its replies. Removed comments keep their place in the thread with no body or author. */
record CommentView(UUID id, UUID authorId, String authorName, String body, boolean removed, boolean viewerIsAuthor,
                   Instant createdAt, List<CommentView> replies) {
}
