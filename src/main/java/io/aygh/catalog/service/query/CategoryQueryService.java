package io.aygh.catalog.service.query;

import io.aygh.catalog.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryQueryService {

    CategoryResponse findById(Long id);

    List<CategoryResponse> findAll();

    List<CategoryResponse> findRoots();
}
