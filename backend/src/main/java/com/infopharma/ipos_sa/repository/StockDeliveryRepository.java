package com.infopharma.ipos_sa.repository;

import com.infopharma.ipos_sa.entity.StockDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDeliveryRepository extends JpaRepository<StockDelivery,Integer> {
}
