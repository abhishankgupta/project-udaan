package org.udaan.taxonomy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.taxonomy.api.CategoryRef;
import org.udaan.taxonomy.api.TaxonomyFacade;

@Service
class TaxonomyService implements TaxonomyFacade {

    private final CategoryRepository categories;

    TaxonomyService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryRef> findActiveCategory(UUID id) {
        return categories.findById(id).filter(Category::isActive)
                .map(c -> new CategoryRef(c.getId(), c.getCode(), c.getNameEn(), c.getNameHi()));
    }
}
