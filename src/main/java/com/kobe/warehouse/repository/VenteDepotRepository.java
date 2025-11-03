package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.VenteDepot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenteDepotRepository extends JpaRepository<VenteDepot, SaleId> {

}
