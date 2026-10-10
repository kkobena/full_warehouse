package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ExplicationEcartDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.VuePilotageDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.ExportPilotageService;
import com.kobe.warehouse.service.pilotage.VuesPilotageService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Onglet Analyser du pilotage : ventilations, explication des écarts, vues enregistrées. Une vue est personnelle : l'accès à
 * l'onglet suffit pour en enregistrer, le service vérifie qu'on n'en modifie que les siennes.
 */
@RestController
@RequestMapping("/api/pilotage")
@RequiresNavAccess("pilotage.analyser")
public class PilotageAnalyseResource {

    private final AnalysePilotageService analysePilotageService;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final VuesPilotageService vuesPilotageService;
    private final ExportPilotageService exportPilotageService;

    public PilotageAnalyseResource(
        AnalysePilotageService analysePilotageService,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        VuesPilotageService vuesPilotageService,
        ExportPilotageService exportPilotageService
    ) {
        this.analysePilotageService = analysePilotageService;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.vuesPilotageService = vuesPilotageService;
        this.exportPilotageService = exportPilotageService;
    }

    @GetMapping("/axes")
    public ResponseEntity<List<AxeAnalyseDTO>> listerAxes() {
        return ResponseEntity.ok(dictionnaireIndicateursService.listerAxesAutorises());
    }

    @GetMapping("/analyse")
    public ResponseEntity<AnalysePilotageDTO> analyser(RequetePilotageDTO requete, RequeteAnalyseDTO analyse) {
        return ResponseEntity.ok(analysePilotageService.analyser(requete, analyse));
    }

    @GetMapping("/analyse/export")
    @RequiresNavAccess(value = "pilotage.analyser", action = NavAction.EXPORT)
    public ResponseEntity<byte[]> exporterAnalyse(RequetePilotageDTO requete, RequeteAnalyseDTO analyse) {
        return Utils.exportCsv(exportPilotageService.exporterAnalyse(requete, analyse), "pilotage-analyse-" + requete.du() + "-" + requete.au());
    }

    @GetMapping("/analyse/explication")
    public ResponseEntity<ExplicationEcartDTO> expliquerEcart(RequetePilotageDTO requete, RequeteAnalyseDTO analyse) {
        return ResponseEntity.ok(analysePilotageService.expliquerEcart(requete, analyse));
    }

    @GetMapping("/vues")
    public ResponseEntity<List<VuePilotageDTO>> listerVues() {
        return ResponseEntity.ok(vuesPilotageService.listerVues());
    }

    @PostMapping("/vues")
    @RequiresNavAccess(value = "pilotage.analyser", action = NavAction.ACCESS)
    public ResponseEntity<VuePilotageDTO> enregistrerVue(@Valid @RequestBody VuePilotageDTO vue) {
        return ResponseEntity.ok(vuesPilotageService.enregistrerVue(vue));
    }

    @DeleteMapping("/vues/{id}")
    @RequiresNavAccess(value = "pilotage.analyser", action = NavAction.ACCESS)
    public ResponseEntity<Void> supprimerVue(@PathVariable Long id) {
        vuesPilotageService.supprimerVue(id);
        return ResponseEntity.noContent().build();
    }
}
