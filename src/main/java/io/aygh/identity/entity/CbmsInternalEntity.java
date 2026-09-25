package io.aygh.identity.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.util.UUID;

@Entity
@Table(name = "cbms_internal")
@SQLDelete(sql = "UPDATE cbms_internal SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CbmsInternalEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "tenant_slug", length = 63)
    private String tenantSlug;

    /** Empty until the admin enters the mart's CBMS credentials. */
    @Column(name = "cbms_username")
    private String cbmsUsername;

    @Column(name = "cbms_password")
    private String cbmsPassword;

    /**
     * Every mart is PAN registered unless someone says otherwise: VAT billing is
     * not being issued yet, so a setup without an explicit registration charges no VAT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_registration", nullable = false, length = 50)
    @Builder.Default
    private TaxRegistration taxRegistration = TaxRegistration.PAN_REGISTERED;

    /**
     * true  -> item rates / MRP already carry VAT (e.g. 100 net -> 88.50 taxable + 11.50 VAT)
     * false -> VAT is added on top of shelf price (e.g. 100 taxable + 13.00 VAT -> 113 net)
     */
    @Column(name = "tax_included", nullable = false)
    @Builder.Default
    private boolean taxIncluded = false;

    @Column(name = "pan", length = 50)
    private String pan;
}
