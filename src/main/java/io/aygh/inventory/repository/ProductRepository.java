package io.aygh.inventory.repository;

import io.aygh.inventory.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCaseAndIdNot(String productCode, Long id);

    boolean existsByNameIgnoreCaseAndCategoryId(String name, Long categoryId);

    boolean existsByNameIgnoreCaseAndCategoryIdAndIdNot(String name, Long categoryId, Long id);

    /**
     * The detail view's roots. The two unit collections are deliberately absent:
     * both are bags, and Hibernate refuses to fetch two of them in one query
     * ({@code MultipleBagFetchException}). They load lazily inside the reading
     * transaction instead — two extra queries for one product, which is what the
     * single-row view can afford and the list cannot.
     */
    @EntityGraph(attributePaths = {"category", "baseUnit"})
    Optional<Product> findDetailById(Long id);

    @EntityGraph(attributePaths = {"category", "baseUnit"})
    Optional<Product> findWithCategoryById(Long id);

    long countByCategoryId(Long categoryId);
}
