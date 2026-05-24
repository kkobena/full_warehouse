package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.PlanificationCertificationFne;
import com.kobe.warehouse.repository.PlanificationCertificationFneRepository;
import com.kobe.warehouse.service.fne.model.CertificationFneResult;
import com.kobe.warehouse.service.fne.service.FneService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CertificationFneSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(CertificationFneSchedulerJob.class);

    private final PlanificationCertificationFneRepository planificationRepository;
    private final FneService fneService;
    private final CertificationFneStatutService statutService;

    public CertificationFneSchedulerJob(
        PlanificationCertificationFneRepository planificationRepository,
        FneService fneService,
        CertificationFneStatutService statutService
    ) {
        this.planificationRepository = planificationRepository;
        this.fneService = fneService;
        this.statutService = statutService;
    }

    /**
     * Point d'entrée appelé par {@link JobOrchestrationService}.
     * Aucune transaction englobante : chaque facture est commitée indépendamment.
     */
    public void executerCertificationsPendantes() {
        LocalDateTime now = LocalDateTime.now();

        // Plans actifs dont la prochaineExecution est échue
        List<PlanificationCertificationFne> planifications = planificationRepository
            .findByActifTrueAndProchaineExecutionBefore(now);

        // Plans actifs dont prochaineExecution n'a jamais été calculée (première activation)
        planifications.addAll(planificationRepository.findByActifTrueAndProchaineExecutionIsNull());

        if (planifications.isEmpty()) {
            return;
        }

        log.info("{} planification(s) de certification FNE à exécuter", planifications.size());
        for (PlanificationCertificationFne plan : planifications) {
            executerUnePlanification(plan);
        }
    }

    private void executerUnePlanification(PlanificationCertificationFne plan) {
        LocalDateTime debut = LocalDateTime.now();
        log.info("Certification FNE planifiée: {} (id={})", plan.getLibelle(), plan.getId());
        try {
            CertificationFneResult result = fneService.certifierFacturesPendantes(plan);
            // Bean séparé → proxy Spring → REQUIRES_NEW effectif (pas de self-invocation)
            statutService.sauvegarderStatut(plan, debut, result);
        } catch (Exception e) {
            log.error("Erreur inattendue certification FNE planification {}: {}", plan.getId(), e.getMessage(), e);
            statutService.sauvegarderEchec(plan, debut, e.getMessage());
        }
    }
}
