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
    private String itemId; // e.g., "100 00001"

    @Column(length = 255, nullable = false)
    private String description; // e.g., "Paracetamol"

    @Column(name = "package_type", length = 50)
    private String packageType; // e.g., "box", "bottle"

    @Column(length = 20)
    private String unit; // e.g., "Caps", "ml"

    @Column(name = "units_in_pack")
    private Integer unitsInPack; // e.g., 20, 30

    @Column(name = "package_cost", precision = 10, scale = 2)
    private BigDecimal packageCost; // e.g., 0.10

    @Column
    private Integer availability; // packs available

    @Column(name = "min_stock_level")
    private Integer minStockLevel; // stock limit

    @Column(name = "reorder_buffer_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal reorderBufferPct = BigDecimal.valueOf(10.00); // default 10%
}