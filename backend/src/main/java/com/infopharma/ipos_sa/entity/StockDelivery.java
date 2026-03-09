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
    private Integer deliveryId;

    @Column(name = "item_id", length = 15, nullable = false)
    private String itemId; // FK to CatalogueItem

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived; // amount of stock delivered

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "recorded_by", length = 100)
    private String recordedBy; // admin who recorded the delivery
}