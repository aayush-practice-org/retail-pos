package io.aygh.vendor.repository;

import io.aygh.vendor.entity.VendorBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorBalanceRepository extends JpaRepository<VendorBalance, Long> {

    Page<VendorBalance> findByVendorId(Long vendorId, Pageable pageable);

    boolean existsByVendorId(Long vendorId);

    /**
     * All three totals in one pass. Three separate {@code SUM} queries would say
     * the same thing, but a summary is read on every vendor page and there is no
     * reason for it to cost three round trips.
     * <p>
     * No {@code ELSE} on the cases: {@code SUM} skips nulls, so an entry of the
     * wrong type contributes nothing rather than a zero, and the result keeps the
     * column's own numeric type.
     */
    @Query("""
            SELECT new io.aygh.vendor.repository.VendorBalanceTotals(
                       SUM(CASE WHEN b.balanceType = io.aygh.vendor.entity.BalanceType.PAYABLE
                                THEN b.amount END),
                       SUM(CASE WHEN b.balanceType = io.aygh.vendor.entity.BalanceType.RECEIVABLE
                                THEN b.amount END),
                       SUM(CASE WHEN b.balanceType = io.aygh.vendor.entity.BalanceType.SETTLEMENT
                                THEN b.amount END))
            FROM VendorBalance b
            WHERE b.vendor.id = :vendorId
            """)
    VendorBalanceTotals totalsFor(@Param("vendorId") Long vendorId);
}
