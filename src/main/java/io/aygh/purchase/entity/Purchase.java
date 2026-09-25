package io.aygh.purchase.entity;

import io.aygh.shared.entity.BaseEntity;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.TaxScheme;
import io.aygh.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Goods bought from a vendor, on one of the vendor's bills.
 * <p>
 * The money columns are all stored rather than recomputed on read. A purchase is
 * a record of what was agreed on a day: recalculating it later against today's
 * prices or today's VAT rate would quietly restate history.
 * <p>
 * Header and lines are one aggregate — there is no purchase without at least one
 * line, and no line that belongs to two purchases — so the items cascade and are
 * orphan-removed.
 */
@Entity
@Table(name = "purchases")
@SQLDelete(sql = "UPDATE purchases SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Purchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /**
     * The vendor's own bill number, as printed on the paper that came with the goods.
     */
    @Column(name = "bill_number", nullable = false, length = 64)
    private String billNumber;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_scheme", nullable = false, length = 20)
    private TaxScheme taxScheme;

    // ── Money ─────────────────────────────────────────────────────────────

    /**
     * Sum of the lines, before discount and before VAT.
     */
    @Column(name = "sub_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * What VAT is charged on: sub total less discount.
     */
    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "vat_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    /**
     * Taxable amount plus VAT — what the vendor is actually owed.
     */
    @Column(name = "net_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal netTotal;

    /** Sent back to the vendor on purchase returns, at what it cost. */
    @Column(name = "returned_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    @Column(name = "remark", length = 255)
    private String remark;

    @OneToMany(mappedBy = "purchase", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseItem> items = new ArrayList<>();

    public void addItem(PurchaseItem item) {
        items.add(item);
        item.setPurchase(this);
    }

    /**
     * Whether the goods were taken on credit and the vendor is still owed.
     */
    public boolean isOnCredit() {
        return paymentMethod == PaymentMethod.CREDIT;
    }
}
