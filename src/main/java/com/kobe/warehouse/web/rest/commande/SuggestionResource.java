package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SuggestionResource {

    private final SuggestionProduitService suggestionProduitService;

    public SuggestionResource(SuggestionProduitService suggestionProduitService) {
        this.suggestionProduitService = suggestionProduitService;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<Page<SuggestionProjection>> getAllSuggestions(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer fournisseurId,
        @RequestParam(required = false) TypeSuggession typeSuggession,
        @RequestParam(required = false) StatutSuggession statut,
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            suggestionProduitService.getAllSuggestion(search, fournisseurId, typeSuggession, statut, pageable)
        );
    }

    /**
     * GET /suggestions/count-by-statut : nombre de suggestions par statut.
     * Utilisé pour les badges onglets de l'UI.
     */
    @GetMapping("/suggestions/count-by-statut")
    public ResponseEntity<Long> countByStatut(@RequestParam StatutSuggession statut) {
        return ResponseEntity.ok(suggestionProduitService.countByStatut(statut));
    }

    @GetMapping("/suggestions/par-fournisseur")
    public ResponseEntity<List<FournisseurSuggestionSummaryDTO>> getSuggestionsParFournisseur(
        @RequestParam(required = false) StatutSuggession statut
    ) {
        return ResponseEntity.ok(suggestionProduitService.getSuggestionsParFournisseur(statut));
    }

    @GetMapping("/suggestions/{id}")
    public ResponseEntity<SuggestionDTO> getSuggestionById(@PathVariable Integer id) {
        Optional<SuggestionDTO> dto = suggestionProduitService.getSuggestionById(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/suggestions/{id}/lines")
    public ResponseEntity<Page<SuggestionLineDTO>> getSuggestionLines(
        @PathVariable Integer id,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String niveauUrgence,
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            suggestionProduitService.getSuggestionLinesByIdWithConsommation(id, search, niveauUrgence, pageable)
        );
    }

    /**
     * GET /suggestions/{id}/all-lines : toutes les lignes sans pagination.
     * Utilisé par le composant d'édition de suggestion.
     */
    @GetMapping("/suggestions/{id}/all-lines")
    public ResponseEntity<List<SuggestionLineDTO>> getAllSuggestionLines(
        @PathVariable Integer id,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String niveauUrgence
    ) {
        return ResponseEntity.ok(
            suggestionProduitService.getAllSuggestionLines(id, search, niveauUrgence)
        );
    }

    @PostMapping("/suggestions/fusionner")
    public ResponseEntity<Void> fusionner(@RequestBody Set<Integer> ids) throws GenericError {
        suggestionProduitService.fusionnerSuggestion(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/suggestions")
    public ResponseEntity<Void> deleteSuggestions(@RequestBody Set<Integer> ids) {
        suggestionProduitService.deleteSuggestion(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/suggestions/lines")
    public ResponseEntity<Void> deleteSuggestionLines(@RequestBody Set<Integer> ids) {
        suggestionProduitService.deleteSuggestionLine(ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/{id}/sanitize")
    public ResponseEntity<Void> sanitize(@PathVariable Integer id) {
        suggestionProduitService.sanitize(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/{id}/commander")
    public ResponseEntity<Void> commander(@PathVariable Integer id) {
        suggestionProduitService.commander(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/commander-selection")
    public ResponseEntity<Void> commanderSelection(@RequestBody CommanderSelectionDTO dto) {
        suggestionProduitService.commanderSelection(dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suggestions/budget-commande")
    public ResponseEntity<BudgetCommandeDTO> getBudgetCommande() {
        return ResponseEntity.ok(suggestionProduitService.getBudgetCommande());
    }

    @PostMapping("/suggestions/{id}/valider")
    public ResponseEntity<Void> valider(@PathVariable Integer id) {
        suggestionProduitService.validerSuggestion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/{id}/rejeter")
    public ResponseEntity<Void> rejeter(@PathVariable Integer id) {
        suggestionProduitService.rejeterSuggestion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/{suggestionId}/lines")
    public ResponseEntity<Void> addLine(
        @PathVariable Integer suggestionId,
        @RequestBody SuggestionLineDTO line
    ) {
        suggestionProduitService.addSuggestionLine(suggestionId, line);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/suggestions/lines/quantity")
    public ResponseEntity<Void> updateLineQuantity(@RequestBody SuggestionLineDTO line) {
        suggestionProduitService.updateSuggestionLinQuantity(line);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /suggestions/lines/{id}/reset-quantite : réinitialise le flag
     * {@code quantiteModifieeManuel} d'une ligne — le batch pourra à nouveau calculer sa quantité.
     */
    @PutMapping("/suggestions/lines/{id}/reset-quantite")
    public ResponseEntity<Void> resetQuantiteManuelle(@PathVariable Integer id) {
        suggestionProduitService.resetQuantiteManuelle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suggestions/{id}/export-csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Integer id) throws Exception {
        byte[] data = suggestionProduitService.exportToCsv(id);
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv; charset=UTF-8")
            .header("Content-Disposition", "attachment; filename=\"suggestion-" + id + ".csv\"")
            .body(data);
    }

    @GetMapping("/suggestions/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Integer id) {
        byte[] data = suggestionProduitService.exportToPdf(id);
        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=\"suggestion-" + id + ".pdf\"")
            .body(data);
    }

    @PostMapping("/suggestions/suggestion-quantite-produit-vendus")
    public ResponseEntity<Integer> suggestionQuantiteProduitVendus(
        @RequestBody List<QauntiteProduitVendus> produitVendus,
        @RequestParam(required = false) Boolean suggerQuantitySold
    ) {
        return ResponseEntity.ok(suggestionProduitService.suggestionQuantiteProduitVendus(produitVendus, suggerQuantitySold));
    }
}
