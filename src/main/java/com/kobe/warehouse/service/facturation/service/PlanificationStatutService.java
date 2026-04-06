package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.HistoriquePlanification;
import com.kobe.warehouse.domain.PlanificationFacturation;
import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.repository.HistoriquePlanificationRepository;
import com.kobe.warehouse.repository.PlanificationFacturationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean séparé pour les sauvegardes de statut en REQUIRES_NEW.
 * Nécessaire pour contourner la limitation Spring AOP (self-invocation).
 */
@Service
public class PlanificationStatutService {

    private final PlanificationFacturationRepository planificationRepository;
    private final HistoriquePlanificationRepository historiqueRepository;

    public PlanificationStatutService(
        PlanificationFacturationRepository planificationRepository,
        HistoriquePlanificationRepository historiqueRepository
    ) {
        this.planificationRepository = planificationRepository;
        this.historiqueRepository = historiqueRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sauvegarderSuccess(
        PlanificationFacturation plan, LocalDateTime debut, LocalDate periodeFin,
        Integer generationCode, LocalDateTime prochaineExecution
    ) {
        enregistrerHistorique(plan, debut, ExecutionStatut.SUCCESS, generationCode, null);
        plan.setDernierePeriodeFin(periodeFin);
        plan.setDerniereExecution(debut);
        plan.setDernierStatut(ExecutionStatut.SUCCESS);
        plan.setDernierMessage(null);
        plan.setProchaineExecution(prochaineExecution);
        planificationRepository.save(plan);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sauvegarderEchec(
        PlanificationFacturation plan, LocalDateTime debut,
        String message, LocalDateTime prochaineExecution
    ) {
        enregistrerHistorique(plan, debut, ExecutionStatut.ECHEC, null, message);
        plan.setDerniereExecution(debut);
        plan.setDernierStatut(ExecutionStatut.ECHEC);
        plan.setDernierMessage(message);
        plan.setProchaineExecution(prochaineExecution);
        planificationRepository.save(plan);
    }

    private void enregistrerHistorique(
        PlanificationFacturation plan, LocalDateTime debut,
        ExecutionStatut statut, Integer generationCode, String message
    ) {
        HistoriquePlanification h = new HistoriquePlanification();
        h.setPlanificationId(plan.getId());
        h.setExecutionDebut(debut);
        h.setExecutionFin(LocalDateTime.now());
        h.setStatut(statut);
        h.setGenerationCode(generationCode);
        h.setMessage(message);
        historiqueRepository.save(h);
    }
}
