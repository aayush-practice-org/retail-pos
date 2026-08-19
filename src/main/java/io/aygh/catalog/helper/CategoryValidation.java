package io.aygh.catalog.helper;

import io.aygh.catalog.entity.Category;
import io.aygh.catalog.repository.CategoryRepository;
import io.aygh.catalog.repository.ProductRepository;
import io.aygh.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryValidation {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public void validateUniqueName(String name, Long parentId, Long excludeId) {
        if (categoryRepository.existsSiblingWithName(name, parentId, excludeId)) {
            throw new BusinessException(parentId == null
                    ? "A top level category named '%s' already exists".formatted(name)
                    : "This category already has a subcategory named '%s'".formatted(name));
        }
    }

    public void validateNoCircularReference(Long categoryId, Category newParent) {
        if (categoryId == null || newParent == null) return;

        Category current = newParent;

        while (current != null) {
            if (categoryId.equals(current.getId())) {
                throw new BusinessException("Circular reference detected: Category cannot be a descendant of itself.");
            }
            current = current.getParent();
        }
    }

    public void validateDeletable(Category category) {
        if (!category.getChildren().isEmpty()) {
            throw new BusinessException("Cannot delete a category that still has subcategories");
        }

        long products = productRepository.countByCategoryId(category.getId());
        if (products > 0) {
            throw new BusinessException(
                    "Cannot delete category '%s' — %d product(s) are still assigned to it"
                            .formatted(category.getName(), products));
        }
    }
}
