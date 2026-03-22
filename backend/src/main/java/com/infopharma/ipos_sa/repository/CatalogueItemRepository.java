package com.infopharma.ipos_sa.repository;

import com.infopharma.ipos_sa.entity.CatalogueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogueItemRepository extends JpaRepository<CatalogueItem,String> {
}
