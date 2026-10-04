package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.security.navaccess.NavAccessExempt;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.stock.SubstitutionComptoirService;
import com.kobe.warehouse.service.stock.dto.SubstitutPropose;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Équivalents disponibles d'un produit, pour la vente (docs/PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §4). À
 * part de {@code /produits/{id}/generiques}, qui liste tous les substituts du catalogue, en stock ou non.
 */
@RestController
@RequestMapping("/api")
@RequiresNavAccess("catalogue")
public class SubstitutionComptoirResource {

    private final SubstitutionComptoirService substitutionComptoirService;

    public SubstitutionComptoirResource(SubstitutionComptoirService substitutionComptoirService) {
        this.substitutionComptoirService = substitutionComptoirService;
    }

    @GetMapping("/produits/{id}/substituts-disponibles")
    @NavAccessExempt("lecture du catalogue, nécessaire à la vente et aux commandes pour tous les rôles")
    public ResponseEntity<List<SubstitutPropose>> lireSubstitutsDisponibles(@PathVariable Integer id) {
        return ResponseEntity.ok(substitutionComptoirService.lireSubstitutsDisponibles(id));
    }
}
