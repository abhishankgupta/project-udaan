package org.udaan.discussion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.udaan.shared.Ids;

@Entity
@Table(name = "comments")
class Comment {

    enum TargetType { PROBLEM, SOLUTION }

    enum Status { VISIBLE, REMOVED }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** The problem the discussion belongs to, for counting and cleanup. */
    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Comment() {
    }

    static Comment post(TargetType type, UUID targetId, UUID problemId, UUID parentId, UUID authorId, String body) {
        Comment c = new Comment();
        Instant now = Instant.now();
        c.id = Ids.newId();
        c.targetType = type;
        c.targetId = targetId;
        c.problemId = problemId;
        c.parentId = parentId;
        c.authorId = authorId;
        c.body = body.trim();
        c.status = Status.VISIBLE;
        c.createdAt = now;
        c.updatedAt = now;
        return c;
    }

    void remove() {
        status = Status.REMOVED;
        updatedAt = Instant.now();
    }

    UUID getId() { return id; }
    TargetType getTargetType() { return targetType; }
    UUID getTargetId() { return targetId; }
    UUID getProblemId() { return problemId; }
    UUID getParentId() { return parentId; }
    UUID getAuthorId() { return authorId; }
    String getBody() { return body; }
    Status getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
}
