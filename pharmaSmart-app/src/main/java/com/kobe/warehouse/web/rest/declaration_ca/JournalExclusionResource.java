package com.kobe.warehouse.web.rest.declaration_ca;

import com.kobe.warehouse.aop.license.RequiresFeature;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.service.declaration_ca.JournalExclusionService;
import com.kobe.warehouse.service.declaration_ca.dto.JournalExclusionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalExclusionParamDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalLigneDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation des journaux d'exclusion du chiffre d'affaires.
 */
@RestController
@RequestMapping("/api/declaration-ca/journaux")
@PreAuthorize("hasAuthority('pr-declaration-ca-exclusion')")
public class JournalExclusionResource {

    private final JournalExclusionService journalExclusionService;

    public JournalExclusionResource(JournalExclusionService journalExclusionService) {
        this.journalExclusionService = journalExclusionService;
    }

    @GetMapping("/unites-gratuites")
    @RequiresFeature(Feature.EXCLUSION_UG)
    public ResponseEntity<JournalExclusionDTO> unitesGratuites(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) String recherche
    ) {
        return ResponseEntity.ok(
            journalExclusionService.unitesGratuites(new JournalExclusionParamDTO(dateDebut, dateFin, recherche, null))
        );
    }

    @GetMapping("/rayons")
    @RequiresFeature(Feature.EXCLUSION_RAYON)
    public ResponseEntity<JournalExclusionDTO> rayons(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) String recherche
    ) {
        return ResponseEntity.ok(
            journalExclusionService.rayonsExclus(new JournalExclusionParamDTO(dateDebut, dateFin, recherche, null))
        );
    }

    @GetMapping("/tiers-payants")
    @RequiresFeature(Feature.EXCLUSION_TP)
    public ResponseEntity<JournalExclusionDTO> tiersPayants(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) String recherche,
        @RequestParam(required = false) Integer tiersPayantId
    ) {
        return ResponseEntity.ok(
            journalExclusionService.ventesTiersPayant(new JournalExclusionParamDTO(dateDebut, dateFin, recherche, tiersPayantId))
        );
    }

    /**
     * Les lignes d'une vente tiers-payant exclue.
     *
     * <p>La date fait partie de la clé : {@code sales} est partitionnée par {@code sale_date}, et
     * l'identifiant seul obligerait à balayer toutes les partitions.
     */
    @GetMapping("/tiers-payants/lignes")
    @RequiresFeature(Feature.EXCLUSION_TP)
    public ResponseEntity<List<JournalLigneDTO>> lignesDeLaVente(
        @RequestParam Long saleId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate
    ) {
        return ResponseEntity.ok(journalExclusionService.lignesDeLaVente(saleId, saleDate));
    }
}
