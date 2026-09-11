package io.aygh.customer.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * A customer of the mart.
 * <p>
 * Identified primarily by phone number for quick POS lookup at checkout.
 * Mandatory when buying on credit so that accounts receivable are properly tracked.
 */
@Entity
@Table(name = "customers")
@SQLDelete(sql = "UPDATE customers SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "pan_number", length = 30)
    private String panNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
