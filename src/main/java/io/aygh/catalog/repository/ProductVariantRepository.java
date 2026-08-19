package io.aygh.catalog.repository;

import io.aygh.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(Long productId);

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    long countByProductId(Long productId);

    @Query("""
            SELECT COUNT(v) > 0 FROM ProductVariant v
            WHERE v.product.id = :productId
              AND LOWER(v.name) = LOWER(:name)
              AND (:excludeId IS NULL OR v.id <> :excludeId)
            """)
    boolean existsByProductIdAndName(@Param("productId") Long productId,
                                     @Param("name") String name,
                                     @Param("excludeId") Long excludeId);

    @Query("""
            SELECT COUNT(v) > 0 FROM ProductVariant v
            WHERE LOWER(v.sku) = LOWER(:sku)
              AND (:excludeId IS NULL OR v.id <> :excludeId)
            """)
    boolean existsBySku(@Param("sku") String sku, @Param("excludeId") Long excludeId);

    @Query("""
            SELECT COUNT(v) > 0 FROM ProductVariant v
            WHERE v.barcode = :barcode
              AND (:excludeId IS NULL OR v.id <> :excludeId)
            """)
    boolean existsByBarcode(@Param("barcode") String barcode, @Param("excludeId") Long excludeId);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.isDefault = true")
    List<ProductVariant> findDefaultsByProductId(@Param("productId") Long productId);

    /**
     * Variant counts for a whole page of products in one round trip,
     * so the product list does not fan out into a query per row.
     */
    @Query("""
            SELECT v.product.id AS productId, COUNT(v) AS total
            FROM ProductVariant v
            WHERE v.product.id IN :productIds
            GROUP BY v.product.id
            """)
    List<VariantCount> countByProductIds(@Param("productIds") Collection<Long> productIds);

    interface VariantCount {
        Long getProductId();

        Long getTotal();
    }
}
