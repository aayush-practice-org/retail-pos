package io.aygh.sales.entity;

import io.aygh.shared.entity.BaseEntity;
import io.aygh.shared.entity.TaxScheme;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Goods handed back against a bill — the IRD's credit note.
 * <p>
 * Numbered on its own sequence and always tied to the sale it reverses. Its
 * amounts are prorated from that sale rather than priced afresh, so a return
 * credits exactly what was charged, under the VAT treatment the bill was raised
 * with, whatever the shelf price or the mart's registration is today.
 */
@Entity
@Table(name = "sales_returns")
@SQLDelete(sql = "UPDATE sales_returns SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReturn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credit_note_number", nullable = false, length = 32, updatable = false)
    private String creditNoteNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, updatable = false)
    private Sale sale;

    @Column(name = "returned_at", nullable = false)
    private Instant returnedAt;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    // ── Snapshotted from the bill ────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_scheme", nullable = false, length = 20)
    private TaxScheme taxScheme;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_pan", length = 30)
    private String customerPan;

    // ── Nepali calendar ───────────────────────────────────────────────────

    /** The BS date of the credit note, e.g. "2081.09.05". */
    @Column(name = "nepali_date", length = 20)
    private String nepaliDate;

    /** The IRD fiscal year the credit note falls in. */
    @Column(name = "fiscal_year", length = 20)
    private String fiscalYear;

    // ── Money ─────────────────────────────────────────────────────────────

    @Column(name = "sub_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal subTotal;

    /** The share of the bill-level discount the returned lines carried. */
    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "vat_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    /** What is credited to the customer: taxable amount plus VAT. */
    @Column(name = "net_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal netTotal;

    /**
     * Cash handed back. The rest of {@link #netTotal} came off what the
     * customer still owed on the bill.
     */
    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** Whether the IRD's CBMS accepted this credit note. */
    @Column(name = "sync_with_ird", nullable = false)
    @Builder.Default
    private Boolean syncWithIrd = false;

    @Column(name = "remark", length = 255)
    private String remark;

    @OneToMany(mappedBy = "salesReturn", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SalesReturnItem> items = new ArrayList<>();

    public void addItem(SalesReturnItem item) {
        items.add(item);
        item.setSalesReturn(this);
    }
}
