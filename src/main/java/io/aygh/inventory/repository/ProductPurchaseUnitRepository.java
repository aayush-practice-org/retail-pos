package io.aygh.inventory.repository;

import io.aygh.inventory.entity.ProductPurchaseUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPurchaseUnitRepository extends JpaRepository<ProductPurchaseUnit, Long> {

    /**
     * The rates come along because every response flattens the one in force onto
     * the row; without them that is a query per purchase unit.
     */
    @EntityGraph(attributePaths = {"unit", "product", "vatRates"})
    List<ProductPurchaseUnit> findByProductId(Long productId);

    @EntityGraph(attributePaths = {"unit", "product", "vatRates"})
    Optional<ProductPurchaseUnit> findDetailByIdAndProductId(Long id, Long productId);

    Optional<ProductPurchaseUnit> findByIdAndProductId(Long id, Long productId);

    Optional<ProductPurchaseUnit> findByProductIdAndUnitId(Long productId, Long unitId);

    boolean existsByProductIdAndUnitId(Long productId, Long unitId);

    boolean existsByProductIdAndUnitIdAndIdNot(Long productId, Long unitId, Long id);

    /**
     * Clears the default flag across a product's purchase units so setting a new
     * one cannot leave two. A bulk update rather than a load-and-loop: there is
     * nothing per-row to decide, and it must not race with itself.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductPurchaseUnit p SET p.isDefault = false "
            + "WHERE p.product.id = :productId AND p.deletedAt IS NULL")
    void clearDefaults(Long productId);
}
