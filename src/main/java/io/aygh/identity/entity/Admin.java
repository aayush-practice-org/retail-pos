package io.aygh.identity.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

import java.time.Instant;
import java.util.UUID;

/**
 * One mart: the company details behind an {@link UserRole#ADMIN} account, and
 * the schema its data lives in.
 * <p>
 * The row shares its primary key with the admin's user row ({@code @MapsId}), so
 * there is no separate identifier for "the tenant" — the admin's id is it, and
 * that is the value every account in the mart carries as its
 * {@link User#getTenantId()}.
 */
@Entity
@Table(name = "admins", schema = "public")
@SQLDelete(sql = "UPDATE public.admins SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin extends BaseEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    // ── Company ───────────────────────────────────────────────────────────

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "company_phone", length = 30)
    private String companyPhone;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    /**
     * The Postgres schema holding this mart's data, derived from the company name
     * at registration. Never updated: renaming it would leave the data behind in
     * a schema nothing points at any more.
     */
    @Column(name = "slug", nullable = false, updatable = false, length = 63)
    private String slug;

    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    // ── Schema provisioning ───────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_status", nullable = false, length = 20)
    @Builder.Default
    private ProvisioningStatus provisioningStatus = ProvisioningStatus.PENDING;

    /** Why the last provisioning attempt failed, truncated to fit the column. */
    @Column(name = "provisioning_error", length = 1000)
    private String provisioningError;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    public void markProvisioned() {
        this.provisioningStatus = ProvisioningStatus.READY;
        this.provisioningError = null;
        this.provisionedAt = Instant.now();
    }

    public void markProvisioningFailed(String reason) {
        this.provisioningStatus = ProvisioningStatus.FAILED;
        this.provisioningError = reason == null ? null : reason.substring(0, Math.min(reason.length(), 1000));
    }
}
