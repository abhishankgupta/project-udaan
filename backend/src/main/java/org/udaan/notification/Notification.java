package org.udaan.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.udaan.shared.Ids;

@Entity
@Table(name = "notifications")
class Notification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    private String body;

    /** Web path to open, e.g. /problems/UDN-LKO-000123. */
    @Column(nullable = false)
    private String link;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "problem_id")
    private UUID problemId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    Notification(UUID userId, String type, String title, String body, String link, UUID actorId, UUID problemId) {
        this.id = Ids.newId();
        this.userId = userId;
        this.type = type;
        this.title = title.length() > 200 ? title.substring(0, 197) + "..." : title;
        this.body = body == null ? null : body.length() > 500 ? body.substring(0, 497) + "..." : body;
        this.link = link;
        this.actorId = actorId;
        this.problemId = problemId;
        this.createdAt = Instant.now();
    }

    UUID getId() { return id; }
    String getType() { return type; }
    String getTitle() { return title; }
    String getBody() { return body; }
    String getLink() { return link; }
    Instant getReadAt() { return readAt; }
    Instant getCreatedAt() { return createdAt; }
}
