package org.udaan.problem;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.udaan.shared.Ids;

/** One entry on a problem's public timeline. */
@Entity
@Table(name = "problem_updates")
class ProblemUpdate {

    @Id
    private UUID id;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "author_id")
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemUpdateType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ProblemStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ProblemStatus toStatus;

    private String body;

    /** OWNER, ACCEPTED_PROPOSER or COMMUNITY; null for automatic entries. */
    @Column(name = "author_role")
    private String authorRole;

    @ElementCollection
    @CollectionTable(name = "problem_update_media", joinColumns = @JoinColumn(name = "update_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "media_id", nullable = false)
    private List<UUID> mediaIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProblemUpdate() {
    }

    static ProblemUpdate statusChange(UUID problemId, UUID authorId, ProblemStatus from, ProblemStatus to, String body) {
        return statusChange(problemId, authorId, null, from, to, body, List.of());
    }

    static ProblemUpdate statusChange(UUID problemId, UUID authorId, String authorRole, ProblemStatus from, ProblemStatus to,
                                      String body, List<UUID> mediaIds) {
        ProblemUpdate u = new ProblemUpdate();
        u.authorRole = authorRole;
        u.mediaIds = new ArrayList<>(mediaIds);
        u.id = Ids.newId();
        u.problemId = problemId;
        u.authorId = authorId;
        u.type = ProblemUpdateType.STATUS_CHANGE;
        u.fromStatus = from;
        u.toStatus = to;
        u.body = body;
        u.createdAt = Instant.now();
        return u;
    }

    static ProblemUpdate progress(UUID problemId, UUID authorId, String authorRole, String body, List<UUID> mediaIds) {
        ProblemUpdate u = new ProblemUpdate();
        u.id = Ids.newId();
        u.problemId = problemId;
        u.authorId = authorId;
        u.authorRole = authorRole;
        u.type = ProblemUpdateType.PROGRESS;
        u.body = body;
        u.mediaIds = new ArrayList<>(mediaIds);
        u.createdAt = Instant.now();
        return u;
    }

    UUID getAuthorId() { return authorId; }
    String getAuthorRole() { return authorRole; }
    List<UUID> getMediaIds() { return List.copyOf(mediaIds); }
    ProblemUpdateType getType() { return type; }
    ProblemStatus getFromStatus() { return fromStatus; }
    ProblemStatus getToStatus() { return toStatus; }
    String getBody() { return body; }
    Instant getCreatedAt() { return createdAt; }
}
