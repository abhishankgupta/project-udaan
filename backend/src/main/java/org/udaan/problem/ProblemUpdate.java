package org.udaan.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProblemUpdate() {
    }

    static ProblemUpdate statusChange(UUID problemId, UUID authorId, ProblemStatus from, ProblemStatus to, String body) {
        ProblemUpdate u = new ProblemUpdate();
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

    ProblemUpdateType getType() { return type; }
    ProblemStatus getFromStatus() { return fromStatus; }
    ProblemStatus getToStatus() { return toStatus; }
    String getBody() { return body; }
    Instant getCreatedAt() { return createdAt; }
}
