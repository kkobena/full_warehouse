package com.kobe.warehouse.service.pilotage.alertes.regles;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.pilotage.TresoreriePilotageService;
import com.kobe.warehouse.service.pilotage.alertes.RegleAlerte;
import com.kobe.warehouse.service.pilotage.calcul.RequetesAlertes;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Organismes dont l'encours dépasse le seuil en jours de chiffre (DSO), au sens du vieillissement des créances. */
@Component
@Order(7)
public class RegleCreancesOrganisme implements RegleAlerte {

    private static final int ORGANISMES_CITES = 3;

    private final TresoreriePilotageService tresoreriePilotageService;
    private final AppConfigurationService appConfigurationService;

    public RegleCreancesOrganisme(TresoreriePilotageService tresoreriePilotageService, AppConfigurationService appConfigurationService) {
        this.tresoreriePilotageService = tresoreriePilotageService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.TRESORERIE_TIERS_PAYANT;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        double seuil = appConfigurationService.getAlerteDsoOrganismePilotage();
        List<String> organismes = new ArrayList<>();
        int nombre = 0;
        for (OrganismeTresorerieDTO organisme : tresoreriePilotageService.analyserTiersPayant(RequetesAlertes.lireMoisADate(aujourdhui, List.of()), aujourdhui).organismes()) {
            if (organisme.encours() > 0 && organisme.dso() != null && organisme.dso() > seuil) {
                nombre++;
                if (organismes.size() < ORGANISMES_CITES) {
                    organismes.add(organisme.libelle() + " (" + organisme.dso() + " j)");
                }
            }
        }
        if (nombre == 0) {
            return List.of();
        }
        String autres = nombre > organismes.size() ? " et " + (nombre - organismes.size()) + " autre(s)" : "";
        return List.of(
            new AlertePilotageDTO(
                "CREANCES",
                GraviteAlerte.MOYENNE,
                nombre == 1 ? "Un organisme tarde à payer" : nombre + " organismes tardent à payer",
                "Encours au-delà de " + Math.round(seuil) + " jours de chiffre : " + String.join(", ", organismes) + autres + ".",
                "tresorerie-tiers-payant"
            )
        );
    }
}
