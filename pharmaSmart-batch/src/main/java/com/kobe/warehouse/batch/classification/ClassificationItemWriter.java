package com.kobe.warehouse.batch.classification;

import com.kobe.warehouse.domain.ClassificationCriticiteLog;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import com.kobe.warehouse.repository.ClassificationCriticiteLogRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class ClassificationItemWriter implements ItemWriter<ClassificationResult> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassificationItemWriter.class);
    private static final String RAISON = "Reclassification mensuelle automatique";

    private final ProduitRepository produitRepository;
    private final ClassificationCriticiteLogRepository logRepository;

    public ClassificationItemWriter(
        ProduitRepository produitRepository,
        ClassificationCriticiteLogRepository logRepository
    ) {
        this.produitRepository = produitRepository;
        this.logRepository = logRepository;
    }

    @Override
    public void write(Chunk<? extends ClassificationResult> chunk) {
        List<Produit> toSave = new ArrayList<>(chunk.size());
        List<ClassificationCriticiteLog> logs = new ArrayList<>(chunk.size());

        for (ClassificationResult result : chunk.getItems()) {
            toSave.add(result.produit());
            logs.add(new ClassificationCriticiteLog()
                .setProduit(result.produit())
                .setAncienneClasse(result.ancienneClasse())
                .setNouvelleClasse(result.nouvelleClasse())
                .setCa12Mois(result.paretoScore().ca12Mois())
                .setFrequenceVenteMois(result.paretoScore().frequenceMois())
                .setScoreTotal(result.paretoScore().caCumulePct())
                .setRaisonChangement(RAISON)
                .setClassificationType(ClassificationType.AUTO)
            );
        }

        produitRepository.saveAll(toSave);
        logRepository.saveAll(logs);
        LOG.info("[CLASSIFICATION-WRITER] {} produits reclassifiés", toSave.size());
    }
}
