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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    public Page<GapLineRecord> getLinesWithGap(Long inventoryId, Pageable pageable) {
        // Tri figé côté requête (écart absolu décroissant, id en départage) : un tri client
        // s'ajouterait au ORDER BY et casserait la stabilité de la pagination.
        return lineRepo.findLinesWithGap(
            inventoryId,
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
        );
    }

    @Override
    public void saveAnalysis(Long inventoryId, List<GapEntryRecord> entries) {
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }

        // Les lignes soumises sont rechargées en un seul select, restreint à l'inventaire :
        // `getReferenceById` dans la boucle déclenchait une requête par ligne à la première
        // lecture du gap, et n'empêchait pas d'écrire sur la ligne d'un autre inventaire.
        Map<Long, StoreInventoryLine> lines = lineRepo
            .findAllByIdInAndInventoryId(
                entries.stream().map(GapEntryRecord::lineId).filter(Objects::nonNull).collect(Collectors.toSet()),
                inventoryId
            )
            .stream()
            .collect(Collectors.toMap(StoreInventoryLine::getId, line -> line));

        if (lines.isEmpty()) {
            return;
        }

        // Qualifications déjà en base pour ces lignes : on met à jour au lieu de recréer,
        // ce qui préserve `created_at` et évite un delete/insert par enregistrement.
        Map<Long, InventoryGapAnalysis> existing = gapRepo
            .findAllByStoreInventoryLineIdIn(lines.keySet())
            .stream()
            .collect(Collectors.toMap(ga -> ga.getStoreInventoryLine().getId(), ga -> ga, (a, b) -> a));

        List<InventoryGapAnalysis> toSave = new ArrayList<>();
        List<Long> toClear = new ArrayList<>();

        for (GapEntryRecord entry : entries) {
            StoreInventoryLine line = lines.get(entry.lineId());
            if (line == null) {
                continue;
            }
            if (entry.cause() == null || entry.cause().isBlank()) {
                toClear.add(line.getId());
                continue;
            }
            InventoryGapAnalysis ga = existing.getOrDefault(line.getId(), new InventoryGapAnalysis());
            ga.setStoreInventoryLine(line);
            ga.setCause(CauseEcart.valueOf(entry.cause()));
            ga.setQuantity(Math.abs(resolveGap(line)));
            ga.setCommentaire(entry.commentaire());
            toSave.add(ga);
        }

        if (!toClear.isEmpty()) {
            gapRepo.deleteAllByStoreInventoryLineIdIn(toClear);
        }
        if (!toSave.isEmpty()) {
            gapRepo.saveAll(toSave);
        }
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
