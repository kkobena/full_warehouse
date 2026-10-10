package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.AchatsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AchatsVentesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RupturesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.StockPilotageDTO;
import com.kobe.warehouse.service.pilotage.AchatsPilotageService;
import com.kobe.warehouse.service.pilotage.StockPilotageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Onglet « Achats & stock » du pilotage. */
@RestController
@RequestMapping("/api/pilotage/achats-stock")
@RequiresNavAccess("pilotage.achats-stock")
public class PilotageAchatsStockResource {

    private final AchatsPilotageService achatsPilotageService;
    private final StockPilotageService stockPilotageService;

    public PilotageAchatsStockResource(AchatsPilotageService achatsPilotageService, StockPilotageService stockPilotageService) {
        this.achatsPilotageService = achatsPilotageService;
        this.stockPilotageService = stockPilotageService;
    }

    @GetMapping("/achats")
    public ResponseEntity<AchatsPilotageDTO> analyserAchats(RequetePilotageDTO requete) {
        return ResponseEntity.ok(achatsPilotageService.analyserAchats(requete));
    }

    @GetMapping("/achats-ventes")
    public ResponseEntity<AchatsVentesPilotageDTO> analyserAchatsVentes(RequetePilotageDTO requete) {
        return ResponseEntity.ok(achatsPilotageService.analyserAchatsVentes(requete));
    }

    @GetMapping("/stock")
    public ResponseEntity<StockPilotageDTO> analyserStock(RequetePilotageDTO requete) {
        return ResponseEntity.ok(stockPilotageService.analyserStock(requete));
    }

    @GetMapping("/ruptures")
    public ResponseEntity<RupturesPilotageDTO> analyserRuptures(RequetePilotageDTO requete) {
        return ResponseEntity.ok(stockPilotageService.analyserRuptures(requete));
    }
}
