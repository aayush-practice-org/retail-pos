package io.aygh.catalog.service.command.impl;

import io.aygh.catalog.dto.request.CategoryRequest;
import io.aygh.catalog.dto.response.CategoryResponse;
import io.aygh.catalog.entity.Category;
import io.aygh.catalog.helper.CategoryResolver;
import io.aygh.catalog.helper.CategoryValidation;
import io.aygh.catalog.mapper.CategoryMapper;
import io.aygh.catalog.repository.CategoryRepository;
import io.aygh.catalog.service.command.CategoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
class CategoryCommandServiceImpl implements CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryValidation categoryValidation;
    private final CategoryResolver categoryResolver;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category parent = categoryResolver.resolveParent(request.parentId());

        categoryValidation.validateUniqueName(request.name(), request.parentId(), null);

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .parent(parent)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Created category id={} under parent={}", saved.getId(), request.parentId());

        return categoryMapper.toResponseFlat(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryResolver.resolve(id);

        Category newParent = categoryResolver.resolveParent(request.parentId());

        // A category may not be moved underneath one of its own descendants
        categoryValidation.validateNoCircularReference(id, newParent);
        categoryValidation.validateUniqueName(request.name(), request.parentId(), id);

        category.changeName(request.name());
        category.changeParent(newParent);
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());

        log.info("Updated category id={}", id);

        return categoryMapper.toResponseFlat(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryResolver.resolve(id);

        categoryValidation.validateDeletable(category);

        categoryRepository.delete(category);
        log.info("Deleted category id={}", id);
    }
}
