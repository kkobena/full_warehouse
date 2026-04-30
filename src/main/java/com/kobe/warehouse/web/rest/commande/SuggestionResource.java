package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.Keys;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class SuggestionResource {

    private final SuggestionProduitService suggestionProduitService;

    public SuggestionResource(SuggestionProduitService suggestionProduitService) {
        this.suggestionProduitService = suggestionProduitService;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<SuggestionProjection>> getAllSuggestions(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Set<Integer> fournisseurIds,
        @RequestParam(required = false) TypeSuggession typeSuggession,
        @RequestParam(required = false) Set<StatutSuggession> statut,
        Pageable pageable
    ) {

        Page<SuggestionProjection> page = suggestionProduitService.getAllSuggestion(search, fournisseurIds, typeSuggession, statut, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());

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
        @RequestParam(required = false) Set<StatutSuggession> statut,
        @RequestParam(required = false) Set<Integer> fournisseurIds,
        @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(suggestionProduitService.getSuggestionsParFournisseur(statut, fournisseurIds, search));
    }

    @GetMapping("/suggestions/{id}")
    public ResponseEntity<SuggestionDTO> getSuggestionById(@PathVariable Integer id) {
        Optional<SuggestionDTO> dto = suggestionProduitService.getSuggestionById(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/suggestions/{id}/lines")
    public ResponseEntity<List<SuggestionLineDTO>> getSuggestionLines(
        @PathVariable Integer id,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String niveauUrgence,
        Pageable pageable
    ) {
        Page<SuggestionLineDTO> page = suggestionProduitService.getSuggestionLinesByIdWithConsommation(id, search, niveauUrgence, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());

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

    /**
     * POST /suggestions/fusionner — fusionne plusieurs suggestions.
     * Body : {@code {"ids":[1,2,...]}} (record {@link Keys}).
     */
    @PostMapping("/suggestions/fusionner")
    public ResponseEntity<Void> fusionner(@RequestBody Keys keys) throws GenericError {
        if (keys.safeIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        suggestionProduitService.fusionnerSuggestion(new HashSet<>(keys.safeIds()));
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /suggestions/delete — supprime une ou plusieurs suggestions.
     * Body : {@code {"ids":[1,2,...]}} (record {@link Keys}).
     */
    @PostMapping("/suggestions/delete")
    public ResponseEntity<Void> deleteSuggestionsPost(@RequestBody Keys keys) {
        if (!keys.safeIds().isEmpty()) {
            suggestionProduitService.deleteSuggestion(new HashSet<>(keys.safeIds()));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /suggestions/delete/lines — supprime une ou plusieurs lignes de suggestion.
     * Body : {@code {"ids":[1,2,...]}} (record {@link Keys}).
     */
    @PostMapping("/suggestions/delete/lines")
    public ResponseEntity<Void> deleteSuggestionLinesPost(@RequestBody Keys keys) {
        if (!keys.safeIds().isEmpty()) {
            suggestionProduitService.deleteSuggestionLine(new HashSet<>(keys.safeIds()));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /suggestions/add-item/{id} — ajoute ou met à jour un produit dans une suggestion.
     * Alias de POST /suggestions/{id}/lines pour la compatibilité frontend.
     */
    @PostMapping("/suggestions/add-item/{id}")
    public ResponseEntity<Void> addOrUpdateItem(
        @PathVariable Integer id,
        @RequestBody SuggestionLineDTO line
    ) {
        suggestionProduitService.addSuggestionLine(id, line);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /suggestions/sanitize/{id} — nettoie (sanitize) une suggestion.
     * La version POST est conservée ; cet alias DELETE est pour la compatibilité frontend.
     */
    @DeleteMapping("/suggestions/sanitize/{id}")
    public ResponseEntity<Void> sanitizeDelete(@PathVariable Integer id) {
        suggestionProduitService.sanitize(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /suggestions/{id} — rejette (supprime) une suggestion par son ID.
     * Utilisé par le frontend pour l'action "rejeter".
     */
    @DeleteMapping("/suggestions/{id}")
    public ResponseEntity<Void> deleteSuggestionById(@PathVariable Integer id) {
        suggestionProduitService.rejeterSuggestion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/{id}/commander")
    public ResponseEntity<CommandeId> commander(
        @PathVariable Integer id,
        @RequestParam(required = false) Integer fournisseurId
    ) {
        return ResponseEntity.ok(suggestionProduitService.commander(id, fournisseurId));
    }

    @PostMapping("/suggestions/commander-selection")
    public ResponseEntity<CommandeId> commanderSelection(@RequestBody CommanderSelectionDTO dto) {
        CommandeId commandeId = suggestionProduitService.commanderSelection(dto);
        return ResponseEntity.ok(commandeId);
    }

    @GetMapping("/suggestions/budget-commande")
    public ResponseEntity<BudgetCommandeDTO> getBudgetCommande() {
        return ResponseEntity.ok(suggestionProduitService.getBudgetCommande());
    }

    @PutMapping("/suggestions/{id}/valider")
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
