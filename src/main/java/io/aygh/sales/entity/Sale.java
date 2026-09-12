package io.aygh.sales.entity;

import io.aygh.customer.entity.Customer;
import io.aygh.shared.entity.BaseEntity;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.entity.TaxScheme;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Goods sold, and the invoice raised for them.
 * <p>
 * There is no separate invoice table. In a mart the sale <em>is</em> the
 * invoice: they are created together, never one without the other, and splitting
 * them would buy nothing but a join and the chance of the two disagreeing.
 * <p>
 * Every money column is stored as struck. A reprint of last month's bill must
 * show last month's prices, not today's.
 */
@Entity
@Table(name = "sales")
@SQLDelete(sql = "UPDATE sales SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The number printed on the bill, unique across the mart and allocated in
     * sequence. Never reused — a gap is a voided bill, and that is information.
     */
    @Column(name = "invoice_number", nullable = false, length = 32, updatable = false)
    private String invoiceNumber;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private SaleChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_scheme", nullable = false, length = 20)
    private TaxScheme taxScheme;

    // ── Customer ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Kept on the sale itself for snapshotting: a VAT bill needs
     * the name and PAN as they were given at the till that day.
     */
    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "customer_pan", length = 30)
    private String customerPan;

    // ── Money ─────────────────────────────────────────────────────────────

    @Column(name = "sub_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "vat_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    /**
     * What the customer owes: taxable amount plus VAT.
     */
    @Column(name = "net_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal netTotal;

    // ── Payment ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * Cash handed back. Stored because the receipt has to print it.
     */
    @Column(name = "change_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(name = "remark", length = 255)
    private String remark;

    @OneToMany(mappedBy = "sale", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    /**
     * Applies a tender and derives the status from it, so the two can never
     * disagree. Anything over the net total is change, not an overpayment: a till
     * takes a 1000 note for a 780 bill every day.
     */
    public void settle(BigDecimal tendered) {
        BigDecimal paid = tendered == null ? BigDecimal.ZERO : tendered;

        if (paid.compareTo(netTotal) >= 0) {
            paidAmount = netTotal;
            changeAmount = paid.subtract(netTotal);
            paymentStatus = PaymentStatus.PAID;
            return;
        }

        paidAmount = paid;
        changeAmount = BigDecimal.ZERO;
        paymentStatus = paid.signum() > 0 ? PaymentStatus.PARTIAL : PaymentStatus.UNPAID;
    }

    /**
     * Still owed on this bill.
     */
    public BigDecimal dueAmount() {
        return netTotal.subtract(paidAmount);
    }
}
