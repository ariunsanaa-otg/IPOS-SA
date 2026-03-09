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
    private Integer paymentId;

    @Column(name = "account_id", length = 10, nullable = false)
    private String accountId; // FK to UserAccount

    @Column(name = "invoice_id", length = 10, nullable = false)
    private String invoiceId; // FK to Invoice

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate; // date payment received

    @Column(name = "amount_paid", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "recorded_by", length = 100)
    private String recordedBy; // who entered the payment

    public enum PaymentMethod {
        BANK_TRANSFER,
        CARD,
        CHEQUE
    }
}