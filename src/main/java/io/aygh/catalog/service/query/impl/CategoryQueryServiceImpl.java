package io.aygh.catalog.service.query.impl;

import io.aygh.catalog.dto.response.CategoryResponse;
import io.aygh.catalog.entity.Category;
import io.aygh.catalog.helper.CategoryResolver;
import io.aygh.catalog.mapper.CategoryMapper;
import io.aygh.catalog.repository.CategoryRepository;
import io.aygh.catalog.service.query.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 *  NO pagination — a mart's category tree is browsed whole, not paged.
 *  findAll() returns the roots with their subcategories nested underneath,
 *  so each category appears exactly once in the response.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CategoryQueryServiceImpl implements CategoryQueryService {

    private final CategoryMapper categoryMapper;
    private final CategoryResolver categoryResolver;
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponseTree(categoryResolver.resolve(id));
    }

    @Override
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllWithChildren()
                .stream()
                .filter(category -> category.getParent() == null)
                .map(categoryMapper::toResponseTree)
                .toList();
    }

    @Override
    public List<CategoryResponse> findRoots() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(categoryMapper::toResponseFlat)
                .toList();
    }
}
