package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ordonnance.VentePrescripteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentePrescripteurRepository extends JpaRepository<VentePrescripteur, VentePrescripteur.Id> {}
