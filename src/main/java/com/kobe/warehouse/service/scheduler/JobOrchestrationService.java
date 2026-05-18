package com.kobe.warehouse.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrateur centralisé des jobs SEMOIS.
 *
 * <p>Remplace les {@code @Scheduled} individuels et le {@code SemoisStartupCoordinator}
 * par un <strong>pipeline séquentiel</strong> dont l'ordre est garanti :
 * <ol>
 *   <li>Snapshot stock quotidien (idempotent — ON CONFLICT DO NOTHING)</li>
 *   <li>Réintégration des exclusions expirées</li>
 *   <li>Agrégation des ventes mensuelles</li>
 *   <li>Classification criticité produits (garde mensuelle interne)</li>
 *   <li>Recalcul SEMOIS + génération suggestions (garde journalière interne)</li>
 *   <li>Inventaire tournant (plannings échus)</li>
 *   <li>Rafraîchissement de toutes les vues matérialisées</li>
 * </ol>
 *
 * <p>Chaque étape conserve ses gardes idempotentes internes (mensuelle, journalière, etc.)
 * et peut donc être ré-exécutée sans risque.
 *
 * <p>Le pipeline s'exécute :
 * <ul>
 *   <li>Au démarrage de l'application (catch-up asynchrone)</li>
 *   <li>Selon le cron configurable {@code pharma-smart.jobs.nightly-pipeline-cron}
 *       (défaut : 09h00)</li>
 * </ul>
 *
 * <p>Les jobs <em>indépendants</em> (reports) conservent leur propre {@code @Scheduled}.
 * Les vues matérialisées gardent aussi leurs crons intra-journée (15 min, 1h, 4x/jour)
 * mais un {@code refreshAll} est exécuté en fin de pipeline pour garantir des dashboards à jour.
 */
@Service
public class JobOrchestrationService {

    private static final Logger LOG = LoggerFactory.getLogger(JobOrchestrationService.class);

    private final AtomicBoolean pipelineRunning = new AtomicBoolean(false);

    private final StockSnapshotSchedulerService stockSnapshotSchedulerService;
    private final SemoisBatchJobService semoisBatchJobService;
    private final VentesAgregeesService ventesAgregeesService;
    private final ClassificationCriticiteService classificationService;
    private final SemoisCalculationService semoisCalculationService;
    private final TournantSchedulerService tournantSchedulerService;
    private final MaterializedViewRefreshService materializedViewRefreshService;
    private final FacturationSchedulerJob facturationSchedulerJob;
    private final CertificationFneSchedulerJob certificationFneSchedulerJob;
    private final AvoirExpirationJob avoirExpirationJob;

    public JobOrchestrationService(
        StockSnapshotSchedulerService stockSnapshotSchedulerService,
        SemoisBatchJobService semoisBatchJobService,
        VentesAgregeesService ventesAgregeesService,
        ClassificationCriticiteService classificationService,
        SemoisCalculationService semoisCalculationService,
        TournantSchedulerService tournantSchedulerService,
        MaterializedViewRefreshService materializedViewRefreshService,
        FacturationSchedulerJob facturationSchedulerJob,
        CertificationFneSchedulerJob certificationFneSchedulerJob,
        AvoirExpirationJob avoirExpirationJob
    ) {
        this.stockSnapshotSchedulerService = stockSnapshotSchedulerService;
        this.semoisBatchJobService = semoisBatchJobService;
        this.ventesAgregeesService = ventesAgregeesService;
        this.classificationService = classificationService;
        this.semoisCalculationService = semoisCalculationService;
        this.tournantSchedulerService = tournantSchedulerService;
        this.materializedViewRefreshService = materializedViewRefreshService;
        this.facturationSchedulerJob = facturationSchedulerJob;
        this.certificationFneSchedulerJob = certificationFneSchedulerJob;
        this.avoirExpirationJob = avoirExpirationJob;
    }

    // ── Types ────────────────────────────────────────────────────────────────────

    public enum JobStep {
        STOCK_SNAPSHOT("Snapshot stock quotidien"),
        REINTEGRATE_EXCLUSIONS("Réintégration exclusions expirées"),
        AGGREGATE_SALES("Agrégation ventes mensuelles"),
        CLASSIFY_PRODUCTS("Classification criticité produits"),
        RECALCULATE_SEMOIS("Recalcul SEMOIS + suggestions"),
        INVENTAIRE_TOURNANT("Inventaire tournant échu"),
        REFRESH_VIEWS("Rafraîchissement vues matérialisées"),
        FACTURATION_PLANIFICATIONS("Planifications de facturation périodique"),
        CERTIFICATION_FNE("Certification FNE des factures générées"),
        EXPIRATION_AVOIRS("Expiration avoirs clients échus");

