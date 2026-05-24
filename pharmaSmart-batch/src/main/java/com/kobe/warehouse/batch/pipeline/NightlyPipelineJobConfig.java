package com.kobe.warehouse.batch.pipeline;

import com.kobe.warehouse.service.scheduler.AvoirExpirationJob;
import com.kobe.warehouse.service.scheduler.MaterializedViewRefreshService;
import com.kobe.warehouse.service.scheduler.SemoisBatchJobService;
import com.kobe.warehouse.service.scheduler.StockSnapshotSchedulerService;
import com.kobe.warehouse.service.scheduler.VentesAgregeesService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Pipeline nocturne Spring Batch (Phase 3).
 *
 * <p>Périmètre : services disponibles dans {@code pharmaSmart-domain} uniquement.
 * Les étapes Facturation, FNE et Tournant restent dans {@code HeavyJobsOrchestrationService}
 * (services avec dépendances HTTP externe, PDF, EditionService registry — Phase 4).
 *
 * <p>Pipeline :
 * <ol>
 *   <li>stockSnapshotStep — snapshot stock quotidien (idempotent)</li>
 *   <li>reintegrateExclusionsStep — exclusions SEMOIS expirées</li>
 *   <li>aggregateSalesStep — agrégation ventes mensuelles</li>
 *   <li>refreshViewsStep — vues matérialisées fraîches avant recalcul</li>
 *   <li>classifyChunkStep — classification ABC-Pareto (chunk-oriented, depuis ClassificationJobConfig)</li>
 *   <li>recalculateSemoisChunkStep — VMM + stock-objectif (chunk-oriented, depuis SemoisCalculationJobConfig)</li>
 *   <li>avoirExpirationStep — expiration avoirs échus</li>
 * </ol>
 */
@Configuration
public class NightlyPipelineJobConfig {

    @Bean
    public Job nightlyPipelineJob(
        JobRepository jobRepository,
        Step stockSnapshotStep,
        Step reintegrateExclusionsStep,
        Step aggregateSalesStep,
        Step refreshViewsStep,
        Step classifyChunkStep,
        Step recalculateSemoisChunkStep,
        Step avoirExpirationStep
    ) {
        return new JobBuilder("nightlyPipelineJob", jobRepository)
            .start(stockSnapshotStep)
            .next(reintegrateExclusionsStep)
            .next(aggregateSalesStep)
            .next(refreshViewsStep)
            .next(classifyChunkStep)
            .next(recalculateSemoisChunkStep)
            .next(avoirExpirationStep)
            .build();
    }

    @Bean
    public Step stockSnapshotStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        StockSnapshotSchedulerService service
    ) {
        return new StepBuilder("stockSnapshotStep", jobRepository)
            .tasklet((c, ctx) -> { service.createDailySnapshot(); return RepeatStatus.FINISHED; }, txManager)
            .build();
    }

    @Bean
    public Step reintegrateExclusionsStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        SemoisBatchJobService service
    ) {
        return new StepBuilder("reintegrateExclusionsStep", jobRepository)
            .tasklet((c, ctx) -> { service.reintegrerExclusionsExpirees(); return RepeatStatus.FINISHED; }, txManager)
            .build();
    }

    @Bean
    public Step aggregateSalesStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        VentesAgregeesService service
    ) {
        return new StepBuilder("aggregateSalesStep", jobRepository)
            .tasklet((c, ctx) -> { service.aggregateMonthlySalesDaily(); return RepeatStatus.FINISHED; }, txManager)
            .build();
    }

    @Bean
    public Step refreshViewsStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        MaterializedViewRefreshService service
    ) {
        return new StepBuilder("refreshViewsStep", jobRepository)
            .tasklet((c, ctx) -> { service.refreshAllViews(); return RepeatStatus.FINISHED; }, txManager)
            .build();
    }

    @Bean
    public Step avoirExpirationStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        AvoirExpirationJob service
    ) {
        return new StepBuilder("avoirExpirationStep", jobRepository)
            .tasklet((c, ctx) -> { service.expireAvoirsEchus(); return RepeatStatus.FINISHED; }, txManager)
            .build();
    }
}
