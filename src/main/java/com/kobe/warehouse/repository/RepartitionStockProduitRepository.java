package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RepartitionStockProduitRepository extends JpaRepository<RepartitionStockProduit, Integer>, RepartitionStockProduitRepositoryCustom {
}
