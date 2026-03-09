package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
        @RequestParam(required = false, name = "fournisseurId") Integer fournisseurId,
        Pageable pageable
    ) {
        Page<SuggestionProjection> page = suggestionProduitService.getAllSuggestion(search, fournisseurId, typeSuggession, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuggestionDTO> getCommande(@PathVariable Integer id) {
        return ResponseUtil.wrapOrNotFound(suggestionProduitService.getSuggestionById(id));
    }



    @GetMapping("/items")
    public ResponseEntity<List<SuggestionLineDTO>> getItemsWithConsommation(
        @RequestParam(name = "suggestionId") Integer suggestionId,
        @RequestParam(required = false, name = "search") String search,
        Pageable pageable
    ) {
        Page<SuggestionLineDTO> page = suggestionProduitService.getSuggestionLinesByIdWithConsommation(suggestionId, search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody Keys keys) {
        suggestionProduitService.deleteSuggestion(keys.ids());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/delete/items")
    public ResponseEntity<Void> deleteItems(@RequestBody Keys keys) {
        suggestionProduitService.deleteSuggestionLine(keys.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sanitize/{id}")
    public ResponseEntity<Void> sanitize(@PathVariable Integer id) {
        suggestionProduitService.sanitize(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fusionner")
    public ResponseEntity<Void> fusionner(@RequestBody Keys keys) {
        suggestionProduitService.fusionnerSuggestion(keys.ids());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add-item/{id}")
    public ResponseEntity<Void> addItem(@PathVariable Integer id, @RequestBody SuggestionLineDTO suggestionLine) {
        suggestionProduitService.addSuggestionLine(id, suggestionLine);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update-quantity")
    public ResponseEntity<Void> updateQuantity(@RequestBody SuggestionLineDTO suggestionLine) {
        suggestionProduitService.updateSuggestionLinQuantity(suggestionLine);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/csv/{id}", produces = "text/csv")
    public ResponseEntity<byte[]> exportToCsv(@PathVariable Integer id) {
        try {
            byte[] csvData = suggestionProduitService.exportToCsv(id);
            String filename = "suggestion_" + id + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss"))
                + ".csv";
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/commander/{id}")
    public ResponseEntity<Void> commander(@PathVariable Integer id) {
        suggestionProduitService.commander(id);
        return ResponseEntity.ok().build();
    }
}
