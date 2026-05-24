package com.kobe.warehouse.service.scheduler;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Gère uniquement les jobs lourds (Facturation, FNE, Tournant) quand le module
 * {@code pharmaSmart-batch} est déployé séparément ({@code pharma-smart.batch.enabled=true}).
 *
 * <p>Les jobs légers (SEMOIS, Classification, Snapshot, etc.) sont alors pris en charge
 * par {@code NightlyPipelineScheduler} dans {@code pharmaSmart-batch}.
 */
@ConditionalOnProperty(name = "pharma-smart.batch.enabled", havingValue = "true")
@Service
public class HeavyJobsOrchestrationService {

    private static final Logger LOG = LoggerFactory.getLogger(HeavyJobsOrchestrationService.class);

    private final FacturationSchedulerJob facturationSchedulerJob;
    private final CertificationFneSchedulerJob certificationFneSchedulerJob;
    private final TournantSchedulerService tournantSchedulerService;

    public HeavyJobsOrchestrationService(
        FacturationSchedulerJob facturationSchedulerJob,
        CertificationFneSchedulerJob certificationFneSchedulerJob,
        TournantSchedulerService tournantSchedulerService
    ) {
        this.facturationSchedulerJob = facturationSchedulerJob;
        this.certificationFneSchedulerJob = certificationFneSchedulerJob;
        this.tournantSchedulerService = tournantSchedulerService;
    }

    @Scheduled(cron = "${pharma-smart.jobs.nightly-pipeline-cron:0 30 9 * * *}")
    public void runHeavyPipeline() {
        execute("NIGHTLY");
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        execute("STARTUP");
    }

    private void execute(String trigger) {
        LOG.info("[HEAVY-PIPELINE-{}] Démarrage Facturation → FNE → Tournant", trigger);
        Instant start = Instant.now();
        try {
            run("Planifications facturation", facturationSchedulerJob::executerPlanificationsEnAttente);
            run("Certification FNE", certificationFneSchedulerJob::executerCertificationsPendantes);
            run("Inventaire tournant", tournantSchedulerService::executerTournantsEchus);
        } finally {
            LOG.info("[HEAVY-PIPELINE-{}] Terminé en {}", trigger,
                Duration.between(start, Instant.now()).toSeconds() + "s");
        }
    }

    private void run(String label, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            LOG.error("[HEAVY-PIPELINE] {} — ERREUR", label, e);
        }
    }
}
