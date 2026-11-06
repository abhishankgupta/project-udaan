package org.udaan.contribution.api;

import java.util.Set;
import java.util.UUID;

public record HelpOffered(UUID problemId, String problemCode, String problemTitle, UUID problemOwnerId,
                          UUID solutionProposerId, UUID contributorId, Set<String> types) {
}
