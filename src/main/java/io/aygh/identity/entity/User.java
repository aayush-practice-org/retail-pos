package io.aygh.identity.entity;

import io.aygh.shared.entity.BaseEntity;
import io.aygh.shared.entity.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "users", schema = "public")
@SQLDelete(sql = "UPDATE public.users SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    /**
     * Assigned in Java rather than by the database, because an admin's own id is
     * also its tenant id: the row cannot be inserted until the value is known,
     * and a generated key is only known afterwards.
     */
    @Id
    private UUID id;

    // ── Credentials ───────────────────────────────────────────────────────

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false)
    private String email;

    // ── Authorisation ─────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 40)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // ── Tenancy ───────────────────────────────────────────────────────────

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "tenant_slug", length = 63)
    private String tenantSlug;

    // ── Personal information ──────────────────────────────────────────────

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "country", length = 100)
    private String country;

    // ── Contact information ───────────────────────────────────────────────

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    // ── Address information ───────────────────────────────────────────────

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    /**
     * Safety net for any path that builds an account without asking for an id first.
     */
    @PrePersist
    void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    // ── UserDetails ───────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role == null ? List.of() : List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status.canSignIn() && getDeletedAt() == null;
    }
}
