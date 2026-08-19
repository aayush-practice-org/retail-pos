package io.aygh.catalog.service.command;

import io.aygh.catalog.dto.request.CategoryRequest;
import io.aygh.catalog.dto.response.CategoryResponse;

public interface CategoryCommandService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
