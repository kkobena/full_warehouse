package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.InventoryGapAnalysis;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.CauseEcart;
import com.kobe.warehouse.repository.InventoryGapAnalysisRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.dto.records.GapEntryRecord;
import com.kobe.warehouse.service.dto.records.GapLineRecord;
import com.kobe.warehouse.service.dto.records.GapSummaryRecord;
import com.kobe.warehouse.service.stock.GapAnalysisService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GapAnalysisServiceImpl implements GapAnalysisService {

    private static final Map<String, String> CAUSE_LABELS = Map.of(
        "CASSE",            "Casse / dommage",
        "VOL",              "Vol",
        "ERREUR_RECEPTION", "Erreur de réception",
        "ERREUR_SAISIE",    "Erreur de saisie",
        "PEREMPTION",       "Péremption",
        "INCONNU",          "Cause inconnue"
    );

    private final InventoryGapAnalysisRepository gapRepo;
    private final StoreInventoryLineRepository lineRepo;

    public GapAnalysisServiceImpl(
        InventoryGapAnalysisRepository gapRepo,
        StoreInventoryLineRepository lineRepo
    ) {
        this.gapRepo = gapRepo;
        this.lineRepo = lineRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GapLineRecord> getLinesWithGap(Long inventoryId) {
        List<StoreInventoryLine> lines = lineRepo.findLinesWithGap(inventoryId);

        // Récupérer les qualifications existantes indexées par lineId
        Map<Long, InventoryGapAnalysis> existing = gapRepo
            .findAllByInventoryId(inventoryId)
            .stream()
            .collect(Collectors.toMap(
                ga -> ga.getStoreInventoryLine().getId(),
                ga -> ga,
                (a, b) -> a // garder le premier si doublon (ne devrait pas arriver)
            ));

        return lines.stream()
            .map(line -> {
                InventoryGapAnalysis ga = existing.get(line.getId());
                Integer prix = line.getLastUnitPrice() != null ? line.getLastUnitPrice() : 0;
                int valeur = Math.abs(line.getGap() != null ? line.getGap() : 0) * prix;
                return new GapLineRecord(
                    line.getId(),
                    line.getProduit().getLibelle(),
                    line.getQuantityInit(),
                    line.getQuantityOnHand(),
                    line.getGap(),
                    valeur,
                    ga != null ? ga.getCause().name() : null,
                    ga != null ? ga.getCommentaire() : null
                );
            })
            .toList();
    }

    @Override
    public void saveAnalysis(Long inventoryId, List<GapEntryRecord> entries) {
        gapRepo.deleteAllByInventoryId(inventoryId);

        List<InventoryGapAnalysis> toSave = entries.stream()
            .filter(e -> e.cause() != null && !e.cause().isBlank())
            .map(e -> {
                InventoryGapAnalysis ga = new InventoryGapAnalysis();
                StoreInventoryLine line = lineRepo.getReferenceById(e.lineId());
                ga.setStoreInventoryLine(line);
                ga.setCause(CauseEcart.valueOf(e.cause()));
                ga.setQuantity(Math.abs(resolveGap(line)));
                ga.setCommentaire(e.commentaire());
                return ga;
            })
            .toList();

        gapRepo.saveAll(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GapSummaryRecord> getSummary(Long inventoryId) {
        return gapRepo.aggregateByInventoryId(inventoryId)
            .stream()
            .map(row -> {
                String cause = ((CauseEcart) row[0]).name();
                return new GapSummaryRecord(
                    cause,
                    CAUSE_LABELS.getOrDefault(cause, cause),
                    (Long) row[1],
                    (Long) row[2]
                );
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnalysis(Long inventoryId) {
        return gapRepo.existsByStoreInventoryLineStoreInventoryId(inventoryId);
    }

    private int resolveGap(StoreInventoryLine line) {
        // Le gap est déjà calculé sur la ligne ; on le relit directement
        return line.getGap() != null ? line.getGap() : 0;
    }
}
