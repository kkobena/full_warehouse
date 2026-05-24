package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFromBonLignesCommand;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import com.kobe.warehouse.service.stock.RetourBonService;
import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @PostMapping("/from-reception")
    public ResponseEntity<AvoirFournisseurDTO> createFromReception(@RequestBody AvoirFromBonLignesCommand command) {
        return ResponseEntity.ok(retourBonService.createFromReception(command));
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
    public ResponseEntity<List<AvoirFournisseurDTO>> getAll(
        @RequestParam(required = false) AvoirFournisseurStatut statut,
        @RequestParam(required = false) String reference,
        @RequestParam(required = false) Integer fournisseurId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtStart,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtEnd,
        Pageable pageable
    ) {
        Page<AvoirFournisseurDTO> page = avoirFournisseurService.findAll(reference, statut, fournisseurId, dtStart, dtEnd, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/encours-par-fournisseur")
    public ResponseEntity<List<AvoirEncoursFournisseurDTO>> getEncoursParFournisseur() {
        return ResponseEntity.ok(avoirFournisseurService.getEncoursParFournisseur());
    }

    @GetMapping("/count-en-attente")
    public ResponseEntity<Long> countEnAttente() {
        return ResponseEntity.ok(avoirFournisseurService.countEnAttente());
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<AvoirFournisseurDTO> updateStatut(
        @PathVariable Integer id,
        @RequestParam AvoirFournisseurStatut statut
    ) {
        return ResponseEntity.ok(avoirFournisseurService.updateStatut(id, statut));
    }
}
