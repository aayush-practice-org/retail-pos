package io.aygh.stock.repository;

import io.aygh.stock.entity.ProductStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    Optional<ProductStock> findByProductId(Long productId);

    /**
     * Takes a write lock on the row for the duration of the transaction.
     * <p>
     * The {@code @Version} column already stops two tills overwriting each other,
     * but it does so by failing the loser — which, on a sale of several lines, is
     * a whole basket lost to a race on one of them. Selling takes this instead:
     * the second till waits a few milliseconds rather than being told to start
     * over.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductStock s WHERE s.product.id = :productId")
    Optional<ProductStock> findByProductIdForUpdate(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"product", "product.baseUnit", "product.category"})
    Page<ProductStock> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.baseUnit", "product.category"})
    @Query("""
            SELECT s FROM ProductStock s
            WHERE (:categoryId IS NULL OR s.product.category.id = :categoryId)
              AND (:search IS NULL OR LOWER(s.product.name) LIKE :search
                   OR LOWER(s.product.productCode) LIKE :search)
              AND (:lowOnly = FALSE
                   OR s.quantity <= 0
                   OR (s.reorderLevel > 0 AND s.quantity <= s.reorderLevel))
            """)
    Page<ProductStock> search(@Param("search") String search,
                              @Param("categoryId") Long categoryId,
                              @Param("lowOnly") boolean lowOnly,
                              Pageable pageable);

    @Query("""
            SELECT COUNT(s) FROM ProductStock s
            WHERE s.quantity <= 0 OR (s.reorderLevel > 0 AND s.quantity <= s.reorderLevel)
            """)
    long countNeedingAttention();
}
