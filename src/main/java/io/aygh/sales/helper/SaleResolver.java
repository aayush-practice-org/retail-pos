package io.aygh.sales.helper;

import io.aygh.exception.BusinessException;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductSellingUnit;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleResolver {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;

    public Sale sale(Long id) {
        return saleRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
    }

    public Sale byInvoiceNumber(String invoiceNumber) {
        return saleRepository.findDetailByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "invoice number", invoiceNumber));
    }

    public Product product(Long id) {
        Product product = productRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (!product.isActive()) {
            throw new BusinessException("'" + product.getName() + "' is not on sale");
        }
        return product;
    }

    /**
     * The unit to bill in. With no id given this falls back to the product's
     * default — which is what a barcode scan resolves to, and what a product
     * sold one way only never has to state.
     */
    public ProductSellingUnit sellingUnit(Product product, Long sellingUnitId) {
        if (sellingUnitId == null) {
            return product.defaultSellingUnit()
                    .orElseThrow(() -> new BusinessException("'" + product.getName()
                            + "' has no default selling unit — name the unit to sell it in"));
        }

        ProductSellingUnit unit = sellingUnitRepository.findByIdAndProductId(sellingUnitId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Selling unit", "id", sellingUnitId + " on product " + product.getId()));

        if (!unit.isActive()) {
            throw new BusinessException("'" + product.getName() + "' is no longer sold by the "
                    + unit.getUnit().getName());
        }
        return unit;
    }
}
