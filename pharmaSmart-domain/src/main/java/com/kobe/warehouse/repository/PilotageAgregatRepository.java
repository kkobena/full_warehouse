package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PilotageVenteJour;
import org.springframework.data.repository.Repository;

/** Point d'entrée Spring Data du recalcul des agrégats ; tout le travail est dans {@link PilotageAgregatRepositoryCustom}. */
public interface PilotageAgregatRepository extends Repository<PilotageVenteJour, Long>, PilotageAgregatRepositoryCustom {}
