package com.kobe.warehouse.web.rest.reassort;

import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.reassort.dto.SuggestionReassortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing suggestion reassort (stock replenishment suggestions)
 */
@RestController
@RequestMapping("/api/suggestion-reassort")
public class SuggestionReassortResource {

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionReassortResource.class);
    private final SuggestionReassortService suggestionReassortService;

    public SuggestionReassortResource(SuggestionReassortService suggestionReassortService) {
        this.suggestionReassortService = suggestionReassortService;
    }

    /**
     * {@code GET /api/suggestion-reassort/open} : Get all open suggestions
     *
     * @param typeReassort filter by type (RAYON or RESERVE)
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of suggestions in body
     */
    @GetMapping("/open")
    public ResponseEntity<List<SuggestionReassortDto>> getOpenSuggestions(
        @RequestParam(required = false) TypeReassort typeReassort
    ) {
        LOG.debug("REST request to get open suggestions, type: {}", typeReassort);
        List<SuggestionReassortDto> suggestions = suggestionReassortService.getOpenningSuggestions(typeReassort);
        return ResponseEntity.ok().body(suggestions);
    }

    /**
     * {@code PUT /api/suggestion-reassort/ligne/:id} : Update ligne reassort quantity
     *
     * @param id       the ligne reassort id
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}
     */
    @PutMapping("/ligne/{id}")
    public ResponseEntity<Void> updateLigneQuantity(
        @PathVariable Integer id,
        @RequestBody UpdateQuantityRequest request
    ) {
        LOG.debug("REST request to update ligne reassort {} quantity to {}", id, request.quantity());
        suggestionReassortService.updateLigneReassort(id, request.quantity());
        return ResponseEntity.ok().build();
    }

    /**
     * {@code DELETE /api/suggestion-reassort/ligne/:id} : Delete a ligne reassort
     *
     * @param id the ligne reassort id
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}
     */
    @DeleteMapping("/ligne/{id}")
    public ResponseEntity<Void> deleteLigne(@PathVariable Integer id) {
        LOG.debug("REST request to delete ligne reassort: {}", id);
        suggestionReassortService.deleteLigneReassort(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code POST /api/suggestion-reassort/:id/validate} : Validate and execute suggestion
     *
     * @param id the suggestion reassort id
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> validateSuggestion(@PathVariable Integer id) {
        LOG.debug("REST request to validate suggestion reassort: {}", id);
        suggestionReassortService.validateSuggestionReassort(id);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code DELETE /api/suggestion-reassort/:id} : Delete a suggestion reassort
     *
     * @param id the suggestion reassort id
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSuggestion(@PathVariable Integer id) {
        LOG.debug("REST request to delete suggestion reassort: {}", id);
        suggestionReassortService.deleteSuggestionReassort(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Request DTO for updating quantity
     */
    public record UpdateQuantityRequest(int quantity) {}
}
