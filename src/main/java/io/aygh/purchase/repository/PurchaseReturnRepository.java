package io.aygh.purchase.repository;

import io.aygh.purchase.entity.PurchaseReturn;
import io.aygh.shared.response.ReturnTotals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    @EntityGraph(attributePaths = {
            "purchase", "vendor", "items", "items.product", "items.purchaseItem",
            "items.purchaseItem.purchaseUnit", "items.purchaseItem.purchaseUnit.unit"})
    Optional<PurchaseReturn> findDetailById(Long id);

    @EntityGraph(attributePaths = {
            "purchase", "vendor", "items", "items.product", "items.purchaseItem",
            "items.purchaseItem.purchaseUnit", "items.purchaseItem.purchaseUnit.unit"})
    List<PurchaseReturn> findByPurchaseIdOrderByCreatedAtAsc(Long purchaseId);

    @EntityGraph(attributePaths = {"purchase", "vendor"})
    @Query("""
            SELECT r FROM PurchaseReturn r
            WHERE (:vendorId IS NULL OR r.vendor.id = :vendorId)
              AND (:search IS NULL OR LOWER(r.debitNoteNumber) LIKE :search
                   OR LOWER(r.purchase.billNumber) LIKE :search)
              AND (CAST(:from AS date) IS NULL OR r.returnDate >= :from)
              AND (CAST(:to AS date) IS NULL OR r.returnDate <= :to)
            """)
    Page<PurchaseReturn> search(@Param("search") String search,
                                @Param("vendorId") Long vendorId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to,
                                Pageable pageable);

    @Query("SELECT MAX(r.debitNoteNumber) FROM PurchaseReturn r WHERE r.debitNoteNumber LIKE :prefix")
    String highestDebitNoteNumber(@Param("prefix") String prefix);

    /** Debit notes raised over a window, for the purchase report. */
    @Query("""
            SELECT new io.aygh.shared.response.ReturnTotals(
                       COUNT(r), SUM(r.netTotal), SUM(r.vatAmount))
            FROM PurchaseReturn r
            WHERE r.createdAt BETWEEN :start AND :end
            """)
    ReturnTotals totalsBetween(@Param("start") Instant start, @Param("end") Instant end);
}
