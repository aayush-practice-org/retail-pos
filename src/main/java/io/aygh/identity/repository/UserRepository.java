package io.aygh.identity.repository;

import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Soft-deleted accounts are invisible here: {@code BaseEntity} carries an
 * {@code @SQLRestriction("deleted_at IS NULL")}, so every method below is
 * already scoped to live accounts.
 * <p>
 * Uniqueness of username and email is installation-wide, not per tenant, because
 * a login arrives before any tenant is known and has to identify exactly one
 * account.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Uniqueness check for an update: everyone but the account being edited. */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByRole(UserRole role);

    /** Scoped lookup — the only safe way for an admin to reach one of its own staff. */
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);

    /**
     * Soft deletes every account belonging to a mart, its admin included.
     * <p>
     * A bulk update rather than a loop of {@code delete} calls: retiring a mart
     * with a hundred staff should be one statement, and there is nothing
     * per-account to decide.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP "
            + "WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL")
    int softDeleteByTenantId(UUID tenantId);
}
