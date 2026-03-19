package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId; // Primary key for OrderItem

    // Many OrderItems belong to one Order
    // Each OrderItem is associated with exactly one Order
    @ManyToOne(fetch = FetchType.LAZY) // Only load the Order when accessed
    @JoinColumn(name = "order_id", nullable = false) // FK column, cannot be null
    private Order order;

    // Many OrderItems refer to one CatalogueItem
    // Each OrderItem is associated with exactly one CatalogueItem
    @ManyToOne(fetch = FetchType.LAZY) // Only load the CatalogueItem when accessed
    @JoinColumn(name = "item_id", nullable = false) // FK column, cannot be null
    private CatalogueItem item;

    @Column(nullable = false)
    private Integer quantity; // Number of units ordered

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "unit_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitCost; // Cost per unit at order time

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "total_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCost; // Computed as quantity × unitCost
}