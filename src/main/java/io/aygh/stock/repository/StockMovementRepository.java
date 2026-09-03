package io.aygh.stock.repository;

import io.aygh.stock.entity.StockMovement;
import io.aygh.stock.entity.StockReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Every read fetches the product and the entered unit: both appear on every row
 * of a ledger listing, and left to their proxies each would be a query per row.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = {"product", "enteredUnit"})
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "enteredUnit"})
    @Query("""
            SELECT m FROM StockMovement m
            WHERE (:productId IS NULL OR m.product.id = :productId)
              AND (:referenceType IS NULL OR m.referenceType = :referenceType)
            """)
    Page<StockMovement> search(@Param("productId") Long productId,
                               @Param("referenceType") StockReferenceType referenceType,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"product", "enteredUnit"})
    List<StockMovement> findByReferenceTypeAndReferenceId(StockReferenceType referenceType, Long referenceId);
}
