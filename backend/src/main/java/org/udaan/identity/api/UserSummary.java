package org.udaan.identity.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Account data other modules may read. Contact details are included for the owner's own views only. */
public record UserSummary(UUID id, String displayName, String email, String phone, List<String> roles, Instant memberSince) {
}
