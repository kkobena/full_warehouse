package com.kobe.warehouse.service.pilotage.alertes.regles;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.alertes.RegleAlerte;
import com.kobe.warehouse.service.pilotage.calcul.RequetesAlertes;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** CA des 7 derniers jours sous un % des mêmes jours N-1. */
@Component
@Order(2)
public class RegleChuteActivite implements RegleAlerte {

    private static final int JOURS = 7;
    private static final double CENT = 100.0;

    private final SeriesPilotageService seriesPilotageService;
    private final AppConfigurationService appConfigurationService;

    public RegleChuteActivite(SeriesPilotageService seriesPilotageService, AppConfigurationService appConfigurationService) {
        this.seriesPilotageService = seriesPilotageService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.TABLEAU_DE_BORD;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        SerieIndicateurDTO ca = seriesPilotageService
            .calculerSeries(RequetesAlertes.lireDerniersJours(aujourdhui, JOURS, TypeComparaison.MEME_PERIODE_N_1, List.of(IndicateurPilotage.CA_TTC)))
            .series()
            .getFirst();
        if (ca.valeur() == null || ca.valeurReference() == null || ca.valeurReference() <= 0) {
            return List.of();
        }
        double pourcentage = ca.valeur() * CENT / ca.valeurReference();
        if (pourcentage >= appConfigurationService.getAlerteChuteActivitePilotage()) {
            return List.of();
        }
        return List.of(
            new AlertePilotageDTO(
                "CHUTE_ACTIVITE",
                GraviteAlerte.HAUTE,
                "Chute d'activité",
                "Le CA des 7 derniers jours atteint " + Math.round(pourcentage) + " % de celui des mêmes jours l'an dernier.",
                "tableau-de-bord"
            )
        );
    }
}
