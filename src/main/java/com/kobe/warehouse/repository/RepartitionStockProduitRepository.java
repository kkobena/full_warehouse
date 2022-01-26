package com.kobe.warehouse.repository;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RepartitionStockProduitRepository extends JpaRepository<RepartitionStockProduit, Long> {
}
