package io.aygh.inventory.service.query.impl;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.dto.response.ProductSummaryResponse;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.mapper.ProductMapper;
import io.aygh.inventory.mapper.ProductUnitMapper;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.service.query.ProductQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductPurchaseUnitRepository purchaseUnitRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;
    private final InventoryResolver resolver;
    private final ProductMapper productMapper;
    private final ProductUnitMapper productUnitMapper;

    @Override
    public PagedResponse<ProductSummaryResponse> findAll(
            String search, Long categoryId, Boolean active, Pageable pageable) {

        resolver.requireTenantContext();

        List<Specification<Product>> filters = Stream.of(matches(search), inCategory(categoryId), isActive(active))
                .filter(Objects::nonNull)
                .toList();

        Page<Product> page = productRepository.findAll(Specification.allOf(filters), pageable);
        return PaginationUtils.toPagedResponse(page, page.map(productMapper::toSummary).getContent());
    }

    @Override
    public ProductDetailResponse findById(Long id) {
        resolver.requireTenantContext();
        return productMapper.toDetail(resolver.productDetail(id));
    }

    @Override
    public List<ProductPurchaseUnitResponse> findPurchaseUnits(Long productId) {
        resolver.requireTenantContext();
        resolver.product(productId);
        return productUnitMapper.toPurchaseResponses(purchaseUnitRepository.findByProductId(productId));
    }

    @Override
    public ProductPurchaseUnitDetailResponse findPurchaseUnit(Long productId, Long purchaseUnitId) {
        resolver.requireTenantContext();
        return productUnitMapper.toDetail(resolver.purchaseUnitDetail(productId, purchaseUnitId));
    }

    @Override
    public List<ProductSellingUnitResponse> findSellingUnits(Long productId) {
        resolver.requireTenantContext();
        resolver.product(productId);
        return productUnitMapper.toSellingResponses(sellingUnitRepository.findByProductId(productId));
    }

    @Override
    public ProductSellingUnitResponse findByBarcode(String barcode) {
        resolver.requireTenantContext();
        return sellingUnitRepository.findByBarcode(barcode)
                .map(productUnitMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "barcode", barcode));
    }

    // ── Filters ───────────────────────────────────────────────────────────

    /**
     * Free text over the three fields someone would actually search a product by.
     * The category is joined rather than compared as a name, so a search for
     * "grain" finds everything on that aisle.
     */
    private static Specification<Product> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("productCode")), pattern),
                cb.like(cb.lower(root.get("brand")), pattern));
    }

    private static Specification<Product> inCategory(Long categoryId) {
        return categoryId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Product> isActive(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
