package io.aygh.vendor.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

/**
 * That a purchase was made from a vendor — the vendor's side of the purchase
 * trail, readable without joining through the purchasing tables.
 * <p>
 * {@code purchaseId} carries no foreign key on purpose: the purchase module is
 * not built yet, and pointing at a table that does not exist would stop this
 * migration from applying. It becomes a real reference when purchasing lands.
 */
@Entity
@Table(name = "vendor_histories")
@SQLDelete(sql = "UPDATE vendor_histories SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VendorHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "purchase_id")
    private Long purchaseId;
}
