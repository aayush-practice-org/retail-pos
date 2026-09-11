package io.aygh.inventory.repository;

import io.aygh.inventory.entity.ProductPurchaseVat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPurchaseVatRepository extends JpaRepository<ProductPurchaseVat, Long> {

    /** All VAT rates recorded for a purchase unit, newest first. */
    List<ProductPurchaseVat> findByProductPurchaseUnitIdOrderByCreatedAtDesc(Long productPurchaseUnitId);

    /** The current (most recently set) active rate for a purchase unit. */
    Optional<ProductPurchaseVat> findTopByProductPurchaseUnitIdOrderByCreatedAtDesc(Long productPurchaseUnitId);
}
