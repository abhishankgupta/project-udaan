package org.udaan.problem;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.udaan.media.api.MediaRef;

/** A problem as shown on its detail page. */
record ProblemView(
        UUID id,
        String publicCode,
        String title,
        String description,
        Category category,
        Place place,
        Urgency urgency,
        Integer peopleAffected,
        String estimatedImpact,
        String desiredOutcome,
        Set<AssistanceType> assistanceTypes,
        ProblemStatus status,
        boolean isPublic,
        Submitter submittedBy,
        List<MediaRef> photos,
        Counts counts,
        List<TimelineEntry> timeline,
        boolean viewerIsOwner,
        boolean viewerSupports,
        boolean viewerFollows,
        Permissions permissions,
        Resolution resolution,
        Instant createdAt) {

    /** What the current viewer may do: post progress updates, and which statuses they may move to. */
    record Permissions(boolean canPostUpdates, java.util.Set<ProblemStatus> allowedTransitions) {
    }

    /** Community confirmation of a resolution; present only while the problem is RESOLVED. */
    record Resolution(int confirms, int disputes, String viewerVote, boolean votingOpen, Instant votingClosesAt) {
    }

    record Category(UUID id, String code, String nameEn, String nameHi) {
    }

    record Place(String stateName, String cityName, String localityName, String addressText, Double latitude,
                 Double longitude) {
    }

    /** {@code id} and {@code displayName} are null for anonymous submissions viewed by others. */
    record Submitter(UUID id, String displayName, boolean anonymous) {
    }

    record Counts(int supporters, int solutions, int comments, int followers) {
    }

    record TimelineEntry(ProblemUpdateType type, ProblemStatus fromStatus, ProblemStatus toStatus, String body,
                         String authorName, String authorRole, List<MediaRef> photos, Instant at) {
    }
}
