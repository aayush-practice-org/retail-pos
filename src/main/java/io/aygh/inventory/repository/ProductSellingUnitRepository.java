package io.aygh.inventory.repository;

import io.aygh.inventory.entity.ProductSellingUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSellingUnitRepository extends JpaRepository<ProductSellingUnit, Long> {

    @EntityGraph(attributePaths = {"unit", "product"})
    List<ProductSellingUnit> findByProductId(Long productId);

    Optional<ProductSellingUnit> findByIdAndProductId(Long id, Long productId);

    Optional<ProductSellingUnit> findByProductIdAndUnitId(Long productId, Long unitId);

    boolean existsByProductIdAndUnitId(Long productId, Long unitId);

    boolean existsByProductIdAndUnitIdAndIdNot(Long productId, Long unitId, Long id);

    /**
     * Barcodes are scanned at the till, so they must identify one row mart-wide.
     */
    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @EntityGraph(attributePaths = {"unit", "product"})
    Optional<ProductSellingUnit> findByBarcode(String barcode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductSellingUnit s SET s.isDefault = false "
            + "WHERE s.product.id = :productId AND s.deletedAt IS NULL")
    void clearDefaults(Long productId);
}
