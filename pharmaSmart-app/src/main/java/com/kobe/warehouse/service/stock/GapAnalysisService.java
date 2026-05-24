package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.GapEntryRecord;
import com.kobe.warehouse.service.dto.records.GapLineRecord;
import com.kobe.warehouse.service.dto.records.GapSummaryRecord;
import java.util.List;

public interface GapAnalysisService {

    /** Lignes de l'inventaire ayant un écart ≠ 0, triées par écart absolu décroissant. */
    List<GapLineRecord> getLinesWithGap(Long inventoryId);

    /**
     * Sauvegarde (ou écrase) la qualification des écarts pour un inventaire.
     * Idempotent : un second appel remplace le précédent.
     */
    void saveAnalysis(Long inventoryId, List<GapEntryRecord> entries);

    /** Résumé agrégé par cause. Vide si aucune qualification n'a été saisie. */
    List<GapSummaryRecord> getSummary(Long inventoryId);

    /** Indique si une qualification a déjà été saisie pour cet inventaire. */
    boolean hasAnalysis(Long inventoryId);
}
