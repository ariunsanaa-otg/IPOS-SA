package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "catalogue_items")
public class CatalogueItem {

    @Id
    @Column(name = "item_id", length = 15)
    private String itemId; // Primary key: Appendix 1, e.g., "100 00001"

    @Column(length = 255, nullable = false)
    private String description; // Item description, e.g., "Paracetamol"

    @Column(name = "package_type", length = 50, nullable = false)
    private String packageType; // Package type, e.g., "box", "bottle"

    @Column(length = 20, nullable = false)
    private String unit; // Unit of measure, e.g., "Caps", "ml"

    @Column(name = "units_in_pack", nullable = false)
    private Integer unitsInPack; // Number of units per pack

    // Precision 10, scale 2: e.g., 12345678.90
    @Column(name = "package_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal packageCost; // Cost per pack in £

    @Column(nullable = false)
    private Integer availability; // Packs available in stock

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel; // Minimum stock limit (not shown to merchants)

    // Precision 5, scale 2: e.g., 10.00 = 10%
    @Column(name = "reorder_buffer_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal reorderBufferPct = BigDecimal.valueOf(10.00);
    // Reorder buffer percentage, default 10%; can be adjusted per item (10–50%)
}