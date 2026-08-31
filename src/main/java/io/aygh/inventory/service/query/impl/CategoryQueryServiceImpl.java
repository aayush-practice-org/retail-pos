package io.aygh.inventory.service.query.impl;

import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;
import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.CategoryUnit;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.mapper.CategoryMapper;
import io.aygh.inventory.mapper.UnitMapper;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.CategoryUnitRepository;
import io.aygh.inventory.service.query.CategoryQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryServiceImpl implements CategoryQueryService {

    private final CategoryRepository categoryRepository;
    private final CategoryUnitRepository categoryUnitRepository;
    private final InventoryResolver resolver;
    private final CategoryMapper categoryMapper;
    private final UnitMapper unitMapper;

    @Override
    public CategoryDetailResponse findById(Long id) {
        Category category = resolver.categoryWithUnits(id);

        CategoryDetailResponse response = categoryMapper.toDetail(category);
        response.setPurchaseUnits(unitsFor(category, UnitUsage.PURCHASE));
        response.setSellingUnits(unitsFor(category, UnitUsage.SELLING));
        response.setProductCount(categoryRepository.countProducts(id));
        return response;
    }

    @Override
    public PagedResponse<CategorySummaryResponse> findAll(String search, Pageable pageable) {

        Specification<Category> spec = matches(search);
        Page<Category> page = spec == null
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findAll(spec, pageable);

        return PaginationUtils.toPagedResponse(page, page.map(categoryMapper::toSummary).getContent());
    }

    @Override
    public List<CategorySummaryResponse> findAllForSelection() {
        return categoryMapper.toSummaries(categoryRepository.findAll(Sort.by("name")));
    }

    @Override
    public List<UnitResponse> findPermittedUnits(Long categoryId, UnitUsage usage) {
        resolver.category(categoryId);

        return categoryUnitRepository.findByCategoryIdAndUsage(categoryId, usage).stream()
                .map(CategoryUnit::getUnit)
                .map(unitMapper::toResponse)
                .toList();
    }

    /** From the already-fetched collection, so neither list costs a second query. */
    private List<UnitResponse> unitsFor(Category category, UnitUsage usage) {
        return category.getCategoryUnits().stream()
                .filter(cu -> cu.getUsage() == usage)
                .map(CategoryUnit::getUnit)
                .map(unitMapper::toResponse)
                .toList();
    }

    private static Specification<Category> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }
}