        private final String label;

        JobStep(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record StepResult(JobStep step, boolean success, String message, Duration duration) {}


    @Scheduled(cron = "${pharma-smart.jobs.nightly-pipeline-cron:0 0 9 * * *}")
    public void runNightlyPipeline() {
        executePipeline("NIGHTLY");
    }


    /**
     * Catch-up au démarrage de l'application.
     * Exécuté en async pour ne pas bloquer la disponibilité des endpoints HTTP.
     * Les gardes idempotentes de chaque étape empêchent les doubles exécutions.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        executePipeline("STARTUP");
    }

    // ── Pipeline ─────────────────────────────────────────────────────────────────

    private static final List<JobStep> ALL_STEPS = List.of(
        JobStep.STOCK_SNAPSHOT,
        JobStep.REINTEGRATE_EXCLUSIONS,
        JobStep.AGGREGATE_SALES,
        JobStep.REFRESH_VIEWS,           // vues matérialisées fraîches avant le recalcul SEMOIS
        JobStep.CLASSIFY_PRODUCTS,
        JobStep.RECALCULATE_SEMOIS,
        JobStep.INVENTAIRE_TOURNANT,
        JobStep.FACTURATION_PLANIFICATIONS,
        JobStep.CERTIFICATION_FNE,
        JobStep.EXPIRATION_AVOIRS
    );

    /**
     * Exécute le pipeline complet de façon séquentielle.
     *
     * @param trigger identifiant du déclencheur (pour les logs)
     */
    public void executePipeline(String trigger) {
        if (!pipelineRunning.compareAndSet(false, true)) {
            LOG.warn("[PIPELINE-{}] Pipeline déjà en cours — skip", trigger);
            return;
        }

        LOG.info("[PIPELINE-{}] Démarrage — {} étape(s)", trigger, ALL_STEPS.size());
        Instant pipelineStart = Instant.now();
        List<StepResult> results = new ArrayList<>();

        try {
            for (JobStep step : ALL_STEPS) {
                StepResult result = executeStep(step);
                results.add(result);
                if (!result.success()) {
                    LOG.error("[PIPELINE-{}] Étape «{}» échouée — pipeline arrêté",
                        trigger, step.getLabel());
                    break;
                }
            }
        } finally {
            pipelineRunning.set(false);
        }

        Duration totalDuration = Duration.between(pipelineStart, Instant.now());
        long successCount = results.stream().filter(StepResult::success).count();
        LOG.info("[PIPELINE-{}] Terminé en {} — {}/{} étapes réussies",
            trigger, formatDuration(totalDuration), successCount, results.size());

    }

    // ── Exécution d'une étape ────────────────────────────────────────────────────

    private StepResult executeStep(JobStep step) {
        LOG.info("[PIPELINE] ▸ Étape: {}", step.getLabel());
        Instant start = Instant.now();
        try {
            switch (step) {
                case STOCK_SNAPSHOT -> stockSnapshotSchedulerService.createDailySnapshot();
                case REINTEGRATE_EXCLUSIONS -> semoisBatchJobService.reintegrerExclusionsExpirees();
                case AGGREGATE_SALES -> ventesAgregeesService.aggregateMonthlySalesDaily();
                case CLASSIFY_PRODUCTS -> classificationService.reclassifierTousProduits();
                case RECALCULATE_SEMOIS -> semoisCalculationService.recalculateAllConfigurations();
                case INVENTAIRE_TOURNANT -> tournantSchedulerService.executerTournantsEchus();
                case FACTURATION_PLANIFICATIONS -> facturationSchedulerJob.executerPlanificationsEnAttente();
                case CERTIFICATION_FNE -> certificationFneSchedulerJob.executerCertificationsPendantes();
                case EXPIRATION_AVOIRS -> avoirExpirationJob.expireAvoirsEchus();
                case REFRESH_VIEWS -> materializedViewRefreshService.refreshAllViews();
            }
            Duration duration = Duration.between(start, Instant.now());
            LOG.info("[PIPELINE]   {} — OK en {}", step.getLabel(), formatDuration(duration));
            return new StepResult(step, true, "OK", duration);
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            LOG.error("[PIPELINE]   {} — ERREUR en {}", step.getLabel(), formatDuration(duration), e);
            return new StepResult(step, false, e.getMessage(), duration);
        }
    }

    // ── Utilitaire ───────────────────────────────────────────────────────────────

    private static String formatDuration(Duration d) {
        if (d.toMinutes() > 0) {
            return d.toMinutes() + "m" + d.toSecondsPart() + "s";
        }
        return d.toSeconds() + "." + String.format("%03d", d.toMillisPart()) + "s";
    }
}
