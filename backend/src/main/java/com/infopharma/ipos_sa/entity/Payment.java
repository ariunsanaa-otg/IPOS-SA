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
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId; // Primary key: auto-incremented

    // Many Payments belong to one UserAccount
    // Each payment is associated with exactly one account
    @ManyToOne(fetch = FetchType.LAZY) // Load the UserAccount only when accessed
    @JoinColumn(name = "account_id", nullable = false) // FK column, cannot be null
    private UserAccount account;

    // Many Payments belong to one Invoice
    // Each payment is associated with exactly one invoice
    @ManyToOne(fetch = FetchType.LAZY) // Load the Invoice only when accessed
    @JoinColumn(name = "invoice_id", nullable = false) // FK column, cannot be null
    private Invoice invoice;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate; // Date the payment was received

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "amount_paid", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaid; // Amount paid for this payment

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod; // Method of payment

    @Column(name = "recorded_by", length = 100)
    private String recordedBy; // Staff who entered the payment

    // -------------------- ENUM --------------------
    public enum PaymentMethod {
        BANK_TRANSFER,
        CARD,
        CHEQUE
    }
}