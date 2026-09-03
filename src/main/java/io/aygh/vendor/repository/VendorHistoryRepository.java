package io.aygh.vendor.repository;

import io.aygh.vendor.entity.VendorHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The vendor behind a history row is lazy, and every response carries its name,
 * so each read here fetches it up front. Left to the proxy it would be a query
 * per row.
 */
@Repository
public interface VendorHistoryRepository extends JpaRepository<VendorHistory, Long> {

    @EntityGraph(attributePaths = "vendor")
    Page<VendorHistory> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "vendor")
    Page<VendorHistory> findByVendorId(Long vendorId, Pageable pageable);

    @EntityGraph(attributePaths = "vendor")
    Optional<VendorHistory> findWithVendorById(Long id);

    long countByVendorId(Long vendorId);
}
