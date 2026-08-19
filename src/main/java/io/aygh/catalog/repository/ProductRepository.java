package io.aygh.catalog.repository;

import io.aygh.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * @param allCategories true to skip the category filter entirely
     * @param categoryIds   the target category plus its descendants; never empty
     *                      (an IN list still has to bind even when unused)
     */
    @EntityGraph(attributePaths = {"category", "baseUnit"})
    @Query("""
            SELECT p FROM Product p
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:allCategories = TRUE OR p.category.id IN :categoryIds)
              AND (:isActive IS NULL OR p.isActive = :isActive)
            """)
    Page<Product> search(@Param("search") String search,
                         @Param("allCategories") boolean allCategories,
                         @Param("categoryIds") Collection<Long> categoryIds,
                         @Param("isActive") Boolean isActive,
                         Pageable pageable);

    @EntityGraph(attributePaths = {"category", "baseUnit"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategoryAndUnit(@Param("id") Long id);

    @Query("""
            SELECT COUNT(p) > 0 FROM Product p
            WHERE LOWER(p.name) = LOWER(:name)
              AND (:excludeId IS NULL OR p.id <> :excludeId)
            """)
    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    long countByCategoryId(Long categoryId);
}
