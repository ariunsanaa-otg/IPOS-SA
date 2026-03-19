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
@Table(
        name = "monthly_discounts",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"account_id", "month_year"} // Ensures one discount per account per month
        )
)
public class MonthlyDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_discount_id")
    private Integer monthlyDiscountId; // Primary key

    // Many MonthlyDiscounts belong to one UserAccount.
    // A discount is always associated with a single account.
    @ManyToOne(fetch = FetchType.LAZY) // Only load the UserAccount when accessed
    @JoinColumn(name = "account_id", nullable = false) // FK column, cannot be null
    private UserAccount account;

    @Column(name = "month_year", nullable = false)
    private LocalDate monthYear; // The calendar month this discount applies to

    // Precision 10: maximum total digits (before + after decimal)
    // Scale 2: maximum 2 digits after the decimal point
    @Column(name = "total_orders_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalOrdersValue;
    // Total value of all orders in the month; used to calculate tier

    // Precision 5: maximum total digits (before + after decimal)
    // Scale 2: maximum 2 digits after the decimal point
    @Column(name = "discount_rate_applied", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountRateApplied;
    // The tier rate that applied (e.g., 1.00 = 1%)

    // Precision 10: maximum total digits (before + after decimal)
    // Scale 2: maximum 2 digits after the decimal point
    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount;
    // Calculated discount value for this month

    // Enum stored as string in DB
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_method", nullable = false)
    private SettlementMethod settlementMethod;
    // How the discount is paid: CHEQUE or ORDER_DEDUCTION

    @Column(nullable = false)
    private Boolean settled = false;
    // Whether the discount has been paid out / deducted

    // Enum for the settlement method
    public enum SettlementMethod {
        CHEQUE, ORDER_DEDUCTION
    }
}