package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "discount_tiers")
public class DiscountTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Integer tierId;

    // Many DiscountTiers belong to one flexible DiscountPlan.
    // Only flexible plans have tiers; fixed plans do not have associated DiscountTiers.
    @ManyToOne(fetch = FetchType.LAZY) // Only load the DiscountPlan when accessed
    @JoinColumn(name = "discount_plan_id", nullable = false) // FK column, cannot be null
    private DiscountPlan discountPlan;

    // Precision 10: maximum total digits (including both sides of decimal)
    // Scale 2: maximum 2 digits after the decimal point
    @Column(name = "min_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal minValue;

    // Precision 10: maximum total digits (including both sides of decimal)
    // Scale 2: maximum 2 digits after the decimal point
    @Column(name = "max_value", precision = 10, scale = 2)
    private BigDecimal maxValue; // nullable, null = no upper bound

    // Precision 5: maximum total digits (including digits before and after decimal)
    // Scale 2: maximum 2 digits after the decimal point
    // Example: 1.00 = 1%, 12.50 = 12.5%
    @Column(name = "discount_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountRate; // e.g., 1.00 = 1%
}