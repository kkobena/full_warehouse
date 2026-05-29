package com.kobe.warehouse.batch.pipeline;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Déclenche {@code nightlyPipelineJob} via deux mécanismes :
 *
 * <ol>
 *   <li><strong>Démarrage</strong> ({@code ApplicationReadyEvent}) : rattrapage au démarrage
 *       du service. Spring Batch ignore silencieusement si le job du jour est déjà
 *       {@code COMPLETED} ; il redémarre depuis l'étape en erreur si le job est {@code FAILED}.</li>
 *   <li><strong>Cron</strong> : exécution planifiée chaque nuit.
 *       Configurer {@code pharma-smart.batch.cron-schedule} :
 *       <ul>
 *
 *         <li>{@code "0 30 0 * * *"} — pharmacie ouverte 24h/24 (00h30)</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>Interrupteur maître</strong> : ce bean n'est instancié que si
 * {@code pharma-smart.batch.active=true}. Laisser à {@code false} (défaut) pendant la
 * phase de tests pour que le service démarre sans déclencher aucun traitement.
 */
@ConditionalOnProperty(name = "pharma-smart.batch.active", havingValue = "true")
@Service
public class NightlyPipelineScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(NightlyPipelineScheduler.class);

    private final JobOperator asyncJobOperator;
    private final Job nightlyPipelineJob;

    public NightlyPipelineScheduler(JobOperator asyncJobOperator, Job nightlyPipelineJob) {
        this.asyncJobOperator = asyncJobOperator;
        this.nightlyPipelineJob = nightlyPipelineJob;
    }

    @Scheduled(cron = "${pharma-smart.batch.cron-schedule:0 30 0 * * *}")
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
