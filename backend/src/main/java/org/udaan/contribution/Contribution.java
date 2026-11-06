package org.udaan.contribution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.udaan.shared.Ids;

/** An offer of help on a problem, optionally towards one solution. */
@Entity
@Table(name = "contributions")
class Contribution {

    static final String INTERESTED = "INTERESTED";
    static final String WITHDRAWN = "WITHDRAWN";

    @Id
    private UUID id;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "solution_id")
    private UUID solutionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String types;

    private String message;

    /** The contributor agreed to share their email/phone with the problem owner (and solution proposer). */
    @Column(name = "share_contact", nullable = false)
    private boolean shareContact;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Contribution() {
    }

    static Contribution offer(UUID problemId, UUID solutionId, UUID userId) {
        Contribution c = new Contribution();
        c.id = Ids.newId();
        c.problemId = problemId;
        c.solutionId = solutionId;
        c.userId = userId;
        c.status = INTERESTED;
        c.createdAt = Instant.now();
        return c;
    }

    void update(Set<ContributionType> types, String message, boolean shareContact) {
        this.types = types.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        this.message = message == null || message.isBlank() ? null : message.trim();
        this.shareContact = shareContact;
        this.updatedAt = Instant.now();
    }

    void withdraw() {
        status = WITHDRAWN;
        updatedAt = Instant.now();
    }

    boolean isActive() {
        return INTERESTED.equals(status);
    }

    UUID getId() { return id; }
    UUID getProblemId() { return problemId; }
    UUID getSolutionId() { return solutionId; }
    UUID getUserId() { return userId; }
    String getMessage() { return message; }
    boolean isShareContact() { return shareContact; }
    Instant getCreatedAt() { return createdAt; }

    Set<ContributionType> getTypes() {
        EnumSet<ContributionType> set = EnumSet.noneOf(ContributionType.class);
        Arrays.stream(types.split(",")).filter(s -> !s.isBlank()).map(ContributionType::valueOf).forEach(set::add);
        return set;
    }
}
