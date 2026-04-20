package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/avoirs-fournisseur")
public class AvoirFournisseurResource {

    private final AvoirFournisseurService avoirFournisseurService;

    public AvoirFournisseurResource(AvoirFournisseurService avoirFournisseurService) {
        this.avoirFournisseurService = avoirFournisseurService;
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
