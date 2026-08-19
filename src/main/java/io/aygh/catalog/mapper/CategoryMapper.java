package io.aygh.catalog.mapper;

import io.aygh.catalog.dto.response.CategoryResponse;
import io.aygh.catalog.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryResponse toResponseFlat(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParent() != null ? category.getParent().getId() : null,
                List.of(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public CategoryResponse toResponseTree(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getChildren().stream()
                        .map(this::toResponseTree)
                        .toList(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
