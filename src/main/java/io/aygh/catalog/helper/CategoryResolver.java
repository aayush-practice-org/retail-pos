package io.aygh.catalog.helper;

import io.aygh.catalog.entity.Category;
import io.aygh.catalog.repository.CategoryRepository;
import io.aygh.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CategoryResolver {

    private final CategoryRepository categoryRepository;

    public Category resolve(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Category id cannot be null");
        }

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found: " + id));
    }

    public Category resolveParent(Long parentId) {
        return parentId == null ? null : resolve(parentId);
    }

    /**
     * The category itself plus every descendant, walked one level at a time.
     * Used so browsing "Grocery" also turns up products filed under
     * "Grocery → Beverages → Soft Drinks".
     */
    public Set<Long> collectSubTreeIds(Long rootId) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(rootId);

        List<Long> frontier = List.of(rootId);

        while (!frontier.isEmpty()) {
            List<Long> next = new ArrayList<>();

            for (Category child : categoryRepository.findByParentIdIn(frontier)) {
                if (ids.add(child.getId())) {
                    next.add(child.getId());
                }
            }

            frontier = next;
        }

        return ids;
    }
}
