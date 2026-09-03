package io.aygh.stock.helper;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.stock.entity.ProductStock;
import io.aygh.stock.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Getting at a product's stock row, creating it the first time it is asked for.
 * <p>
 * A product with no row has never moved, which is the same thing as having none
 * — so rather than making every caller handle an empty Optional, the row is
 * opened at zero on first touch. That keeps "stock exists for every product" an
 * invariant the rest of the module can rely on without a backfill migration
 * every time a product is added.
 */
@Component
@RequiredArgsConstructor
public class StockResolver {

    private final ProductStockRepository productStockRepository;
    private final ProductRepository productRepository;

    public Product product(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    /** Read-only view of a product's stock; never creates a row. */
    public ProductStock stockOf(Long productId) {
        return productStockRepository.findByProductId(productId)
                .orElseGet(() -> ProductStock.builder()
                        .product(product(productId))
                        .build());
    }

    /**
     * The row to write through, locked for this transaction. Callers must be
     * inside one — the lock is worthless otherwise.
     */
    @Transactional
    public ProductStock lockFor(Product product) {
        return productStockRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> productStockRepository.save(ProductStock.builder()
                        .product(product)
                        .build()));
    }
}
