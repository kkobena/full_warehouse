package com.kobe.warehouse.batch.inventaire;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job autonome d'import inventaire depuis un fichier CSV.
 * Déclenché manuellement via endpoint REST ou dépôt de fichier.
 *
 * <p>Paramètres obligatoires dans les JobParameters :
 * <ul>
 *   <li>{@code filePath} (String) — chemin absolu vers le fichier CSV</li>
 *   <li>{@code storeInventoryId} (Long) — ID du StoreInventory cible</li>
 * </ul>
 *
 * <pre>{@code
 * POST /api/admin/batch/jobs/inventaireImportJob/launch
 * {"filePath": "/chemin/vers/inventaire.csv", "storeInventoryId": 42}
 * }</pre>
 *
 * <p>Format CSV attendu : {@code cip13;lot;quantite;peremption} (entête obligatoire, séparateur ";").
 */
@Configuration
public class InventaireImportJobConfig {

    @Value("${pharma-smart.batch.chunk-size:100}")
    private int chunkSize;

    @Bean
    public Job inventaireImportJob(
        JobRepository jobRepository,
        Step parseAndPersistStep
    ) {
        return new JobBuilder("inventaireImportJob", jobRepository)
            .start(parseAndPersistStep)
            .build();
    }

    @Bean
    public Step parseAndPersistStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        FlatFileItemReader<InventaireLigneRaw> inventaireCsvReader,
        InventaireLineProcessor inventaireLineProcessor,
        InventaireLineWriter inventaireLineWriter
    ) {
        return new StepBuilder("parseAndPersistStep", jobRepository)
            .<InventaireLigneRaw, InventaireLigneValidee>chunk(chunkSize).transactionManager(txManager)
            .reader(inventaireCsvReader)
            .processor(inventaireLineProcessor)
            .writer(inventaireLineWriter)
            .faultTolerant()
                .skip(Exception.class).skipLimit(100)
            .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<InventaireLigneRaw> inventaireCsvReader(
        @Value("#{jobParameters['filePath']}") String filePath
    ) {
        return new FlatFileItemReaderBuilder<InventaireLigneRaw>()
            .name("inventaireCsvReader")
            .resource(new FileSystemResource(filePath))
            .delimited().delimiter(";")
            .names("cip13", "lot", "quantite", "peremption")
            .targetType(InventaireLigneRaw.class)
            .linesToSkip(1)
            .build();
    }
}
