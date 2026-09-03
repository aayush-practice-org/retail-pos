package io.aygh.vendor.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;

/**
 * A supplier the mart buys from.
 * <p>
 * Master data only: what the vendor owes and is owed is not a column here but a
 * ledger of {@link VendorBalance} rows, so a balance can always be explained by
 * the entries that produced it rather than by whoever last wrote the field.
 */
@Entity
@Table(name = "vendors")
@SQLDelete(sql = "UPDATE vendors SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    /** The vendor's tax registration. Optional, but unique across the mart when given. */
    @Column(name = "pan_number", length = 30)
    private String panNumber;

    /**
     * Inverse side, deliberately not cascaded: the trail of what was bought from
     * this vendor outlives the decision to stop buying from it.
     */
    @OneToMany(mappedBy = "vendor", fetch = FetchType.LAZY)
    @Builder.Default
    private List<VendorHistory> histories = new ArrayList<>();
}
