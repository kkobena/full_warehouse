package com.kobe.warehouse.batch.semois;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.repository.SemoisClasseConfigRepository;
import com.kobe.warehouse.service.scheduler.SemoisCalculationService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Délègue le traitement du chunk à {@code SemoisCalculationService.processBatch()},
 * préservant ainsi le batch-loading SQL optimisé (pas de N+1 par produit).
 *
 * <p>Le flag {@code canUpdateProduitReapproInfo} est évalué en {@link BeforeStep}
 * (une seule fois par step) puis réutilisé pour tout le chunk.
 */
@Component
public class SemoisItemWriter implements ItemWriter<Produit> {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisItemWriter.class);
    private static final String APP_LAST_REAPPRO_DATE = "APP_LAST_DAY_REAPPRO_CALCULATION";

    private final SemoisCalculationService semoisCalculationService;
    private final SemoisClasseConfigRepository semoisClasseConfigRepository;
    private final AppConfigurationService appConfigurationService;

    private Map<ClasseCriticite, SemoisClasseConfig> classeConfigMap;
    private boolean canUpdateProduitReapproInfo;

    public SemoisItemWriter(
        SemoisCalculationService semoisCalculationService,
        SemoisClasseConfigRepository semoisClasseConfigRepository,
        AppConfigurationService appConfigurationService
    ) {
        this.semoisCalculationService = semoisCalculationService;
        this.semoisClasseConfigRepository = semoisClasseConfigRepository;
        this.appConfigurationService = appConfigurationService;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.classeConfigMap = semoisClasseConfigRepository.findAll()
            .stream()
            .collect(Collectors.toMap(SemoisClasseConfig::getClasseCriticite, Function.identity()));

        LocalDate lastReapproDate = appConfigurationService
            .findOneById(APP_LAST_REAPPRO_DATE)
            .map(cfg -> cfg.getValue() != null && !cfg.getValue().isBlank()
                ? LocalDate.parse(cfg.getValue()) : null)
            .orElse(null);

        this.canUpdateProduitReapproInfo = lastReapproDate == null
            || !lastReapproDate.withDayOfMonth(1).equals(LocalDate.now().withDayOfMonth(1));

        LOG.info("[SEMOIS-WRITER] classeConfigMap={} entrées, canUpdateReapproInfo={}",
            classeConfigMap.size(), canUpdateProduitReapproInfo);
    }

    @Override
    public void write(Chunk<? extends Produit> chunk) {
        if (classeConfigMap.isEmpty()) {
            LOG.warn("[SEMOIS-WRITER] Aucune SemoisClasseConfig — chunk ignoré");
            return;
        }
        var result = semoisCalculationService.processBatch(
            new ArrayList<>(chunk.getItems()),
            canUpdateProduitReapproInfo,
            classeConfigMap
        );
        LOG.debug("[SEMOIS-WRITER] chunk={} items — succès={}, erreurs={}",
            chunk.size(), result.successCount(), result.errorCount());
    }
}
