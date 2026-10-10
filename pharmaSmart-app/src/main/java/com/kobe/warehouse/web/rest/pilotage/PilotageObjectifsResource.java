package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.GrilleObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.SaisieObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviObjectifsDTO;
import com.kobe.warehouse.service.pilotage.ObjectifsPilotageService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Onglet Objectifs du pilotage : lecture et suivi à l'accès, saisie et proposition au droit de modification. */
@RestController
@RequestMapping("/api/pilotage/objectifs")
@RequiresNavAccess("pilotage.objectifs")
public class PilotageObjectifsResource {

    private final ObjectifsPilotageService objectifsPilotageService;

    public PilotageObjectifsResource(ObjectifsPilotageService objectifsPilotageService) {
        this.objectifsPilotageService = objectifsPilotageService;
    }

    @GetMapping
    public ResponseEntity<GrilleObjectifsDTO> lireGrille(@RequestParam int annee) {
        return ResponseEntity.ok(objectifsPilotageService.lireGrille(annee, LocalDate.now()));
    }

    @PutMapping
    public ResponseEntity<GrilleObjectifsDTO> enregistrer(@Valid @RequestBody SaisieObjectifsDTO saisie) {
        return ResponseEntity.ok(objectifsPilotageService.enregistrer(saisie, LocalDate.now()));
    }

    @GetMapping("/proposition")
    @RequiresNavAccess(value = "pilotage.objectifs", action = NavAction.EDIT)
    public ResponseEntity<List<Double>> proposer(
        @RequestParam int annee,
        @RequestParam IndicateurPilotage indicateur,
        @RequestParam(defaultValue = "0") double hausse
    ) {
        return ResponseEntity.ok(objectifsPilotageService.proposer(annee, indicateur, hausse));
    }

    @GetMapping("/suivi")
    public ResponseEntity<SuiviObjectifsDTO> suivre(@RequestParam int annee) {
        return ResponseEntity.ok(objectifsPilotageService.suivre(annee, LocalDate.now()));
    }
}
