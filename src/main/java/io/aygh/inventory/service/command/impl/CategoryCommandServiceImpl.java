package io.aygh.inventory.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.CategoryRequest;
import io.aygh.inventory.dto.request.CategoryUnitRequest;
import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.CategoryUnit;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.helper.InventoryValidation;
import io.aygh.inventory.mapper.CategoryMapper;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.CategoryUnitRepository;
import io.aygh.inventory.service.command.CategoryCommandService;
import io.aygh.inventory.service.query.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryCommandServiceImpl implements CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final CategoryUnitRepository categoryUnitRepository;
    private final InventoryResolver resolver;
    private final InventoryValidation validation;
    private final CategoryMapper categoryMapper;
    private final CategoryQueryService categoryQueryService;

    @Override
    public CategorySummaryResponse create(CategoryRequest request) {
        validation.requireCategoryNameAvailable(request.name(), null);

        Category saved = categoryRepository.save(categoryMapper.toEntity(request));
        log.info("Created category '{}'", saved.getName());
        return categoryMapper.toSummary(saved);
    }

    @Override
    public CategorySummaryResponse update(Long id, CategoryRequest request) {
        Category category = resolver.category(id);

        validation.requireCategoryNameAvailable(request.name(), id);
        categoryMapper.applyUpdate(request, category);

        Category saved = categoryRepository.save(category);
        log.info("Updated category '{}'", saved.getName());
        return categoryMapper.toSummary(saved);
    }

    @Override
    public CategorySummaryResponse makeDefault(Long id) {
        Category category = resolver.category(id);

        if (category.isDefaultCategory()) {
            return categoryMapper.toSummary(category);
        }

        // Released before it is claimed, not for tidiness: only one row may
        // carry the flag, so setting the new one first violates the index.
        // The clear detaches everything, hence the second load.
        categoryRepository.clearDefault();

        Category target = resolver.category(id);
        target.setDefaultCategory(true);

        Category saved = categoryRepository.save(target);
        log.info("Unfiled products now go to category '{}'", saved.getName());
        return categoryMapper.toSummary(saved);
    }

    @Override
    public void delete(Long id) {
        Category category = resolver.category(id);

        validation.requireCategoryEmpty(id);
        validation.requireNotDefaultCategory(category);

        categoryRepository.delete(category);
        log.info("Removed category '{}'", category.getName());
    }

    @Override
    public CategoryDetailResponse allowUnit(Long categoryId, CategoryUnitRequest request) {
        Category category = resolver.category(categoryId);
        Unit unit = resolver.unit(request.unitId());

        validation.requireUnitNotAlreadyPermitted(categoryId, unit.getId(), request.usage());

        categoryUnitRepository.save(CategoryUnit.builder()
                .category(category)
                .unit(unit)
                .usage(request.usage())
                .build());

        log.info("Category '{}' now permits '{}' for {}", category.getName(), unit.getName(), request.usage());
        return categoryQueryService.findById(categoryId);
    }

    @Override
    public void revokeUnit(Long categoryId, Long categoryUnitId) {
        CategoryUnit permission = categoryUnitRepository.findByIdAndCategoryId(categoryUnitId, categoryId)
                .orElseThrow(() -> new BusinessException(
                        "That unit permission does not belong to category " + categoryId));

        categoryUnitRepository.delete(permission);
        log.info("Category {} no longer permits unit {} for {}",
                categoryId, permission.getUnit().getId(), permission.getUsage());
    }
}
