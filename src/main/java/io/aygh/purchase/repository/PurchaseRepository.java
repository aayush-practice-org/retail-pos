package io.aygh.purchase.repository;

import io.aygh.purchase.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /** A vendor's bill numbers must not repeat, or a duplicate entry goes unnoticed. */
    boolean existsByVendorIdAndBillNumberIgnoreCase(Long vendorId, String billNumber);

    /**
     * The lines and everything they display, in one read. The detail view needs
     * all of it, and each association left lazy is a query per line.
     */
    @EntityGraph(attributePaths = {
            "vendor", "items", "items.product", "items.product.baseUnit",
            "items.purchaseUnit", "items.purchaseUnit.unit"})
    Optional<Purchase> findDetailById(Long id);

    /**
     * Only the vendor is fetched: a listing shows the vendor's name on every row
     * but never opens the lines.
     */
    @EntityGraph(attributePaths = "vendor")
    @Query("""
            SELECT p FROM Purchase p
            WHERE (:vendorId IS NULL OR p.vendor.id = :vendorId)
              AND (:search IS NULL OR LOWER(p.billNumber) LIKE :search)
              AND (CAST(:from AS date) IS NULL OR p.purchaseDate >= :from)
              AND (CAST(:to AS date) IS NULL OR p.purchaseDate <= :to)
            """)
    Page<Purchase> search(@Param("search") String search,
                          @Param("vendorId") Long vendorId,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          Pageable pageable);

    @Query("SELECT COUNT(i) FROM PurchaseItem i WHERE i.purchase.id = :purchaseId")
    int countItems(@Param("purchaseId") Long purchaseId);
}
