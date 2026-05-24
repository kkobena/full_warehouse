package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.dto.records.GapEntryRecord;
import com.kobe.warehouse.service.dto.records.GapLineRecord;
import com.kobe.warehouse.service.dto.records.GapSummaryRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryByGroupRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import com.kobe.warehouse.service.stock.GapAnalysisService;
import com.kobe.warehouse.service.stock.InventoryValuationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventaires/{id}")
public class GapAnalysisResource {

    private final GapAnalysisService gapService;
    private final InventoryValuationService valuationService;

    public GapAnalysisResource(GapAnalysisService gapService, InventoryValuationService valuationService) {
        this.gapService = gapService;
        this.valuationService = valuationService;
    }

    /** Lignes présentant un écart — pour alimenter le modal de qualification. */
    @GetMapping("/gap-lines")
    public ResponseEntity<List<GapLineRecord>> getGapLines(@PathVariable Long id) {
        return ResponseEntity.ok(gapService.getLinesWithGap(id));
    }

    /** Sauvegarde la qualification des causes d'écart. Ne bloque pas la clôture. */
    @PostMapping("/gap-analysis")
    public ResponseEntity<Void> saveAnalysis(
        @PathVariable Long id,
        @RequestBody List<GapEntryRecord> entries
    ) {
        gapService.saveAnalysis(id, entries);
        return ResponseEntity.noContent().build();
    }

    /** Résumé agrégé par cause. */
    @GetMapping("/gap-summary")
    public ResponseEntity<List<GapSummaryRecord>> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(gapService.getSummary(id));
    }

    /** Indique si une qualification existe déjà. */
    @GetMapping("/gap-analysis/exists")
    public ResponseEntity<Map<String, Boolean>> hasAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("exists", gapService.hasAnalysis(id)));
    }

    // ── Valorisation ventilée (5.4) ──────────────────────────────────────────

    /** Résumé global de valorisation (avant / après / écart). */
    @GetMapping("/valuation")
    public ResponseEntity<StoreInventorySummaryRecord> getValuation(@PathVariable Long id) {
        return ResponseEntity.ok(valuationService.getGlobalSummary(id));
    }

    /**
     * Ventilation par groupe.
     *
     * @param groupBy STORAGE (défaut) | FAMILLE | RAYON
     */
    @GetMapping("/valuation/by-group")
    public ResponseEntity<List<StoreInventorySummaryByGroupRecord>> getValuationByGroup(
        @PathVariable Long id,
        @RequestParam(defaultValue = "STORAGE") String groupBy
    ) {
        return ResponseEntity.ok(valuationService.getSummaryByGroup(id, groupBy));
    }
}
