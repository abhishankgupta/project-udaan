package org.udaan.identity.api;

import java.util.Optional;
import java.util.UUID;

/** The identity module's public API for other modules (section 4.1). */
public interface IdentityFacade {

    Optional<UserSummary> findUser(UUID userId);

    UserSummary rename(UUID userId, String displayName);
}
