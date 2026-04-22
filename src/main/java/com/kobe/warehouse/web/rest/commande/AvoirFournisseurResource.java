package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFromBonLignesCommand;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import com.kobe.warehouse.service.stock.RetourBonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/avoirs-fournisseur")
public class AvoirFournisseurResource {

    private final AvoirFournisseurService avoirFournisseurService;
    private final RetourBonService retourBonService;

    public AvoirFournisseurResource(AvoirFournisseurService avoirFournisseurService, RetourBonService retourBonService) {
        this.avoirFournisseurService = avoirFournisseurService;
        this.retourBonService = retourBonService;
    }

    @PostMapping
    public ResponseEntity<AvoirFournisseurDTO> create(@RequestBody AvoirFournisseurCommand command) {
        return ResponseEntity.ok(avoirFournisseurService.create(command));
    }

    @PostMapping("/from-bon-lignes")
    public ResponseEntity<AvoirFournisseurDTO> createFromBonLignes(@RequestBody AvoirFromBonLignesCommand command) {
        return ResponseEntity.ok(retourBonService.createFromBonLignes(command));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<AvoirFournisseurDTO> annuler(
        @PathVariable Integer id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String motif = body != null ? body.get("motif") : null;
        return ResponseEntity.ok(avoirFournisseurService.annuler(id, motif));
    }

    @GetMapping
    public ResponseEntity<Page<AvoirFournisseurDTO>> getAll(
        @RequestParam(required = false) AvoirFournisseurStatut statut,
        @RequestParam(required = false) Integer fournisseurId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtStart,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtEnd,
        Pageable pageable
    ) {
        return ResponseEntity.ok(avoirFournisseurService.findAll(statut, fournisseurId, dtStart, dtEnd, pageable));
    }

    @GetMapping("/encours-par-fournisseur")
    public ResponseEntity<List<AvoirEncoursFournisseurDTO>> getEncoursParFournisseur() {
        return ResponseEntity.ok(avoirFournisseurService.getEncoursParFournisseur());
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<AvoirFournisseurDTO> updateStatut(
        @PathVariable Integer id,
        @RequestParam AvoirFournisseurStatut statut
    ) {
        return ResponseEntity.ok(avoirFournisseurService.updateStatut(id, statut));
    }
}
