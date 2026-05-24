package com.kobe.warehouse.batch.classification;

import com.kobe.warehouse.domain.Produit;
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

/**
 * Job autonome de classification criticité ABC-Pareto (Phase 2).
 *
 * <p>Processor : charge config + Pareto en {@code @BeforeStep}, calcule la classe via
 * {@code ClassificationBatchProcessor}, retourne {@code null} pour les items filtrés.
 * Writer : {@code saveAll} des produits modifiés + {@code saveAll} des logs en bulk.
 */
@Configuration
public class ClassificationJobConfig {

    @Value("${pharma-smart.batch.chunk-size:100}")
    private int chunkSize;

    @Bean
    public Job classificationCriticiteJob(
        JobRepository jobRepository,
        Step classifyChunkStep,
        ClassificationStepListener classificationStepListener
    ) {
        return new JobBuilder("classificationCriticiteJob", jobRepository)
            .preventRestart()
            .start(classifyChunkStep)
            .build();
    }

    @Bean
    public Step classifyChunkStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        JpaPagingItemReader<Produit> classificationProduitReader,
        ClassificationItemProcessor classificationItemProcessor,
        ClassificationItemWriter classificationItemWriter,
        ClassificationStepListener classificationStepListener
    ) {
        return new StepBuilder("classifyChunkStep", jobRepository)
            .<Produit, ClassificationResult>chunk(chunkSize).transactionManager(txManager)
            .reader(classificationProduitReader)
            .processor(classificationItemProcessor)
            .writer(classificationItemWriter)
            .faultTolerant()
                .skip(Exception.class).skipLimit(20)
            .listener(classificationStepListener)
            .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Produit> classificationProduitReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<Produit>()
            .name("classificationProduitReader")
            .entityManagerFactory(emf)
            .queryString("SELECT p FROM Produit p WHERE p.actif = true ORDER BY p.id")
            .pageSize(chunkSize)
            .build();
    }
}
