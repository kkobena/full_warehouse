package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.pilotage.ComparaisonAnneesService;
import com.kobe.warehouse.service.pilotage.ExportPilotageService;
import com.kobe.warehouse.web.rest.Utils;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Onglet « Comparer les années » du pilotage. */
@RestController
@RequestMapping("/api/pilotage")
@RequiresNavAccess("pilotage.comparer-annees")
public class PilotageAnneesResource {

    private final ComparaisonAnneesService comparaisonAnneesService;
    private final ExportPilotageService exportPilotageService;

    public PilotageAnneesResource(ComparaisonAnneesService comparaisonAnneesService, ExportPilotageService exportPilotageService) {
        this.comparaisonAnneesService = comparaisonAnneesService;
        this.exportPilotageService = exportPilotageService;
    }

    @GetMapping("/annees")
    public ResponseEntity<ComparaisonAnneesDTO> comparerAnnees(RequeteAnneesDTO requete) {
        return ResponseEntity.ok(comparaisonAnneesService.comparerAnnees(requete, LocalDate.now()));
    }

    @GetMapping("/annees/export")
    @RequiresNavAccess(value = "pilotage.comparer-annees", action = NavAction.EXPORT)
    public ResponseEntity<byte[]> exporterAnnees(RequeteAnneesDTO requete) {
        return Utils.exportCsv(exportPilotageService.exporterAnnees(requete), "pilotage-annees-" + requete.indicateur().name().toLowerCase());
    }
}
