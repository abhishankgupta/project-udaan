package org.udaan.solution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record SolutionView(
        UUID id,
        ProblemLink problem,
        String title,
        String summary,
        String approach,
        BigDecimal estimatedCostInr,
        String timelineText,
        String resourcesRequired,
        List<String> skills,
        SolutionStatus status,
        Person proposer,
        int endorsements,
        boolean viewerEndorsed,
        boolean viewerIsProposer,
        boolean viewerIsProblemOwner,
        boolean canAccept,
        Instant createdAt) {

    record ProblemLink(UUID id, String publicCode, String title, String status) {
    }

    record Person(UUID id, String displayName) {
    }
}
