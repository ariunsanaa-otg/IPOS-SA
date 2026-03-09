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
@Table(name = "invoices")
public class Invoice {

    @Id
    @Column(name = "invoice_id", length = 10)
    private String invoiceId; // e.g., "197362"

    @Column(name = "order_id", length = 10, nullable = false)
    private String orderId; // FK to Order

    @Column(name = "account_id", length = 10, nullable = false)
    private String accountId; // FK to UserAccount

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate; // date the invoice was raised

    @Column(name = "amount_due", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountDue; // total amount for the invoice
}