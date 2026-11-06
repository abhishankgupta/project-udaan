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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.udaan.shared.Ids;

@Entity
@Table(name = "problems")
class Problem {

    @Id
    private UUID id;

    @Column(name = "public_code", nullable = false, unique = true)
    private String publicCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "submitter_id", nullable = false)
    private UUID submitterId;

    @Column(name = "state_id", nullable = false)
    private Integer stateId;

    @Column(name = "city_id", nullable = false)
    private Integer cityId;

    @Column(name = "locality_id")
    private Integer localityId;

    @Column(name = "locality_text")
    private String localityText;

    @Column(name = "address_text")
    private String addressText;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(name = "people_affected")
    private Integer peopleAffected;

    @Column(name = "estimated_impact")
    private String estimatedImpact;

    @Column(name = "desired_outcome", nullable = false)
    private String desiredOutcome;

    /** Comma-separated {@link AssistanceType} names. */
    @Column(name = "assistance_types", nullable = false)
    private String assistanceTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemStatus status;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    // Counters are changed only by atomic SQL increments (ProblemCounters), never by saving the entity.
    @Column(name = "support_count", nullable = false, updatable = false)
    private int supportCount;

    @Column(name = "solution_count", nullable = false, updatable = false)
    private int solutionCount;

    @Column(name = "comment_count", nullable = false, updatable = false)
    private int commentCount;

    @Column(name = "follower_count", nullable = false, updatable = false)
    private int followerCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "accepted_solution_id")
    private UUID acceptedSolutionId;

    @Column(name = "accepted_proposer_id")
    private UUID acceptedProposerId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    private long version;

    @ElementCollection
    @CollectionTable(name = "problem_media", joinColumns = @JoinColumn(name = "problem_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "media_id", nullable = false)
    private List<UUID> mediaIds = new ArrayList<>();

    protected Problem() {
    }

    record Draft(String title, String description, UUID categoryId, int stateId, int cityId, Integer localityId,
                 String localityText, String addressText, Double latitude, Double longitude, Urgency urgency,
                 Integer peopleAffected, String estimatedImpact, String desiredOutcome,
                 Set<AssistanceType> assistanceTypes, boolean anonymous, List<UUID> mediaIds) {
    }

    static Problem submit(String publicCode, UUID submitterId, Draft d) {
        Problem p = new Problem();
        Instant now = Instant.now();
        p.id = Ids.newId();
        p.publicCode = publicCode;
        p.submitterId = submitterId;
        p.title = d.title().trim();
        p.description = d.description().trim();
        p.categoryId = d.categoryId();
        p.stateId = d.stateId();
        p.cityId = d.cityId();
        p.localityId = d.localityId();
        p.localityText = d.localityId() == null ? blankToNull(d.localityText()) : null;
        p.addressText = blankToNull(d.addressText());
        p.latitude = d.latitude();
        p.longitude = d.longitude();
        p.urgency = d.urgency();
        p.peopleAffected = d.peopleAffected();
        p.estimatedImpact = blankToNull(d.estimatedImpact());
        p.desiredOutcome = d.desiredOutcome().trim();
        p.assistanceTypes = d.assistanceTypes().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        p.anonymous = d.anonymous();
        p.mediaIds = new ArrayList<>(d.mediaIds());
        p.status = ProblemStatus.SUBMITTED;
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    void changeStatus(ProblemStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
        this.resolvedAt = newStatus == ProblemStatus.RESOLVED ? this.updatedAt : null;
    }

    void recordAcceptedSolution(UUID solutionId, UUID proposerId) {
        this.acceptedSolutionId = solutionId;
        this.acceptedProposerId = proposerId;
    }

    java.util.Set<ProblemLifecycle.Role> rolesOf(UUID userId) {
        java.util.EnumSet<ProblemLifecycle.Role> roles = java.util.EnumSet.noneOf(ProblemLifecycle.Role.class);
        if (userId != null && userId.equals(submitterId)) {
            roles.add(ProblemLifecycle.Role.OWNER);
        }
        if (userId != null && userId.equals(acceptedProposerId)) {
            roles.add(ProblemLifecycle.Role.ACCEPTED_PROPOSER);
        }
        return roles;
    }

    boolean isVisibleTo(UUID viewerId) {
        return status.isPublic() || submitterId.equals(viewerId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    UUID getId() { return id; }
    String getPublicCode() { return publicCode; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    UUID getCategoryId() { return categoryId; }
    UUID getSubmitterId() { return submitterId; }
    Integer getCityId() { return cityId; }
    Integer getLocalityId() { return localityId; }
    String getLocalityText() { return localityText; }
    String getAddressText() { return addressText; }
    Double getLatitude() { return latitude; }
    Double getLongitude() { return longitude; }
    Urgency getUrgency() { return urgency; }
    Integer getPeopleAffected() { return peopleAffected; }
    String getEstimatedImpact() { return estimatedImpact; }
    String getDesiredOutcome() { return desiredOutcome; }
    ProblemStatus getStatus() { return status; }
    boolean isAnonymous() { return anonymous; }
    int getSupportCount() { return supportCount; }
    int getSolutionCount() { return solutionCount; }
    int getCommentCount() { return commentCount; }
    int getFollowerCount() { return followerCount; }
    Instant getCreatedAt() { return createdAt; }
    List<UUID> getMediaIds() { return List.copyOf(mediaIds); }
    UUID getAcceptedProposerId() { return acceptedProposerId; }
    Instant getResolvedAt() { return resolvedAt; }

    Set<AssistanceType> getAssistanceTypes() {
        EnumSet<AssistanceType> set = EnumSet.noneOf(AssistanceType.class);
        Arrays.stream(assistanceTypes.split(",")).filter(s -> !s.isBlank()).map(AssistanceType::valueOf).forEach(set::add);
        return set;
    }
}
