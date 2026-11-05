package org.udaan.taxonomy;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/categories — public list of active problem categories. */
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {

    private final CategoryRepository categories;

    CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<CategoryDto> list() {
        return categories.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(c -> new CategoryDto(c.getId(), c.getCode(), c.getNameEn(), c.getNameHi()))
                .toList();
    }

    record CategoryDto(UUID id, String code, String nameEn, String nameHi) {}
}
