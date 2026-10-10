package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.DifferesTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementsTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import com.kobe.warehouse.service.pilotage.TresoreriePilotageService;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Onglet « Trésorerie & tiers payant » du pilotage. */
@RestController
@RequestMapping("/api/pilotage/tresorerie")
@RequiresNavAccess("pilotage.tresorerie-tiers-payant")
public class PilotageTresorerieResource {

    private final TresoreriePilotageService tresoreriePilotageService;

    public PilotageTresorerieResource(TresoreriePilotageService tresoreriePilotageService) {
        this.tresoreriePilotageService = tresoreriePilotageService;
    }

    @GetMapping("/encaissements")
    public ResponseEntity<EncaissementsTresorerieDTO> analyserEncaissements(RequetePilotageDTO requete) {
        return ResponseEntity.ok(tresoreriePilotageService.analyserEncaissements(requete));
    }

    @GetMapping("/tiers-payant")
    public ResponseEntity<TiersPayantTresorerieDTO> analyserTiersPayant(RequetePilotageDTO requete) {
        return ResponseEntity.ok(tresoreriePilotageService.analyserTiersPayant(requete, LocalDate.now()));
    }

    @GetMapping("/differes")
    public ResponseEntity<DifferesTresorerieDTO> analyserDifferes(RequetePilotageDTO requete) {
        return ResponseEntity.ok(tresoreriePilotageService.analyserDifferes(requete, LocalDate.now()));
    }
}
