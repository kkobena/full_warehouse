# Plan d'intégration Spring Batch — PharmaSmart

## Table des matières

1. [Synthèse](#1-synthèse)
2. [Structure Maven multi-module](#2-structure-maven-multi-module)
3. [État actuel](#3-état-actuel)
4. [Architecture des jobs](#4-architecture-des-jobs)
5. [Dépendances & configuration](#5-dépendances--configuration)
6. [Jobs détaillés](#6-jobs-détaillés)
   - 6.1 SemoisCalculationJob
   - 6.2 ClassificationCriticiteJob
   - 6.3 SuggestionsReapproJob
   - 6.4 VentesAgregeesJob
   - 6.5 InventaireImportJob
   - 6.6 FacturationPeriodiqueueJob
   - 6.7 NightlyPipelineJob (orchestrateur)
7. [Migration progressive](#7-migration-progressive)
8. [Monitoring & opérations](#8-monitoring--opérations)
9. [Risques & contre-indications](#9-risques--contre-indications)

---

## 1. Synthèse

PharmaSmart dispose d'un pipeline nocturne sophistiqué (10 étapes, ~3 services >500 lignes)
entièrement réimplémenté à la main avec des patterns `@Scheduled + paginations + AtomicBoolean`.
L'intégration de **Spring Batch** apporterait :

| Besoin actuel (custom) | Apport Spring Batch |
|---|---|
| `AtomicBoolean pipelineRunning` | Verrou `JobRepository` persistant, multi-instance natif |
| Gardes idempotentes `APP_LAST_DAY_*` | `JobParameters` + `JobInstanceAlreadyCompleteException` |
| Pagination manuelle avec `self.processBatch()` | `JpaPagingItemReader<T>` générique |
| `LOG.info` custom pour le monitoring | `JobExecution`, `StepExecution`, Actuator `/batch` |
| Restart manuel en cas d'échec | Restart automatique depuis le dernier `chunk` validé |
| Skip silencieux dans les boucles | `skip()` / `retry()` déclaratifs avec `SkipListener` |
| Chaîne séquentielle fragile | `FlowBuilder` avec transitions conditionnelles |

**Principe directeur :** Réutiliser **à 100%** les entités JPA, repositories et services métier
existants. Spring Batch encapsule le *transport* (lecture/découpage/écriture), pas le *métier*.

---

## 2. Structure Maven multi-module

### 2.1 Problème : dépendance sur l'app entière

Si le module batch dépendait du jar complet de l'application, Spring tenterait d'auto-configurer
tout ce qu'il trouve : sécurité JWT, serveur web, génération PDF, Firebase, imprimante série, etc.
Même avec des exclusions manuelles, c'est fragile et couplé à l'ensemble de l'application.

### 2.2 Solution : 3 modules distincts

```
full_warehouse/                    ← app principale (RIEN ne bouge)
│   pom.xml                        ← devient parent/agrégateur (packaging=pom)
│   src/                           ← CODE EXTRAIT vers pharmaSmart-domain (voir §7)
│   angular.json, package.json     ← inchangés (build Angular)
│   src-tauri/                     ← inchangé (Tauri)
│   scripts/prepare-sidecar.js     ← inchangé (cherche target/*.jar)
│
├── pharmaSmart-domain/            ← NOUVEAU — jar partagé (entités + repos + services)
│   ├── src/main/java/com/kobe/warehouse/
│   │   ├── domain/                ← entités JPA
│   │   ├── repository/            ← Spring Data repos
│   │   ├── service/dto/           ← DTOs
│   │   ├── service/scheduler/     ← services métier batch
│   │   └── config/               ← DatabaseConfiguration, CacheConfiguration
│   └── pom.xml
│
└── pharmaSmart-batch/             ← NOUVEAU — livrable Spring Boot (sans web)
    ├── src/main/java/com/kobe/warehouse/batch/
    │   ├── BatchApplication.java
    │   ├── config/
    │   ├── pipeline/
    │   ├── semois/
    │   ├── classification/
    │   └── inventaire/
    └── pom.xml
```

### 2.3 Dépendances entre modules

```
spring-boot-starter-parent
        │
        ├── pharmaSmart-domain    (JPA + Hibernate + cache — PAS de web)
        │         ↑
        ├── pharmaSmart-app       (dépend de domain + web + sécurité + PDF...)
        │         ↑
        │   [fat jar → target/pharmaSmart-*.jar → Tauri sidecar]
        │
        └── pharmaSmart-batch     (dépend de domain + spring-batch — PAS de web)
                  ↑
            [fat jar → pharmaSmart-batch/target/*.jar → livrable déployé séparément]
```

**Le batch ne voit que `pharmaSmart-domain`** : zéro dépendance sur PDF, sécurité JWT,
imprimante, Firebase ou les controllers REST.

### 2.4 Impact sur Tauri

| Point d'ancrage Tauri | Valeur | Impact |
|---|---|---|
| `tauri.conf.json` → `frontendDist` | `../target/classes/static` | **Aucun** |
| `prepare-sidecar.js` → JAR path | `target/pharmaSmart-*.jar` (racine) | **Aucun** |
| `npm run webapp:build:tauri` | build Angular → `target/classes/static` | **Aucun** |
| `mvnw.cmd clean package -Pprod` | build app principale → `target/` | **Aucun** |

Le fat jar de l'app principale reste produit dans `target/` à la racine. Tauri est entièrement
préservé.

### 2.5 Commandes de build

```bash
# Build complet depuis la racine (ordre géré par Maven reactor)
./mvnw.cmd clean install -Pprod          # Windows Git Bash
mvnw.cmd clean install -Pprod           # Windows CMD

# Build du seul module batch (après install du domain)
cd pharmaSmart-batch
mvnw.cmd clean package -DskipTests

# Build Tauri (inchangé)
npm run tauri:build:bundled-jre
```

---

## 3. État actuel

### 3.1 Pipeline nocturne (`JobOrchestrationService`)

```
STOCK_SNAPSHOT → REINTEGRATE_EXCLUSIONS → AGGREGATE_SALES → REFRESH_VIEWS
    → CLASSIFY_PRODUCTS → RECALCULATE_SEMOIS → INVENTAIRE_TOURNANT
    → FACTURATION_PLANIFICATIONS → CERTIFICATION_FNE → EXPIRATION_AVOIRS
```

Déclencheurs :
- `@Scheduled(cron = "${pharma-smart.jobs.nightly-pipeline-cron:0 0 9 * * *}")` — cron configurable
- `@EventListener(ApplicationReadyEvent.class) @Async` — catch-up au démarrage

### 3.2 Services candidats identifiés

| Service | Lignes | Volume estimé | Complexité | Priorité |
|---|---|---|---|---|
| `SemoisCalculationService` | 1 205 | Tous produits actifs (~100/batch) | Très haute | **P0** |
| `ClassificationCriticiteService` | 542 | Tous produits actifs (~100/batch) | Haute | **P0** |
| `SemoisBatchJobService` | 374 | Produits éligibles + FP | Haute | **P1** |
| `VentesAgregeesService` | 212 | Ventes du mois en cours | Moyenne | **P1** |
| `InventaireImportService` | — | Lignes CSV (illimité) | Moyenne | **P2** |
| `FacturationSchedulerJob` | — | Planifications en attente | Moyenne | **P2** |
| `AvoirExpirationJob` | — | Avoirs échus | Faible | **P3** |

### 3.3 Patterns déjà présents (réutilisables)

- **Pagination par pages de 100** → `JpaPagingItemReader` avec `pageSize(100)`
- **`self.processBatch()` avec `REQUIRES_NEW`** → `ItemProcessor` + chunk-boundary Batch
- **Chunking `IN()` > 1000** → même logique dans `ItemReader` avec critères d'entité
- **Batch-load Map en mémoire** → `StepExecutionContext` partagé entre steps du même job
- **`saveAll()`** → `JpaItemWriter` ou `RepositoryItemWriter`

---

## 4. Architecture des jobs

### 4.1 Vue d'ensemble

```
BatchConfiguration
│
├── NightlyPipelineJob          ← Remplace JobOrchestrationService
│   ├── stockSnapshotStep       ← Tasklet (SQL natif idempotent)
│   ├── reintegrateExclusionsStep ← Tasklet
│   ├── aggregateSalesStep      ← Chunk (VentesAgregeesJob)
│   ├── refreshViewsStep        ← Tasklet
│   ├── classifyProductsStep    ← Chunk (ClassificationCriticiteJob)
│   ├── recalculateSemoisStep   ← Chunk (SemoisCalculationJob)
│   ├── createSuggestionsStep   ← Chunk (SuggestionsReapproJob)
│   ├── inventaireTournantStep  ← Tasklet
│   ├── facturationStep         ← Tasklet
│   ├── certificationFneStep    ← Tasklet
│   └── avoirExpirationStep     ← Tasklet
│
├── InventaireImportJob         ← Job autonome (déclenchement manuel/API)
│   ├── parseStep               ← FlatFileItemReader → ItemProcessor
│   └── persistStep             ← JpaItemWriter
│
└── ScheduledReportJob          ← Job autonome (cron 0 0 * * * *)
    └── generateReportsStep     ← Tasklet
```

### 4.2 Principe de réutilisation

```
┌──────────────────────────────────────────────────────┐
│         pharmaSmart-batch (Spring Batch Layer)       │
│  ItemReader → ItemProcessor → ItemWriter (chunk)     │
│               ↑                    ↑                 │
│       Délègue à             Délègue à                │
└───────────────┼─────────────────── ┼ ────────────────┘
                │                    │
┌───────────────▼────────────────────▼────────────────┐
│          pharmaSmart-domain (Couche Métier)          │
│  Repository   Service   Entity   DTO                │
│  (INCHANGÉS)  (INCHANGÉS)                           │
└─────────────────────────────────────────────────────┘
```

**Aucune modification des entités JPA, repositories ou logique métier.**

---

## 5. Dépendances & configuration

### 5.1 `pom.xml` racine (devient parent + agrégateur)

```xml
<project>
  <groupId>com.kobe.warehouse</groupId>
  <artifactId>pharmaSmart-parent</artifactId>
  <version>0.2.3</version>
  <packaging>pom</packaging>                   <!-- CHANGEMENT : jar → pom -->

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
  </parent>

  <!-- Déclaration des modules — Maven construit dans l'ordre des dépendances -->
  <modules>
    <module>pharmaSmart-domain</module>         <!-- 1er : pas de dépendance interne -->
    <module>pharmaSmart-app</module>            <!-- 2e : dépend de domain -->
    <module>pharmaSmart-batch</module>          <!-- 3e : dépend de domain -->
  </modules>

  <!-- Versions partagées dans dependencyManagement (héritage sans import automatique) -->
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.kobe.warehouse</groupId>
        <artifactId>pharmaSmart-domain</artifactId>
        <version>${project.version}</version>
      </dependency>
      <!-- ... autres versions gérées centralement -->
    </dependencies>
  </dependencyManagement>

  <!-- Propriétés communes (java.version, hibernate.version, etc.) -->
  <properties>
    <java.version>25</java.version>
    <hibernate.version>7.2.1.Final</hibernate.version>
    <!-- ... -->
  </properties>
</project>
```

> **Note :** Le code source existant dans `src/` reste dans `pharmaSmart-app/` qui conserve
> toute la configuration de build actuelle (Angular, Flyway, spring-boot-maven-plugin, profiles).

### 5.2 `pharmaSmart-domain/pom.xml`

```xml
<project>
  <parent>
    <groupId>com.kobe.warehouse</groupId>
    <artifactId>pharmaSmart-parent</artifactId>
    <version>0.2.3</version>
    <relativePath>../pom.xml</relativePath>
  </parent>

  <artifactId>pharmaSmart-domain</artifactId>
  <packaging>jar</packaging>                   <!-- jar simple, PAS de spring-boot repackage -->

  <dependencies>
    <!-- JPA / Hibernate — socle entités et repos -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Cache Caffeine — CacheConfiguration utilisée par les services -->
    <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>caffeine</artifactId>
    </dependency>
    <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>jcache</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
      <groupId>org.hibernate.validator</groupId>
      <artifactId>hibernate-validator</artifactId>
      <version>${hibernate-validator.version}</version>
    </dependency>

    <!-- PostgreSQL driver (runtime) -->
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>

    <!-- Flyway — migrations partagées -->
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- Jackson pour les DTOs -->
    <dependency>
      <groupId>com.fasterxml.jackson.datatype</groupId>
      <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Utilitaires métier -->
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-lang3</artifactId>
    </dependency>
  </dependencies>

  <!-- PAS de spring-boot-maven-plugin → jar standard installable comme dépendance -->
</project>
```

### 5.3 `pharmaSmart-batch/pom.xml`

```xml
<project>
  <parent>
    <groupId>com.kobe.warehouse</groupId>
    <artifactId>pharmaSmart-parent</artifactId>
    <version>0.2.3</version>
    <relativePath>../pom.xml</relativePath>
  </parent>

  <artifactId>pharmaSmart-batch</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <!-- Domain partagé — entités, repos, services métier -->
    <dependency>
      <groupId>com.kobe.warehouse</groupId>
      <artifactId>pharmaSmart-domain</artifactId>
    </dependency>

    <!-- Spring Batch — la seule dépendance spécifique -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-batch</artifactId>
    </dependency>

    <!-- Actuator pour monitoring des jobs -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Tests -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.batch</groupId>
      <artifactId>spring-batch-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <mainClass>com.kobe.warehouse.batch.BatchApplication</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### 5.4 `BatchApplication.java`

```java
package com.kobe.warehouse.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {
        "com.kobe.warehouse.domain",
        "com.kobe.warehouse.repository",
        "com.kobe.warehouse.service.scheduler",
        "com.kobe.warehouse.config",
        "com.kobe.warehouse.batch"
    }
)
@EnableScheduling
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
```

### 5.5 `pharmaSmart-batch/src/main/resources/application.yml`

```yaml
spring:
  main:
    web-application-type: none             # pas de serveur HTTP

  datasource:
    url: ${PHARMA_DB_URL:jdbc:postgresql://localhost:5432/warehouse}
    username: ${PHARMA_DB_USER:warehouse}
    password: ${PHARMA_DB_PASSWORD:}
    hikari:
      maximum-pool-size: 5                 # batch n'a pas besoin d'autant de connexions

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
          batch_size: 50

  batch:
    jdbc:
      initialize-schema: always
      table-prefix: BATCH_
    job:
      enabled: false                       # pas de lancement automatique au démarrage

pharma-smart:
  batch:
    chunk-size: 100
    jobs:
      nightly-pipeline-cron: "0 0 9 * * *"

management:
  endpoints:
    web:
      exposure:
        include: health, info, batch
```

### 5.6 `BatchConfiguration.java`

```java
package com.kobe.warehouse.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-"));
        launcher.afterPropertiesSet();
        return launcher;
    }
}
```

### 5.7 Migration Flyway pour les méta-tables Spring Batch

Les méta-tables Spring Batch (`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, etc.) sont créées
automatiquement avec `initialize-schema: always`. Si on préfère les gérer via Flyway :

```sql
-- pharmaSmart-app/src/main/resources/db/migration/V1.9.0__spring_batch_schema.sql
-- Copier le contenu de :
-- org/springframework/batch/core/schema-postgresql.sql (dans spring-batch-core.jar)
-- Ou laisser spring.batch.jdbc.initialize-schema: always gérer cela automatiquement.
```

---

## 6. Jobs détaillés

### 6.1 SemoisCalculationJob (P0 — 1 205 lignes à migrer)

#### Contexte

`SemoisCalculationService.recalculateAllConfigurations()` parcourt TOUS les produits actifs par pages
de 100, recalcule VMM/stock-objectif/saisonnalité, puis appelle `SemoisBatchJobService` en chaîne.
Le proxy `@Lazy self` est utilisé pour forcer `REQUIRES_NEW` par page — Spring Batch rend cela inutile.

#### Structure du Job

```
SemoisCalculationJob
├── loadParetoCacheStep          Tasklet — charge Map<Integer,ParetoScore> dans JobExecutionContext
├── recalculateSemoisStep        Chunk<Produit, SemoisUpdateResult>
│   ├── Reader:  JpaPagingItemReader<Produit>
│   ├── Processor: SemoisItemProcessor  (délègue à SemoisCalculationService)
│   └── Writer:  SemoisItemWriter       (délègue à repositories existants)
└── postRecalculateStep          Tasklet — invalide caches Caffeine, log final
```

#### Code

```java
// pharmaSmart-batch/src/main/java/com/kobe/warehouse/batch/semois/SemoisCalculationJobConfig.java
package com.kobe.warehouse.batch.semois;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.scheduler.SemoisCalculationService;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SemoisCalculationJobConfig {

    @Value("${pharma-smart.batch.chunk-size:100}")
    private int chunkSize;

    @Bean
    public Job semoisCalculationJob(
        JobRepository jobRepository,
        Step loadParetoCacheStep,
        Step recalculateSemoisStep,
        Step postRecalculateStep
    ) {
        return new JobBuilder("semoisCalculationJob", jobRepository)
            .preventRestart()
            .start(loadParetoCacheStep)
            .next(recalculateSemoisStep)
            .next(postRecalculateStep)
            .build();
    }

    @Bean
    public Step loadParetoCacheStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        SemoisCalculationService semoisCalculationService
    ) {
        return new StepBuilder("loadParetoCacheStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                var paretoMap = semoisCalculationService.loadParetoMap();
                chunkContext.getStepContext()
                    .getStepExecution().getJobExecution()
                    .getExecutionContext().put("paretoMap", paretoMap);
                return RepeatStatus.FINISHED;
            }, txManager)
            .build();
    }

    @Bean
    public Step recalculateSemoisStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        JpaPagingItemReader<Produit> semoisProduitReader,
        SemoisItemProcessor semoisItemProcessor,
        SemoisItemWriter semoisItemWriter
    ) {
        return new StepBuilder("recalculateSemoisStep", jobRepository)
            .<Produit, SemoisUpdateResult>chunk(chunkSize, txManager)
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
            .queryString("""
                SELECT p FROM Produit p
                JOIN FETCH p.fournisseurPrincipal fp
                WHERE p.actif = true
                  AND p.deconditionne = false
                ORDER BY p.id
                """)
            .pageSize(chunkSize)
            .build();
    }
}
```

```java
// SemoisItemProcessor.java — délègue au service métier existant (dans pharmaSmart-domain)
@Component
public class SemoisItemProcessor implements ItemProcessor<Produit, SemoisUpdateResult> {

    private final SemoisCalculationService semoisCalculationService;

    public SemoisItemProcessor(SemoisCalculationService semoisCalculationService) {
        this.semoisCalculationService = semoisCalculationService;
    }

    @Override
    public SemoisUpdateResult process(Produit produit) {
        return semoisCalculationService.calculateForProduit(produit);
    }
}
```

```java
// SemoisItemWriter.java — réutilise les repositories du domain
@Component
public class SemoisItemWriter implements ItemWriter<SemoisUpdateResult> {

    private final SemoisConfigurationRepository semoisRepo;
    private final ProduitRepository produitRepo;

    public SemoisItemWriter(SemoisConfigurationRepository semoisRepo,
                            ProduitRepository produitRepo) {
        this.semoisRepo = semoisRepo;
        this.produitRepo = produitRepo;
    }

    @Override
    public void write(Chunk<? extends SemoisUpdateResult> chunk) {
        var configs  = chunk.getItems().stream().map(SemoisUpdateResult::config).toList();
        var produits = chunk.getItems().stream().map(SemoisUpdateResult::produit).toList();
        semoisRepo.saveAll(configs);
        produitRepo.saveAll(produits);
    }
}
```

---

### 6.2 ClassificationCriticiteJob (P0 — 542 lignes)

`ClassificationCriticiteService` utilise déjà un `ClassificationBatchProcessor` interne avec
`REQUIRES_NEW` par page. La migration est quasi-mécanique.

```java
@Configuration
public class ClassificationJobConfig {

    @Value("${pharma-smart.batch.chunk-size:100}")
    private int chunkSize;

    @Bean
    public Job classificationCriticiteJob(
        JobRepository jobRepository,
        Step classifyStep,
        Step logClassificationStep
    ) {
        return new JobBuilder("classificationCriticiteJob", jobRepository)
            .start(classifyStep)
            .next(logClassificationStep)
            .build();
    }

    @Bean
    public Step classifyStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        JpaPagingItemReader<Produit> classificationProduitReader,
        ClassificationItemProcessor classificationProcessor,
        ClassificationItemWriter classificationWriter
    ) {
        return new StepBuilder("classifyStep", jobRepository)
            .<Produit, ClassificationResult>chunk(chunkSize, txManager)
            .reader(classificationProduitReader)
            .processor(classificationProcessor)
            .writer(classificationWriter)
            .faultTolerant()
                .skip(Exception.class).skipLimit(20)
            .listener(new ClassificationStepListener())
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
```

---

### 6.3 SuggestionsReapproJob (P1)

Exécuté en step enchaîné après `recalculateSemoisStep` dans le pipeline nocturne.

```java
@Bean
public Step createSuggestionsStep(
    JobRepository jobRepository,
    PlatformTransactionManager txManager,
    SuggestionsItemReader reader,
    SuggestionsItemProcessor processor,
    SuggestionsItemWriter writer
) {
    return new StepBuilder("createSuggestionsStep", jobRepository)
        .<ProduitEligible, SuggestionResult>chunk(chunkSize, txManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
            .skip(SuggestionProtegeeException.class).skipLimit(Integer.MAX_VALUE)
        .build();
}
```

---

### 6.4 VentesAgregeesJob (P1)

`VentesAgregeesService` utilise une stratégie SQL upsert (`INSERT ... ON CONFLICT`).
Le traitement est déjà bulk — une Tasklet suffit.

```java
@Bean
public Step aggregateSalesStep(
    JobRepository jobRepository,
    PlatformTransactionManager txManager,
    VentesAgregeesService ventesAgregeesService
) {
    return new StepBuilder("aggregateSalesStep", jobRepository)
        .tasklet((contribution, ctx) -> {
            ventesAgregeesService.aggregateMonthlySalesDaily();
            return RepeatStatus.FINISHED;
        }, txManager)
        .build();
}
```

---

### 6.5 InventaireImportJob (P2)

Job autonome déclenché **manuellement** via endpoint REST ou import CSV.

```
InventaireImportJob
├── parseAndValidateStep    FlatFileItemReader → ValidateurLigneProcessor → ValidLines
└── persistInventaireStep   ValidLines → InventaireLineWriter (réutilise InventaireImportService)
```

```java
@Configuration
public class InventaireImportJobConfig {

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
```

---

### 6.6 FacturationPeriodiqueueJob (P2)

```java
@Bean
public Step facturationStep(
    JobRepository jobRepository,
    PlatformTransactionManager txManager,
    FacturationSchedulerJob facturationSchedulerJob
) {
    return new StepBuilder("facturationStep", jobRepository)
        .tasklet((contribution, ctx) -> {
            facturationSchedulerJob.executerPlanificationsEnAttente();
            return RepeatStatus.FINISHED;
        }, txManager)
        .build();
}
```

---

### 6.7 NightlyPipelineJob — orchestrateur (remplace `JobOrchestrationService`)

```java
// pharmaSmart-batch/src/main/java/com/kobe/warehouse/batch/pipeline/NightlyPipelineJobConfig.java
@Configuration
public class NightlyPipelineJobConfig {

    @Bean
    public Job nightlyPipelineJob(
        JobRepository jobRepository,
        Step stockSnapshotStep,
        Step reintegrateExclusionsStep,
        Step aggregateSalesStep,
        Step refreshViewsStep,
        Step classifyStep,
        Step recalculateSemoisStep,
        Step createSuggestionsStep,
        Step inventaireTournantStep,
        Step facturationStep,
        Step certificationFneStep,
        Step avoirExpirationStep
    ) {
        return new JobBuilder("nightlyPipelineJob", jobRepository)
            .start(stockSnapshotStep)
            .next(reintegrateExclusionsStep)
            .next(aggregateSalesStep)
            .next(refreshViewsStep)
            .next(classifyStep)
            .next(recalculateSemoisStep)
            .next(createSuggestionsStep)
            .next(inventaireTournantStep)
            .next(facturationStep)
            .next(certificationFneStep)
            .next(avoirExpirationStep)
            .build();
    }
}
```

```java
// NightlyPipelineScheduler.java — remplace JobOrchestrationService
@Service
public class NightlyPipelineScheduler {

    private final JobLauncher asyncJobLauncher;
    private final Job nightlyPipelineJob;

    public NightlyPipelineScheduler(JobLauncher asyncJobLauncher, Job nightlyPipelineJob) {
        this.asyncJobLauncher = asyncJobLauncher;
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
            asyncJobLauncher.run(nightlyPipelineJob, params);
        } catch (JobInstanceAlreadyCompleteException e) {
            // Normal : job déjà terminé avec succès aujourd'hui
        } catch (Exception e) {
            // Spring Batch a déjà persisté l'état d'échec
        }
    }
}
```

---

## 7. Migration progressive

### Phase 0 — Extraction du module domain (1 semaine) ← NOUVELLE ÉTAPE

C'est le prérequis à tout le reste. Identifier et déplacer les classes partagées vers
`pharmaSmart-domain/`.

**Contenu de `pharmaSmart-domain` (à extraire de `src/`) :**

```
com.kobe.warehouse/
├── domain/               ← entités JPA (toutes)
├── repository/           ← Spring Data repos (tous)
├── service/dto/          ← DTOs (tous)
├── service/scheduler/    ← les 13 services scheduler (candidats batch)
├── service/stock/        ← services stock (utilisés par scheduler)
├── service/AppConfiguration*  ← configuration applicative
└── config/
    ├── DatabaseConfiguration.java
    └── CacheConfiguration.java
```

**Stratégie d'extraction :**
1. Créer `pharmaSmart-domain/` avec son `pom.xml`
2. Déplacer les packages via le refactoring IDE (IntelliJ : Refactor → Move)
3. Compiler `pharmaSmart-domain` seul : `mvnw.cmd install -pl pharmaSmart-domain`
4. Ajouter la dépendance dans `pharmaSmart-app/pom.xml`
5. Vérifier que `pharmaSmart-app` compile et que les tests passent
6. Vérifier que `mvnw.cmd clean package -Pprod` produit toujours `target/pharmaSmart-*.jar`
7. Vérifier que Tauri build fonctionne : `npm run tauri:build:bundled-jre`

### Phase 1 — Fondations batch (3 jours)

1. Créer `pharmaSmart-batch/` avec `pom.xml` et `BatchApplication.java`
2. Configurer `application.yml` (datasource, `web-application-type: none`, batch)
3. Ajouter `BatchConfiguration` + `asyncJobLauncher`
4. Vérifier que `pharmaSmart-batch` démarre et se connecte à la base

### Phase 2 — Extraction des méthodes (parallèle à phase 1)

Exposer des méthodes `package-private` dans les services existants :

```java
// SemoisCalculationService — AVANT
private SemoisUpdateResult processSingleProduit(Produit p, Map<...> paretoMap) { ... }

// APRÈS (accessible depuis le batch dans le même package)
SemoisUpdateResult calculateForProduit(Produit p) { ... }
```

### Phase 3 — Migration P0 (2 semaines)

1. Créer `SemoisCalculationJobConfig` + `SemoisItemProcessor` + `SemoisItemWriter`
2. Créer `ClassificationJobConfig` + processeurs associés
3. Intégrer dans `NightlyPipelineJobConfig`
4. Garder `JobOrchestrationService` **en parallèle** pendant 1 sprint de validation
5. Supprimer `JobOrchestrationService` après validation

### Phase 4 — Migration P1/P2 (2 semaines)

1. `VentesAgregeesJob` (Tasklet, trivial)
2. `SuggestionsReapproJob` (enchaîné SEMOIS)
3. `InventaireImportJob` (autonome)
4. `FacturationPeriodiqueueJob`

### Règle de coexistence (phases 3-4)

```java
// JobOrchestrationService — désactiver les étapes migrées au fur et à mesure
case RECALCULATE_SEMOIS -> {
    if (semoisBatchEnabled) {
        log.info("SEMOIS géré par Spring Batch — skip");
    } else {
        semoisCalculationService.recalculateAllConfigurations();
    }
}
```

```yaml
pharma-smart.batch.semois-enabled: true   # bascule progressive par flag
```

---

## 8. Monitoring & opérations

### 8.1 Actuator endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, batch
  endpoint:
    batch:
      enabled: true
```

- `GET /actuator/batch/jobs` — liste tous les jobs
- `GET /actuator/batch/jobs/{jobName}/instances` — instances d'un job
- `GET /actuator/batch/jobs/{jobName}/instances/{instanceId}/executions` — détail exécutions

### 8.2 Relancer un job échoué

```java
@PostMapping("/api/admin/batch/jobs/{jobName}/restart")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Long> restartJob(
    @PathVariable String jobName,
    JobExplorer jobExplorer,
    JobOperator jobOperator
) throws Exception {
    JobInstance lastInstance = jobExplorer.getLastJobInstance(jobName);
    List<JobExecution> executions = jobExplorer.getJobExecutions(lastInstance);
    JobExecution lastFailed = executions.stream()
        .filter(e -> e.getStatus() == BatchStatus.FAILED)
        .findFirst().orElseThrow();
    Long newExecutionId = jobOperator.restart(lastFailed.getId());
    return ResponseEntity.accepted().body(newExecutionId);
}
```

### 8.3 Tables de méta-données Spring Batch

| Table | Contenu |
|---|---|
| `BATCH_JOB_INSTANCE` | Instances (une par jour pour le pipeline) |
| `BATCH_JOB_EXECUTION` | Statut, start, end, exit code |
| `BATCH_STEP_EXECUTION` | Détail par step (readCount, writeCount, skipCount) |
| `BATCH_JOB_EXECUTION_CONTEXT` | Contexte partagé inter-steps (ParetoMap, etc.) |
| `BATCH_STEP_EXECUTION_CONTEXT` | Contexte step (checkpoint de restart) |

### 8.4 SkipListener — audit des items ignorés

```java
public class SemoisSkipListener implements SkipListener<Produit, SemoisUpdateResult> {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisSkipListener.class);

    @Override
    public void onSkipInProcess(Produit produit, Throwable t) {
        LOG.warn("[SEMOIS-SKIP] Produit id={} cip13={} ignoré : {}",
            produit.getId(), produit.getCip13(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(SemoisUpdateResult result, Throwable t) {
        LOG.error("[SEMOIS-SKIP-WRITE] Écriture ignorée pour produit id={} : {}",
            result.produit().getId(), t.getMessage());
    }
}
```

### 8.5 StepExecutionListener — remplacement des gardes idempotentes

```java
@Component
public class ClassificationStepListener implements StepExecutionListener {

    private final AppConfigurationService configService;

    public ClassificationStepListener(AppConfigurationService configService) {
        this.configService = configService;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            configService.updateValue("APP_LAST_DAY_CLASSIFICATION",
                LocalDate.now().toString());
        }
        return stepExecution.getExitStatus();
    }
}
```

---

## 9. Risques & contre-indications

### 9.1 Risques techniques

| Risque | Probabilité | Impact | Mitigation |
|---|---|---|---|
| Double exécution SEMOIS (Batch + Scheduler) | Haute pendant migration | Données corrompues | Désactiver `@Scheduled` dès activation du job Batch |
| `LazyInitializationException` dans `ItemProcessor` | Moyenne | Skip/rollback chunk | `JOIN FETCH` dans la JPQL du reader |
| Dépendances transitives imprévues dans domain | Moyenne | Compilation échoue | Extraire progressivement, compiler après chaque déplacement |
| Mémoire insuffisante (cache Pareto > 1M produits) | Faible (pharmacie ~10k produits) | OOM | `JobExecutionContext` vs Map en step |
| Conflit tables `BATCH_*` avec schéma `warehouse` | Faible | Erreur démarrage | `spring.batch.jdbc.table-prefix=BATCH_` (déjà configuré) |
| Ordre de build Maven incorrect | Moyenne | Compilation échoue | Déclarer `pharmaSmart-domain` avant `pharmaSmart-app` dans `<modules>` |

### 9.2 Ce qu'il ne faut PAS migrer

| Service | Raison |
|---|---|
| `MaterializedViewRefreshService` | SQL natif PostgreSQL pur, parallélisme custom via `@Async` — pas de chunk |
| `ScheduledReportService` | Génération PDF/email, pas de traitement volumétrique |
| `CertificationFneStatutService` | Une transaction par planification, Tasklet si migré |
| `UserService` nettoyage | Volume négligeable |

### 9.3 Contraintes Spring Boot 4 / Spring Batch 5

- `@EnableBatchProcessing` est **optionnel** en Boot 4 (auto-configuration disponible)
- Utiliser `JpaTransactionManager` (pas `DataSourceTransactionManager`) pour les chunks JPA
- `JpaPagingItemReader` : penser à `setTransacted(false)` si la transaction chunk est déjà ouverte

```java
// Dans les steps chunk avec JPA
.transactionManager(jpaTransactionManager)  // explicitement JPA, pas DataSource
```

---

## Résumé des livrables

### Nouveau module `pharmaSmart-domain/`

```
pharmaSmart-domain/
├── pom.xml
└── src/main/java/com/kobe/warehouse/
    ├── domain/               ← entités JPA (extraites de pharmaSmart-app)
    ├── repository/           ← repos Spring Data (extraits)
    ├── service/dto/          ← DTOs (extraits)
    ├── service/scheduler/    ← services batch (extraits)
    └── config/
        ├── DatabaseConfiguration.java
        └── CacheConfiguration.java
```

### Nouveau module `pharmaSmart-batch/`

```
pharmaSmart-batch/
├── pom.xml
└── src/main/java/com/kobe/warehouse/batch/
    ├── BatchApplication.java
    ├── config/
    │   └── BatchConfiguration.java
    ├── pipeline/
    │   ├── NightlyPipelineJobConfig.java
    │   └── NightlyPipelineScheduler.java      ← remplace JobOrchestrationService
    ├── semois/
    │   ├── SemoisCalculationJobConfig.java
    │   ├── SemoisItemProcessor.java
    │   ├── SemoisItemWriter.java
    │   ├── SemoisUpdateResult.java            ← record
    │   └── SemoisSkipListener.java
    ├── classification/
    │   ├── ClassificationJobConfig.java
    │   ├── ClassificationItemProcessor.java
    │   ├── ClassificationItemWriter.java
    │   ├── ClassificationResult.java
    │   └── ClassificationStepListener.java
    ├── suggestions/
    │   ├── SuggestionsItemReader.java
    │   ├── SuggestionsItemProcessor.java
    │   └── SuggestionsItemWriter.java
    └── inventaire/
        ├── InventaireImportJobConfig.java
        ├── InventaireLigneRaw.java
        ├── InventaireLineProcessor.java
        └── InventaireLineWriter.java
```

### Fichiers modifiés dans `pharmaSmart-app`

```
pom.xml                                         ← packaging=pom, <modules>, <dependencyManagement>
pharmaSmart-app/pom.xml                         ← nouveau, dépend de pharmaSmart-domain
src/main/resources/db/migration/
└── V1.9.0__spring_batch_schema.sql             ← méta-tables Spring Batch (optionnel)
```

**Services modifiés (extraction de méthodes uniquement, dans pharmaSmart-domain) :**
- `SemoisCalculationService` : `calculateForProduit(Produit)` package-private
- `ClassificationCriticiteService` : `classifyProduit(Produit)` package-private
- `SemoisBatchJobService` : `processSuggestionForProduit(ProduitEligible)` package-private

**Services inchangés :** tous les repositories, entités JPA, DTOs, services REST, controllers.
