package com.kobe.warehouse.batch.semois;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * Job autonome de recalcul SEMOIS avec chunk-based processing.
 *
 * <p>Ce job N'EST PAS encore intégré dans {@code nightlyPipelineJob} (Phase 1).
 * Le pipeline nocturne utilise encore le Tasklet {@code recalculateSemoisStep}.
 * L'intégration complète (Phase 2) nécessite d'exposer
 * {@code SemoisCalculationService.calculateForProduit()} comme méthode publique.
 *
 * <p>Avantages sur le Tasklet une fois en Phase 2 :
 * <ul>
 *   <li>Restart depuis le dernier checkpoint (chunk validé) en cas d'échec</li>
 *   <li>Monitoring par step dans {@code /actuator/batch}</li>
 *   <li>Skip/retry déclaratifs par item</li>
 * </ul>
 */
@Configuration
public class SemoisCalculationJobConfig {

    @Value("${pharma-smart.batch.chunk-size:100}")
    private int chunkSize;

    @Bean
    public Job semoisCalculationJob(
        JobRepository jobRepository,
        Step recalculateSemoisChunkStep
    ) {
        return new JobBuilder("semoisCalculationJob", jobRepository)
            .preventRestart()
            .start(recalculateSemoisChunkStep)
            .build();
    }

    @Bean
    public Step recalculateSemoisChunkStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        JpaPagingItemReader<Produit> semoisProduitReader,
        SemoisItemProcessor semoisItemProcessor,
        SemoisItemWriter semoisItemWriter
    ) {
        return new StepBuilder("recalculateSemoisChunkStep", jobRepository)
            .<Produit, Produit>chunk(chunkSize).transactionManager(txManager)
            .reader(semoisProduitReader)
            .processor(semoisItemProcessor)
            .writer(semoisItemWriter)
            .faultTolerant()
                .skip(Exception.class).skipLimit(50)
            .listener(new SemoisSkipListener())
            .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Produit> semoisProduitReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<Produit>()
            .name("semoisProduitReader")
            .entityManagerFactory(emf)
            .transacted(false)
            .parameterValues(Map.of("status", Status.ENABLE,"typeProduit", TypeProduit.PACKAGE))
            .queryString("""
                SELECT p FROM Produit p
                JOIN FETCH p.fournisseurProduitPrincipal fp
                WHERE p.status = :status
                  AND p.typeProduit = :typeProduit
                ORDER BY p.id
                """)
            .pageSize(chunkSize)
            .build();
    }
}
