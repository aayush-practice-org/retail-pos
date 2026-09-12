package io.aygh.sales.repository;

import io.aygh.sales.dto.response.PaymentTypeTotal;
import io.aygh.sales.dto.response.SalesReportSummary;
import io.aygh.sales.dto.response.SalesTotalsResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.shared.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = {
            "items", "items.product", "items.sellingUnit", "items.sellingUnit.unit"})
    Optional<Sale> findDetailById(Long id);

    @EntityGraph(attributePaths = {
            "items", "items.product", "items.sellingUnit", "items.sellingUnit.unit"})
    Optional<Sale> findDetailByInvoiceNumber(String invoiceNumber);

    /**
     * Listings never open the lines, so nothing is fetched beyond the header.
     */
    @Query("""
            SELECT s FROM Sale s
            WHERE (:search IS NULL OR LOWER(s.invoiceNumber) LIKE :search
                   OR LOWER(s.customerName) LIKE :search
                   OR LOWER(s.customerPhone) LIKE :search)
              AND (:status IS NULL OR s.paymentStatus = :status)
              AND (CAST(:from AS timestamp) IS NULL OR s.soldAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR s.soldAt <= :to)
            """)
    Page<Sale> search(@Param("search") String search,
                      @Param("status") PaymentStatus status,
                      @Param("from") Instant from,
                      @Param("to") Instant to,
                      Pageable pageable);

    @Query("SELECT COUNT(i) FROM SaleItem i WHERE i.sale.id = :saleId")
    int countItems(@Param("saleId") Long saleId);

    /**
     * The dashboard figures in one aggregate. {@code SUM} over no rows is null,
     * which the response record normalises — see
     * {@link SalesTotalsResponse}'s callers.
     */
    @Query("""
            SELECT new io.aygh.sales.dto.response.SalesTotalsResponse(
                       COUNT(s),
                       SUM(s.subTotal),
                       SUM(s.discountAmount),
                       SUM(s.vatAmount),
                       SUM(s.netTotal),
                       SUM(s.paidAmount),
                       SUM(s.netTotal - s.paidAmount))
            FROM Sale s
            WHERE (CAST(:from AS timestamp) IS NULL OR s.soldAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR s.soldAt <= :to)
            """)
    SalesTotalsResponse totalsBetween(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * The highest number issued under one prefix, for allocating the next. Read
     * inside the sale's transaction and paired with the unique index on
     * {@code invoice_number}, which is what actually stops two tills taking the
     * same number.
     */
    @Query("SELECT MAX(s.invoiceNumber) FROM Sale s WHERE s.invoiceNumber LIKE :prefix")
    String highestInvoiceNumber(@Param("prefix") String prefix);

    @Query("""
               SELECT new io.aygh.sales.dto.response.SalesReportSummary(
                      SUM(s.netTotal), SUM (s.vatAmount), COUNT(s), null 
            
                    )  FROM Sale  s 
                                WHERE s.createdAt between :start and :end
            """)
    SalesReportSummary salesReport(Instant start, Instant end);

    @Query("""
                SELECT new io.aygh.sales.dto.response.PaymentTypeTotal(
                    s.paymentMethod,
                    SUM(s.netTotal)
                )
                FROM Sale s
                WHERE s.createdAt BETWEEN :start AND :end
                GROUP BY s.paymentMethod
            """)
    List<PaymentTypeTotal> paymentByType(
            Instant start,
            Instant end
    );

    @Query("""
            SELECT COALESCE(SUM(s.netTotal - s.paidAmount), 0)
            FROM Sale s
            WHERE s.customer.id = :customerId
              AND s.paymentStatus != io.aygh.shared.entity.PaymentStatus.PAID
            """)
    BigDecimal findOutstandingBalanceByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            SELECT s FROM Sale s
            WHERE s.customer.id = :customerId
              AND s.paymentStatus != io.aygh.shared.entity.PaymentStatus.PAID
            ORDER BY s.soldAt ASC
            """)
    List<Sale> findUnpaidSalesByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            SELECT s FROM Sale s
            WHERE s.soldAt BETWEEN :start AND :end
            ORDER BY s.soldAt ASC, s.invoiceNumber ASC
            """)
    List<Sale> findSalesBookSales(@Param("start") Instant start, @Param("end") Instant end);
}
