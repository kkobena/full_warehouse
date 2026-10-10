package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PilotageVenteLigneJour;
import org.springframework.data.repository.Repository;

/** Point d'entrée Spring Data de la ventilation ; tout le travail est dans {@link PilotageAnalyseRepositoryCustom}. */
public interface PilotageAnalyseRepository extends Repository<PilotageVenteLigneJour, Long>, PilotageAnalyseRepositoryCustom {}
