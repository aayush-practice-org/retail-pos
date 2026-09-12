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

    @Column(name = "cbms_username", nullable = false)
    private String cbmsUsername;

    @Column(name = "cbms_password", nullable = false)
    private String cbmsPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_registration", nullable = false, length = 50)
    private TaxRegistration taxRegistration;

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
