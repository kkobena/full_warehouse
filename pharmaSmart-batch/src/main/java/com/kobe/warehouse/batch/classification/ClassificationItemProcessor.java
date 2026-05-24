package com.kobe.warehouse.batch.classification;

import com.kobe.warehouse.domain.ClassificationConfig;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.service.classification.ClassificationBatchProcessor;
import com.kobe.warehouse.service.classification.ClassificationBatchProcessor.ParetoScore;
import com.kobe.warehouse.service.scheduler.ClassificationCriticiteService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ClassificationItemProcessor implements ItemProcessor<Produit, ClassificationResult> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassificationItemProcessor.class);

    private final ClassificationBatchProcessor batchProcessor;
    private final ClassificationCriticiteService classificationService;

    private ClassificationConfig config;
    private Map<Integer, ParetoScore> paretoMap;

    public ClassificationItemProcessor(
        ClassificationBatchProcessor batchProcessor,
        ClassificationCriticiteService classificationService
    ) {
        this.batchProcessor = batchProcessor;
        this.classificationService = classificationService;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.config = classificationService.getConfig();
        this.paretoMap = classificationService.loadParetoMap();
        LOG.info("[CLASSIFICATION-PROCESSOR] Config et Pareto chargés: {} scores", paretoMap.size());
    }

    @Override
    public ClassificationResult process(Produit produit) {
        if (Boolean.TRUE.equals(produit.getIsClassificationOverridden())) {
            return null;
        }

        int ancienneteMois = produit.getCreatedAt() != null
            ? (int) ChronoUnit.MONTHS.between(produit.getCreatedAt().toLocalDate(), LocalDate.now())
            : Integer.MAX_VALUE;
        if (ancienneteMois < config.getNbMoisMinNouveauProduit()) {
            return null;
        }

        ParetoScore pareto = paretoMap.getOrDefault(produit.getId(), ParetoScore.NO_SALES);
        ClasseCriticite classeActuelle = produit.getEffectiveClasseCriticite();
        ClasseCriticite classeSuggeree = batchProcessor.determinerClasse(pareto, produit, config);

        if (classeSuggeree == classeActuelle) {
            return null;
        }

        if (!batchProcessor.passeHysteresis(pareto.caCumulePct(), classeActuelle, classeSuggeree, config)) {
            return null;
        }

        produit.setClasseCriticite(classeSuggeree);
        return new ClassificationResult(produit, classeActuelle, classeSuggeree, true, pareto);
    }
}
