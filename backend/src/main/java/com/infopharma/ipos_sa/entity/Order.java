package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id", length = 10)
    private String orderId; // Primary key: e.g., "IP2034"

    // Many Orders belong to one UserAccount.
    // Each order is associated with exactly one account.
    @ManyToOne(fetch = FetchType.LAZY) // Only load the UserAccount when accessed
    @JoinColumn(name = "account_id", nullable = false) // FK column, cannot be null
    private UserAccount account;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate; // Date when the order was placed

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "total_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalValue; // Total value of the order

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // Order workflow status

    @Column(name = "dispatched_by", length = 100)
    private String dispatchedBy; // Name of the staff who dispatched the order

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate; // Date when order was dispatched

    @Column(length = 100)
    private String courier; // Courier name

    @Column(name = "courier_ref", length = 100)
    private String courierRef; // Reference number from courier

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery; // Expected delivery date

    @Column(name = "delivery_date")
    private LocalDate deliveryDate; // Actual delivery date

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "discount_applied", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountApplied = BigDecimal.valueOf(0.00); // Any discount applied to order

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus; // Payment workflow status

    // -------------------- ENUMS --------------------
    public enum OrderStatus {
        ACCEPTED,
        BEING_PROCESSED,
        DISPATCHED,
        DELIVERED
    }

    public enum PaymentStatus {
        PENDING,
        PAID
    }
}