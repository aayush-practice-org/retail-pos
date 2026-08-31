package io.aygh.identity.repository;

import io.aygh.identity.entity.Admin;
import io.aygh.identity.entity.ProvisioningStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID>, JpaSpecificationExecutor<Admin> {

    /**
     * The user is mapped lazily so listing admins does not fan out into a query
     * per row; every read that renders an admin needs it, so it is fetched here.
     */
    @EntityGraph(attributePaths = "user")
    Optional<Admin> findWithUserById(UUID id);

    @EntityGraph(attributePaths = "user")
    Optional<Admin> findWithUserBySlug(String slug);

    boolean existsByCompanyNameIgnoreCase(String companyName);

    boolean existsByCompanyNameIgnoreCaseAndIdNot(String companyName, UUID id);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, UUID id);

    /**
     * Slugs are unique across soft-deleted rows too — the schema behind one is a
     * physical resource that a delete does not release — so this deliberately
     * ignores the soft-delete restriction the entity carries.
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM public.admins WHERE slug = :slug)", nativeQuery = true)
    boolean slugTaken(String slug);

    List<Admin> findByProvisioningStatus(ProvisioningStatus provisioningStatus);

    @Query("SELECT a.slug FROM Admin a")
    List<String> findAllSlugs();
}
