package org.udaan.contribution;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * An offer of help. {@code message} and {@code contact} are only filled in for the problem owner and,
 * for offers towards a solution, that solution's proposer; contact only if the contributor agreed.
 */
record ContributionView(UUID id, UUID contributorId, String contributorName, Set<ContributionType> types,
                        UUID solutionId, String solutionTitle, String problemCode, String problemTitle,
                        String message, Contact contact, boolean viewerIsContributor, Instant createdAt) {

    record Contact(String email, String phone) {
    }
}
