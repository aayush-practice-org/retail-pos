package io.aygh.inventory.helper;

import io.aygh.exception.BusinessException;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.inventory.entity.ProductSellingUnit;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.repository.UnitRepository;
import io.aygh.shared.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loading inventory rows by id, with a 404 instead of an empty Optional.
 * <p>
 * The child lookups take their parent's id as well and query on both. Nothing
 * would break if they did not — every row here already lives inside the
 * caller's tenant schema — but a purchase unit reached through the wrong
 * product's URL is a bug in the caller, and answering it would hide that.
 */
@Component
@RequiredArgsConstructor
public class InventoryResolver {

    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductPurchaseUnitRepository purchaseUnitRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;

    public Unit unit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", id));
    }

    public Category category(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    public Category categoryWithUnits(Long id) {
        return categoryRepository.findWithUnitsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    public Product product(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public Product productDetail(Long id) {
        return productRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public ProductPurchaseUnit purchaseUnit(Long productId, Long id) {
        return purchaseUnitRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase unit", "id", id + " on product " + productId));
    }

    public ProductPurchaseUnit purchaseUnitDetail(Long productId, Long id) {
        return purchaseUnitRepository.findDetailByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase unit", "id", id + " on product " + productId));
    }

    public ProductSellingUnit sellingUnit(Long productId, Long id) {
        return sellingUnitRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Selling unit", "id", id + " on product " + productId));
    }


}
