package com.kobe.warehouse.batch.inventaire;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persiste les lignes d'inventaire validées dans {@code StoreInventoryLine}.
 *
 * <p>Paramètre de job requis : {@code storeInventoryId} (Long).
 * Paramètre de job requis : {@code filePath} (String).
 *
 * <p>Phase 3 (futur) : gestion complète des lots ({@code InventoryLot})
 * avec lot + péremption par ligne.
 */
@Component
public class InventaireLineWriter implements ItemWriter<InventaireLigneValidee> {

    private static final Logger LOG = LoggerFactory.getLogger(InventaireLineWriter.class);

    private final StoreInventoryLineRepository lineRepository;
    private Long storeInventoryId;

    public InventaireLineWriter(StoreInventoryLineRepository lineRepository) {
        this.lineRepository = lineRepository;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.storeInventoryId = stepExecution.getJobParameters().getLong("storeInventoryId");
        if (storeInventoryId == null) {
            LOG.warn("[INVENTAIRE-WRITER] Paramètre storeInventoryId manquant — les écritures seront ignorées");
        } else {
            LOG.info("[INVENTAIRE-WRITER] Inventaire cible : id={}", storeInventoryId);
        }
    }

    @Override
    public void write(Chunk<? extends InventaireLigneValidee> chunk) {
        if (storeInventoryId == null) {
            LOG.warn("[INVENTAIRE-WRITER] storeInventoryId absent — {} lignes ignorées", chunk.size());
            return;
        }

        Set<String> cips = chunk.getItems().stream()
            .map(InventaireLigneValidee::cip13)
            .collect(Collectors.toSet());

        Map<String, Integer> quantiteByCip = chunk.getItems().stream()
            .collect(Collectors.toMap(
                InventaireLigneValidee::cip13,
                InventaireLigneValidee::quantite,
                (existing, replacement) -> replacement
            ));

        // JOIN FETCH produit + fournisseurProduitPrincipal chargé en EAGER (@ManyToOne)
        var lines = lineRepository.findAllByStoreInventoryIdAndCodeCipIn(storeInventoryId, cips);

        var toSave = new ArrayList<StoreInventoryLine>(lines.size());
        for (StoreInventoryLine line : lines) {
            String cip = extractCip(line);
            if (cip == null) continue;

            Integer quantite = quantiteByCip.get(cip);
            if (quantite == null) continue;

            int init = line.getQuantityInit() != null ? line.getQuantityInit() : 0;
            line.setQuantityOnHand(quantite);
            line.setGap(quantite - init);
            line.setUpdated(true);
            line.setUpdatedAt(LocalDateTime.now());
            toSave.add(line);
        }

        lineRepository.saveAll(toSave);
        LOG.info("[INVENTAIRE-WRITER] {} lignes mises à jour / {} CIPs dans le chunk",
            toSave.size(), cips.size());
    }

    private String extractCip(StoreInventoryLine line) {
        FournisseurProduit fp = line.getProduit() != null
            ? line.getProduit().getFournisseurProduitPrincipal()
            : null;
        return fp != null ? fp.getCodeCip() : null;
    }
}
