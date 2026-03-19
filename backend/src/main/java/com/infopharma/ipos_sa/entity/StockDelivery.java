package com.infopharma.ipos_sa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stock_deliveries")
public class StockDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Integer deliveryId; // Primary key: auto-incremented

    // Many StockDeliveries belong to one CatalogueItem
    // Each delivery is associated with exactly one catalogue item
    @ManyToOne(fetch = FetchType.LAZY) // Load CatalogueItem only when accessed
    @JoinColumn(name = "item_id", nullable = false) // FK column, cannot be null
    private CatalogueItem item;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived; // Amount of stock delivered

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate; // Date stock was delivered

    @Column(name = "recorded_by", length = 100)
    private String recordedBy; // Admin who recorded the delivery
}