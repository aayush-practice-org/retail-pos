package io.aygh.inventory.repository;

import io.aygh.inventory.entity.CategoryUnit;
import io.aygh.inventory.entity.UnitUsage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryUnitRepository extends JpaRepository<CategoryUnit, Long> {

    @EntityGraph(attributePaths = "unit")
    List<CategoryUnit> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "unit")
    List<CategoryUnit> findByCategoryIdAndUsage(Long categoryId, UnitUsage usage);

    boolean existsByCategoryIdAndUnitIdAndUsage(Long categoryId, Long unitId, UnitUsage usage);

    Optional<CategoryUnit> findByIdAndCategoryId(Long id, Long categoryId);

    boolean existsByUnitId(Long unitId);
}
