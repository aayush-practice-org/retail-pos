package io.aygh.catalog.service.command;

import io.aygh.catalog.dto.request.ProductRequest;
import io.aygh.catalog.dto.response.ProductResponse;

public interface ProductCommandService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
