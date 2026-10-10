package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.DemarqueRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.MargeRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.DemarquePilotageService;
import com.kobe.warehouse.service.pilotage.MargePilotageService;
import com.kobe.warehouse.service.pilotage.RemisesPilotageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Onglet « Rentabilité & remises » du pilotage : marge, remises, démarque. */
@RestController
@RequestMapping("/api/pilotage/rentabilite")
@RequiresNavAccess("pilotage.rentabilite-remises")
public class PilotageRentabiliteResource {

    private final MargePilotageService margePilotageService;
    private final RemisesPilotageService remisesPilotageService;
    private final DemarquePilotageService demarquePilotageService;

    public PilotageRentabiliteResource(
        MargePilotageService margePilotageService,
        RemisesPilotageService remisesPilotageService,
        DemarquePilotageService demarquePilotageService
    ) {
        this.margePilotageService = margePilotageService;
        this.remisesPilotageService = remisesPilotageService;
        this.demarquePilotageService = demarquePilotageService;
    }

    @GetMapping("/marge")
    public ResponseEntity<MargeRentabiliteDTO> analyserMarge(RequetePilotageDTO requete, @RequestParam(defaultValue = "FAMILLE") AxeAnalyse axe) {
        return ResponseEntity.ok(margePilotageService.analyserMarge(requete, axe));
    }

    @GetMapping("/remises")
    public ResponseEntity<RemisesRentabiliteDTO> analyserRemises(RequetePilotageDTO requete) {
        return ResponseEntity.ok(remisesPilotageService.analyserRemises(requete));
    }

    @GetMapping("/demarque")
    public ResponseEntity<DemarqueRentabiliteDTO> analyserDemarque(RequetePilotageDTO requete) {
        return ResponseEntity.ok(demarquePilotageService.analyserDemarque(requete));
    }
}
