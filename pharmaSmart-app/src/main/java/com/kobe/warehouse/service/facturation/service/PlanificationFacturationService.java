package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.PlanificationFacturation;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.HistoriquePlanificationDto;
import com.kobe.warehouse.service.facturation.dto.PlanificationDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanificationFacturationService {
    List<PlanificationDto> findAll();

    PlanificationDto create(PlanificationDto dto);

    PlanificationDto update(Integer id, PlanificationDto dto);

    void toggleActif(Integer id);

    void delete(Integer id);

    FactureEditionResponse executerMaintenant(Integer id);

    Page<HistoriquePlanificationDto> getHistorique(Integer id, Pageable pageable);

    /**
     * Exécute une planification depuis le scheduler : met à jour derniereExecution,
     * dernierStatut, prochaineExecution et enregistre l'historique.
     * Avance toujours prochaineExecution (même en cas d'erreur) pour éviter les retries infinis.
     */
    void executerPlanificationScheduled(PlanificationFacturation plan);
}
