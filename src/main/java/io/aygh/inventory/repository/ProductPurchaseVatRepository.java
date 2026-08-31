package io.aygh.inventory.repository;

import io.aygh.inventory.entity.ProductPurchaseVat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPurchaseVatRepository extends JpaRepository<ProductPurchaseVat, Long> {

    List<ProductPurchaseVat> findByProductPurchaseUnitIdOrderByEffectiveFromDesc(Long productPurchaseUnitId);

    /** The rate still open — the one a purchase made today is taxed at. */
    Optional<ProductPurchaseVat> findByProductPurchaseUnitIdAndEffectiveToIsNull(Long productPurchaseUnitId);

    /** The rate that was in force on a given day, for reprinting an old document. */
    default Optional<ProductPurchaseVat> findEffectiveOn(Long productPurchaseUnitId, LocalDate date) {
        return findByProductPurchaseUnitIdOrderByEffectiveFromDesc(productPurchaseUnitId).stream()
                .filter(rate -> rate.appliesOn(date))
                .findFirst();
    }
}
