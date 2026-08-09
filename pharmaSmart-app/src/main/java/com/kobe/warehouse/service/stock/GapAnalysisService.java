package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.GapEntryRecord;
import com.kobe.warehouse.service.dto.records.GapLineRecord;
import com.kobe.warehouse.service.dto.records.GapSummaryRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GapAnalysisService {

    /** Page des lignes de l'inventaire ayant un écart ≠ 0, triées par écart absolu décroissant. */
    Page<GapLineRecord> getLinesWithGap(Long inventoryId, Pageable pageable);

    /**
     * Enregistre la qualification des lignes présentes dans {@code entries}, et seulement
     * celles-là : une ligne absente de la charge utile garde sa qualification.
     *
     * <p>Sémantique d'upsert, et non de remplacement global — l'écran est paginé, il ne
     * soumet que ce que l'opérateur a modifié. Un `delete all` effacerait les qualifications
     * des pages non visitées.
     *
     * <p>Une entrée dont la cause est vide supprime la qualification de sa ligne.
     * Idempotent : rejouer la même charge utile produit le même état.
     */
    void saveAnalysis(Long inventoryId, List<GapEntryRecord> entries);

    /** Résumé agrégé par cause. Vide si aucune qualification n'a été saisie. */
    List<GapSummaryRecord> getSummary(Long inventoryId);

    /** Indique si une qualification a déjà été saisie pour cet inventaire. */
    boolean hasAnalysis(Long inventoryId);
}
