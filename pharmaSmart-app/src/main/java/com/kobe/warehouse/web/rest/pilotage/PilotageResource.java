package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO;
import com.kobe.warehouse.service.dto.pilotage.EcartsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.pilotage.AgregatsPilotageService;
import com.kobe.warehouse.service.pilotage.AlertesPilotageService;
import com.kobe.warehouse.service.pilotage.ChiffreAffairesReferenceService;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.EcartsPilotageService;
import com.kobe.warehouse.service.pilotage.ExportPilotageService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.web.rest.Utils;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pilotage de l'officine : dictionnaire des indicateurs, CA de référence, recalcul des agrégats. */
@RestController
@RequestMapping("/api/pilotage")
@RequiresNavAccess("pilotage")
public class PilotageResource {

    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final ChiffreAffairesReferenceService chiffreAffairesReferenceService;
    private final AgregatsPilotageService agregatsPilotageService;
    private final SeriesPilotageService seriesPilotageService;
    private final EcartsPilotageService ecartsPilotageService;
    private final ExportPilotageService exportPilotageService;
    private final AlertesPilotageService alertesPilotageService;

    public PilotageResource(
        DictionnaireIndicateursService dictionnaireIndicateursService,
        ChiffreAffairesReferenceService chiffreAffairesReferenceService,
        AgregatsPilotageService agregatsPilotageService,
        SeriesPilotageService seriesPilotageService,
        EcartsPilotageService ecartsPilotageService,
        ExportPilotageService exportPilotageService,
        AlertesPilotageService alertesPilotageService
    ) {
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.chiffreAffairesReferenceService = chiffreAffairesReferenceService;
        this.agregatsPilotageService = agregatsPilotageService;
        this.seriesPilotageService = seriesPilotageService;
        this.ecartsPilotageService = ecartsPilotageService;
        this.exportPilotageService = exportPilotageService;
        this.alertesPilotageService = alertesPilotageService;
    }

    @GetMapping("/series")
    public ResponseEntity<SeriesPilotageDTO> calculerSeries(RequetePilotageDTO requete) {
        return ResponseEntity.ok(seriesPilotageService.calculerSeries(requete));
    }

    @GetMapping("/series/export")
    @RequiresNavAccess(value = "pilotage.tableau-de-bord", action = NavAction.EXPORT)
    public ResponseEntity<byte[]> exporterSeries(RequetePilotageDTO requete) {
        return Utils.exportCsv(exportPilotageService.exporterSeries(requete), "pilotage-" + requete.du() + "-" + requete.au());
    }

    @GetMapping("/ecarts")
    public ResponseEntity<EcartsPilotageDTO> expliquerEcarts(RequetePilotageDTO requete, @RequestParam(defaultValue = "5") int contributions) {
        return ResponseEntity.ok(ecartsPilotageService.expliquerEcarts(requete, contributions));
    }

    @GetMapping("/alertes")
    @RequiresNavAccess("pilotage.tableau-de-bord")
    public ResponseEntity<List<AlertePilotageDTO>> listerAlertes() {
        return ResponseEntity.ok(alertesPilotageService.listerAlertes(LocalDate.now()));
    }

    @GetMapping("/indicateurs")
    public ResponseEntity<List<IndicateurPilotageDTO>> listerIndicateurs() {
        return ResponseEntity.ok(dictionnaireIndicateursService.listerIndicateursAutorises());
    }

    @GetMapping("/chiffre-affaires")
    public ResponseEntity<ChiffreAffairesReferenceDTO> calculerChiffreAffaires(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
    ) {
        return ResponseEntity.ok(chiffreAffairesReferenceService.calculerChiffreAffaires(du, au));
    }

    /** Reconstruction d'une période (reprise de données, correction) : droit d'exécution, réservé à l'administrateur par défaut. */
    @PostMapping("/agregats/recalcul")
    @RequiresNavAccess(value = "pilotage", action = NavAction.EXECUTE)
    public ResponseEntity<Void> recalculerAgregats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
    ) {
        agregatsPilotageService.recalculer(du, au);
        return ResponseEntity.noContent().build();
    }
}
