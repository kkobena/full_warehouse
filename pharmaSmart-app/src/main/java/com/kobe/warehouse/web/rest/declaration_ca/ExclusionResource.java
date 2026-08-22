package com.kobe.warehouse.web.rest.declaration_ca;

import com.kobe.warehouse.aop.license.RequiresFeature;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.service.declaration_ca.ExclusionReferentielService;
import com.kobe.warehouse.service.declaration_ca.dto.ExclusionItemDTO;
import com.kobe.warehouse.service.declaration_ca.dto.ExclusionRequestDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Référentiels d'exclusion du chiffre d'affaires à déclarer.
 *
 * <p>Deux verrous indépendants, et il en faut deux. {@link RequiresFeature}, posé par méthode, dit
 * ce que l'officine a <strong>acheté</strong> — les trois exclusions se vendent séparément.
 * {@code pr-declaration-ca-exclusion} dit qui, parmi ses employés, a le droit d'y toucher. Une
 * licence ne remplace pas une habilitation.
 */
@RestController
@RequestMapping("/api/declaration-ca")
@PreAuthorize("hasAuthority('pr-declaration-ca-exclusion')")
public class ExclusionResource {

    private final ExclusionReferentielService exclusionReferentielService;
    private final AppConfigurationService appConfigurationService;

    public ExclusionResource(
        ExclusionReferentielService exclusionReferentielService,
        AppConfigurationService appConfigurationService
    ) {
        this.exclusionReferentielService = exclusionReferentielService;
        this.appConfigurationService = appConfigurationService;
    }

    @GetMapping("/rayons")
    @RequiresFeature(Feature.EXCLUSION_RAYON)
    public ResponseEntity<List<ExclusionItemDTO>> listerRayons(
        @RequestParam(value = "exclus", required = false) Boolean exclus
    ) {
        return ResponseEntity.ok(exclusionReferentielService.listerRayons(exclus));
    }

    /**
     * Positionne l'état d'exclusion de plusieurs rayons.
     *
     * <p>{@code PUT} et non {@code POST} : la requête porte l'état cible, pas une bascule. Rejouée,
     * elle donne le même résultat — un double-clic ou une reprise réseau ne peut pas inverser
     * silencieusement une sélection.
     */
    @PutMapping("/rayons/exclusion")
    @RequiresFeature(Feature.EXCLUSION_RAYON)
    public ResponseEntity<Map<String, Integer>> majExclusionRayons(@Valid @RequestBody ExclusionRequestDTO requete) {
        int modifies = exclusionReferentielService.majExclusionRayons(requete.ids(), requete.exclure());
        return ResponseEntity.ok(Map.of("modifies", modifies));
    }

    @GetMapping("/tiers-payants")
    @RequiresFeature(Feature.EXCLUSION_TP)
    public ResponseEntity<List<ExclusionItemDTO>> listerTiersPayants(
        @RequestParam(value = "exclus", required = false) Boolean exclus
    ) {
        return ResponseEntity.ok(exclusionReferentielService.listerTiersPayants(exclus));
    }

    @PutMapping("/tiers-payants/exclusion")
    @RequiresFeature(Feature.EXCLUSION_TP)
    public ResponseEntity<Map<String, Integer>> majExclusionTiersPayants(@Valid @RequestBody ExclusionRequestDTO requete) {
        int modifies = exclusionReferentielService.majExclusionTiersPayants(requete.ids(), requete.exclure());
        return ResponseEntity.ok(Map.of("modifies", modifies));
    }

    @GetMapping("/parametres")
    @RequiresFeature(Feature.EXCLUSION_UG)
    public ResponseEntity<Map<String, Boolean>> lireParametres() {
        return ResponseEntity.ok(Map.of("excludeFreeUnit", appConfigurationService.excludeFreeUnit()));
    }

    @PutMapping("/parametres")
    @RequiresFeature(Feature.EXCLUSION_UG)
    public ResponseEntity<Map<String, Boolean>> majParametres(@RequestBody Map<String, Boolean> parametres) {
        boolean exclure = Boolean.TRUE.equals(parametres.get("excludeFreeUnit"));
        appConfigurationService.setExcludeFreeUnit(exclure);
        return ResponseEntity.ok(Map.of("excludeFreeUnit", exclure));
    }
}
