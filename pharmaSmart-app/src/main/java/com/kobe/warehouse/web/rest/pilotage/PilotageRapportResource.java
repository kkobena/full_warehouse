package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.RequeteRapportPilotageDTO;
import com.kobe.warehouse.service.pilotage.RapportPilotageService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rapport PDF du pilotage. L'intercepteur exige l'export d'au moins un onglet ; le service vérifie ensuite chaque onglet
 * demandé et refuse tout le rapport s'il en manque un.
 */
@RestController
@RequestMapping("/api/pilotage")
public class PilotageRapportResource {

    private final RapportPilotageService rapportPilotageService;

    public PilotageRapportResource(RapportPilotageService rapportPilotageService) {
        this.rapportPilotageService = rapportPilotageService;
    }

    @PostMapping("/rapport/pdf")
    @RequiresNavAccess(
        value = {
            "pilotage.tableau-de-bord",
            "pilotage.analyser",
            "pilotage.comparer-annees",
            "pilotage.rentabilite-remises",
            "pilotage.achats-stock",
            "pilotage.tresorerie-tiers-payant",
            "pilotage.clients-equipe",
        },
        action = NavAction.EXPORT
    )
    public ResponseEntity<byte[]> imprimerRapport(@Valid @RequestBody RequeteRapportPilotageDTO demande) {
        var requete = demande.requete();
        return Utils.printPDF(rapportPilotageService.imprimerRapport(demande), "pilotage-" + requete.du() + "-" + requete.au() + ".pdf");
    }
}
