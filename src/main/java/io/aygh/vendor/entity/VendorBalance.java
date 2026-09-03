package io.aygh.vendor.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * One entry in a vendor's ledger.
 * <p>
 * Append-only: a mistake is corrected by posting the opposite entry, never by
 * editing the row that recorded it. That is why there is no update path through
 * the service layer and no optimistic-lock version — nothing ever contends for
 * a row that is written once.
 * <p>
 * {@code amount} is always positive; {@link #balanceType} decides which way it
 * moves the outstanding figure.
 */
@Entity
@Table(name = "vendor_balances")
@SQLDelete(sql = "UPDATE vendor_balances SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VendorBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Lazy: the ledger is listed far more often than the vendor behind it is
     * read, and the responses only ever need the id, which a proxy already has.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_type", nullable = false, length = 20)
    private BalanceType balanceType;
}
