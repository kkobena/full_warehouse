package com.kobe.warehouse.batch.pipeline;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Déclenche {@code nightlyPipelineJob} (périmètre domain : SEMOIS, Classification,
 * Ventes, Stock, Avoirs, Vues matérialisées).
 *
 * <p>{@code JobOrchestrationService} de l'app principale continue à gérer
 * Facturation, FNE et Tournant jusqu'à la Phase 3 de la migration.
 */
@Service
public class NightlyPipelineScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(NightlyPipelineScheduler.class);

    private final JobOperator asyncJobOperator;
    private final Job nightlyPipelineJob;

    public NightlyPipelineScheduler(JobOperator asyncJobOperator, Job nightlyPipelineJob) {
        this.asyncJobOperator = asyncJobOperator;
        this.nightlyPipelineJob = nightlyPipelineJob;
    }

    @Scheduled(cron = "${pharma-smart.jobs.nightly-pipeline-cron:0 0 9 * * *}")
    public void runNightlyPipeline() {
        launch("NIGHTLY");
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        launch("STARTUP");
    }

    private void launch(String trigger) {
        try {
            JobParameters params = new JobParametersBuilder()
                .addString("date", LocalDate.now().toString())
                .addString("trigger", trigger)
                .toJobParameters();
            asyncJobOperator.start(nightlyPipelineJob, params);
            LOG.info("[PIPELINE-{}] Job lancé avec date={}", trigger, LocalDate.now());
        } catch (JobInstanceAlreadyCompleteException e) {
            LOG.info("[PIPELINE-{}] Job déjà terminé avec succès aujourd'hui — skip", trigger);
        } catch (Exception e) {
            LOG.error("[PIPELINE-{}] Erreur au lancement du job", trigger, e);
        }
    }
}
