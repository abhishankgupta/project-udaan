package org.udaan.solution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.udaan.shared.Ids;

@Entity
@Table(name = "solutions")
class Solution {

    @Id
    private UUID id;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Column(name = "proposer_id", nullable = false)
    private UUID proposerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String approach;

    @Column(name = "estimated_cost_inr")
    private BigDecimal estimatedCostInr;

    @Column(name = "timeline_text")
    private String timelineText;

    @Column(name = "resources_required")
    private String resourcesRequired;

    @Column(name = "skills_required")
    private String skillsRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SolutionStatus status;

    // Changed only by atomic SQL (endorsements), never by saving the entity.
    @Column(name = "endorsement_count", nullable = false, updatable = false)
    private int endorsementCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Solution() {
    }

    record Draft(String title, String summary, String approach, BigDecimal estimatedCostInr, String timelineText,
                 String resourcesRequired, List<String> skills) {
    }

    static Solution propose(UUID problemId, UUID proposerId, Draft d) {
        Solution s = new Solution();
        Instant now = Instant.now();
        s.id = Ids.newId();
        s.problemId = problemId;
        s.proposerId = proposerId;
        s.title = d.title().trim();
        s.summary = d.summary().trim();
        s.approach = d.approach().trim();
        s.estimatedCostInr = d.estimatedCostInr();
        s.timelineText = blankToNull(d.timelineText());
        s.resourcesRequired = blankToNull(d.resourcesRequired());
        s.skillsRequired = d.skills() == null ? null : blankToNull(String.join(",", d.skills().stream()
                .map(String::trim).filter(x -> !x.isEmpty()).distinct().limit(15).toList()));
        s.status = SolutionStatus.PUBLISHED;
        s.createdAt = now;
        s.updatedAt = now;
        return s;
    }

    void accept() {
        status = SolutionStatus.ACCEPTED;
        updatedAt = Instant.now();
    }

    void withdraw() {
        status = SolutionStatus.WITHDRAWN;
        updatedAt = Instant.now();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    UUID getId() { return id; }
    UUID getProblemId() { return problemId; }
    UUID getProposerId() { return proposerId; }
    String getTitle() { return title; }
    String getSummary() { return summary; }
    String getApproach() { return approach; }
    BigDecimal getEstimatedCostInr() { return estimatedCostInr; }
    String getTimelineText() { return timelineText; }
    String getResourcesRequired() { return resourcesRequired; }
    SolutionStatus getStatus() { return status; }
    int getEndorsementCount() { return endorsementCount; }
    Instant getCreatedAt() { return createdAt; }

    List<String> getSkills() {
        return skillsRequired == null ? List.of() : Arrays.stream(skillsRequired.split(",")).toList();
    }
}
