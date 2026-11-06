package org.udaan.taxonomy.api;

import java.util.Optional;
import java.util.UUID;

public interface TaxonomyFacade {

    /** An active category, or empty. */
    Optional<CategoryRef> findActiveCategory(UUID id);
}
