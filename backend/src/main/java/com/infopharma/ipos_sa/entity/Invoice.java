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
    private String invoiceId; // Primary key: e.g., "197362"

    // One Invoice corresponds to exactly one Order
    // Each Order generates exactly one Invoice
    @OneToOne(fetch = FetchType.LAZY) // Load the Order only when accessed
    @JoinColumn(name = "order_id", nullable = false) // FK column, cannot be null
    private Order order;

    // Many Invoices belong to one UserAccount
    // Each invoice is associated with exactly one account
    @ManyToOne(fetch = FetchType.LAZY) // Load the UserAccount only when accessed
    @JoinColumn(name = "account_id", nullable = false) // FK column, cannot be null
    private UserAccount account;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate; // Date the invoice was raised

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "amount_due", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountDue; // Total amount for the invoice
}