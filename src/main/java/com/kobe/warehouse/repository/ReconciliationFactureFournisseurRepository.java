package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ReconciliationFactureFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationFactureFournisseurRepository
    extends JpaRepository<ReconciliationFactureFournisseur, Integer> {
}
