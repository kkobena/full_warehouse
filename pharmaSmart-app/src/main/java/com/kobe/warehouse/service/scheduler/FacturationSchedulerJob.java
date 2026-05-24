package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.PlanificationFacturation;
import com.kobe.warehouse.repository.PlanificationFacturationRepository;
import com.kobe.warehouse.service.facturation.service.PlanificationFacturationService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FacturationSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(FacturationSchedulerJob.class);

    private final PlanificationFacturationRepository planificationRepository;
    private final PlanificationFacturationService planificationService;

    public FacturationSchedulerJob(
        PlanificationFacturationRepository planificationRepository,
        PlanificationFacturationService planificationService
    ) {
        this.planificationRepository = planificationRepository;
        this.planificationService = planificationService;
    }

    public void executerPlanificationsEnAttente() {
        List<PlanificationFacturation> planifications = planificationRepository
            .findByActifTrueAndProchaineExecutionBefore(LocalDateTime.now());

        if (planifications.isEmpty()) {
            return;
        }

        log.info("{} planification(s) de facturation à exécuter", planifications.size());
        for (PlanificationFacturation plan : planifications) {
            planificationService.executerPlanificationScheduled(plan);
        }
    }
}
