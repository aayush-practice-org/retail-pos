package io.aygh.inventory.repository;

import io.aygh.inventory.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * The unit permissions are lazy so listing categories stays one query; the
     * detail view needs them all, so it fetches them up front rather than
     * paying a query per row.
     */
    @EntityGraph(attributePaths = {"categoryUnits", "categoryUnits.unit"})
    Optional<Category> findWithUnitsById(Long id);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProducts(Long categoryId);
}
