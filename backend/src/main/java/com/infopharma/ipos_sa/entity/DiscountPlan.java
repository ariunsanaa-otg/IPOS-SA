package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "discount_plans")
public class DiscountPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_plan_id")
    private Integer discountPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType;

    @OneToMany(mappedBy = "discountPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscountTier> tiers;

    public enum PlanType {
        FIXED, FLEXIBLE
    }
}