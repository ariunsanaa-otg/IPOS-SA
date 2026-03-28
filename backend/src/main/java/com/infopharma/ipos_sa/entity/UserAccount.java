package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(length = 50, nullable = false)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType; // merchant, admin, manager

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus; // normal, suspended, in_default

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(length = 255)
    private String address;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 20)
    private String fax;

    @Column(length = 100, nullable = false)
    private String email;

    // creditLimit: maximum 10 digits total, 2 decimal places (e.g., 12345678.90)
    @Column(name = "credit_limit", precision = 10, scale = 2)
    private BigDecimal creditLimit;

    // Many UserAccounts can share the same DiscountPlan. Each account references exactly one plan.
    @ManyToOne(fetch = FetchType.LAZY) //Lazy loading avoids unnecessary database queries and reduces memory usage.
    @JoinColumn(name = "discount_plan_id", nullable = true) // FK column
    private DiscountPlan discountPlan;

    // balance: total 10 digits, 2 decimal places (e.g., 12345678.90); cannot be null, default 0.00
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.valueOf(0.00);

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    // One UserAccount can have many MonthlyDiscounts.
    // 'mappedBy = "account"' means that the link (foreign key) is stored in MonthlyDiscount.
    // This side just “looks at” the list of discounts, it doesn’t own the database column.
    // Lazy loading: the discounts are only loaded from the database when you actually use this list.
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<MonthlyDiscount> monthlyDiscounts;


    // Enums for account_type and account_status
    public enum AccountType { MERCHANT, ADMIN, MANAGER }
    public enum AccountStatus { NORMAL, SUSPENDED, IN_DEFAULT }
}