package io.aygh.sales.repository;

import io.aygh.sales.entity.SalesReturn;
import io.aygh.shared.response.ReturnTotals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    @EntityGraph(attributePaths = {"sale", "items"})
    Optional<SalesReturn> findDetailById(Long id);

    @EntityGraph(attributePaths = {"sale", "items"})
    List<SalesReturn> findBySaleIdOrderByReturnedAtAsc(Long saleId);

    @Query("""
            SELECT r FROM SalesReturn r JOIN FETCH r.sale s
            WHERE (:search IS NULL OR LOWER(r.creditNoteNumber) LIKE :search
                   OR LOWER(s.invoiceNumber) LIKE :search
                   OR LOWER(r.customerName) LIKE :search)
              AND (CAST(:from AS timestamp) IS NULL OR r.returnedAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR r.returnedAt <= :to)
            """)
    Page<SalesReturn> search(@Param("search") String search,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

    /**
     * The highest credit note number issued under one prefix, for allocating
     * the next. Paired with the unique index, as for invoice numbers.
     */
    @Query("SELECT MAX(r.creditNoteNumber) FROM SalesReturn r WHERE r.creditNoteNumber LIKE :prefix")
    String highestCreditNoteNumber(@Param("prefix") String prefix);

    /** Credit notes raised over a window, for the sales report. */
    @Query("""
            SELECT new io.aygh.shared.response.ReturnTotals(
                       COUNT(r), SUM(r.netTotal), SUM(r.vatAmount))
            FROM SalesReturn r
            WHERE r.returnedAt BETWEEN :start AND :end
            """)
    ReturnTotals totalsBetween(@Param("start") Instant start, @Param("end") Instant end);
}
