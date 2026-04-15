package com.infopharma.ipos_sa.repository;

import com.infopharma.ipos_sa.entity.CatalogueItem;
import com.infopharma.ipos_sa.entity.StockDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockDeliveryRepository extends JpaRepository<StockDelivery, Integer> {
    List<StockDelivery> findByDeliveryDateBetween(LocalDate from, LocalDate to);
    List<StockDelivery> findByItem(CatalogueItem item);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM stock_deliveries WHERE item_id = :itemId", nativeQuery = true)
    void deleteByItemId(@Param("itemId") String itemId);
}
