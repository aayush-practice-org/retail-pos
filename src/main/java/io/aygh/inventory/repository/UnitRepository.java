package io.aygh.inventory.repository;

import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Lives in the tenant schema, so nothing here filters by tenant — the
 * connection's schema is the tenant. Soft-deleted rows are already invisible
 * via the {@code @SQLRestriction} on {@code BaseEntity}.
 */
@Repository
public interface UnitRepository extends JpaRepository<Unit, Long>, JpaSpecificationExecutor<Unit> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySymbolIgnoreCase(String symbol);

    boolean existsBySymbolIgnoreCaseAndIdNot(String symbol, Long id);

    List<Unit> findByMeasurementType(MeasurementType measurementType);

    /** The unit every other unit of this type converts through. */
    Optional<Unit> findByMeasurementTypeAndReferenceUnitIsTrue(MeasurementType measurementType);

    /**
     * Resolving what someone typed. An importer writing "kg" should not have to
     * look an id up first, and a symbol is what appears on the shelf label.
     */
    Optional<Unit> findBySymbolIgnoreCase(String symbol);

    Optional<Unit> findByNameIgnoreCase(String name);
}
