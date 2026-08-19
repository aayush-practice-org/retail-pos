package io.aygh.identity.repository;

import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findAllByOrderByCreatedAtAsc();

    /**
     * @param allRoles true to skip the role filter entirely; {@code role} is still bound
     *                 but never read (a null enum has no type Postgres can infer)
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:allRoles = TRUE OR u.role = :role)
              AND (:isActive IS NULL OR u.isActive = :isActive)
            """)
    Page<User> search(@Param("search") String search,
                      @Param("allRoles") boolean allRoles,
                      @Param("role") UserRole role,
                      @Param("isActive") Boolean isActive,
                      Pageable pageable);

    @Query("""
            SELECT COUNT(u) > 0 FROM User u
            WHERE LOWER(u.username) = LOWER(:username)
              AND (:excludeId IS NULL OR u.id <> :excludeId)
            """)
    boolean existsByUsername(@Param("username") String username, @Param("excludeId") UUID excludeId);

    @Query("""
            SELECT COUNT(u) > 0 FROM User u
            WHERE LOWER(u.email) = LOWER(:email)
              AND (:excludeId IS NULL OR u.id <> :excludeId)
            """)
    boolean existsByEmail(@Param("email") String email, @Param("excludeId") UUID excludeId);

    long countByRole(UserRole role);
}
