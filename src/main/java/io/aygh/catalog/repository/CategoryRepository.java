package io.aygh.catalog.repository;

import io.aygh.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNull();

    List<Category> findByParentIdIn(Collection<Long> parentIds);

    /**
     * Names only have to be unique among siblings, so "Snacks" may sit under both
     * Grocery and Bakery without clashing.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE LOWER(c.name) = LOWER(:name)
              AND (:parentId IS NULL AND c.parent IS NULL OR c.parent.id = :parentId)
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    boolean existsSiblingWithName(@Param("name") String name,
                                  @Param("parentId") Long parentId,
                                  @Param("excludeId") Long excludeId);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children")
    List<Category> findAllWithChildren();
}
