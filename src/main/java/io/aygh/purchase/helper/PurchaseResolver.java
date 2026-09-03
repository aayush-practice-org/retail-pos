package io.aygh.purchase.helper;

import io.aygh.exception.BusinessException;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loading what a purchase refers to, with a 404 instead of an empty Optional.
 * <p>
 * The purchase-unit lookup takes the product id as well and queries on both. A
 * unit belonging to a different product is a bug in the caller, and pricing a
 * line against it would be the kind of error that only shows up in a stock count
 * weeks later.
 */
@Component
@RequiredArgsConstructor
public class PurchaseResolver {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final ProductPurchaseUnitRepository purchaseUnitRepository;

    public Purchase purchase(Long id) {
        return purchaseRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));
    }

    public Product product(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public ProductPurchaseUnit purchaseUnit(Long productId, Long id) {
        ProductPurchaseUnit unit = purchaseUnitRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase unit", "id", id + " on product " + productId));

        if (!unit.isActive()) {
            throw new BusinessException("'" + unit.getUnit().getName()
                    + "' is no longer a unit this product is bought in");
        }
        return unit;
    }
}
