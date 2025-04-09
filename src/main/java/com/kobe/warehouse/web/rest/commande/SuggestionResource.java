package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.projection.SuggestionProjection;
import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionResource {

    private final SuggestionProduitService suggestionProduitService;

    public SuggestionResource(SuggestionProduitService suggestionProduitService) {
        this.suggestionProduitService = suggestionProduitService;
    }

    @GetMapping
    public ResponseEntity<List<SuggestionProjection>> getAll(
        @RequestParam(required = false, name = "typeSuggession") TypeSuggession typeSuggession,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "fournisseurId") Long fournisseurId,
        Pageable pageable
    ) {
        Page<SuggestionProjection> page = suggestionProduitService.getAllSuggestion(search, fournisseurId, typeSuggession, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuggestionDTO> getCommande(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(suggestionProduitService.getSuggestionById(id));
    }

    @GetMapping("/items")
    public ResponseEntity<List<SuggestionLineDTO>> getItems(
        @RequestParam(name = "suggestionId") long suggestionId,
        @RequestParam(required = false, name = "search") String search,
        Pageable pageable
    ) {
        Page<SuggestionLineDTO> page = suggestionProduitService.getSuggestionLinesById(suggestionId, search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody Keys keys) {
        suggestionProduitService.deleteSuggestion(keys.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/items")
    public ResponseEntity<Void> deleteItems(@RequestBody Keys keys) {
        suggestionProduitService.deleteSuggestionLine(keys.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sanitize/{id}")
    public ResponseEntity<Void> sanitize(@PathVariable Long id) {
        suggestionProduitService.sanitize(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fusionner")
    public ResponseEntity<Void> fusionner(@RequestBody Keys keys) {
        suggestionProduitService.fusionnerSuggestion(keys.ids());
        return ResponseEntity.ok().build();
    }
}
