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

/** Ratio ventes / achats (TTC) des 30 derniers jours sous le seuil : on achète plus qu'on ne vend. */
@Component
@Order(6)
public class RegleAchatsQuiDerapent implements RegleAlerte {

    private static final int JOURS = 30;

    private final SeriesPilotageService seriesPilotageService;
    private final AppConfigurationService appConfigurationService;

    public RegleAchatsQuiDerapent(SeriesPilotageService seriesPilotageService, AppConfigurationService appConfigurationService) {
        this.seriesPilotageService = seriesPilotageService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.ACHATS_STOCK;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        Double ventes = null;
        Double achats = null;
        for (SerieIndicateurDTO serie : seriesPilotageService
            .calculerSeries(RequetesAlertes.lireDerniersJours(aujourdhui, JOURS, TypeComparaison.AUCUNE, List.of(IndicateurPilotage.CA_TTC, IndicateurPilotage.ACHATS_TTC)))
            .series()) {
            if (IndicateurPilotage.CA_TTC.name().equals(serie.indicateur().code())) {
                ventes = serie.valeur();
            } else {
                achats = serie.valeur();
            }
        }
        if (ventes == null || achats == null || achats <= 0) {
            return List.of();
        }
        double ratio = ventes / achats;
        if (ratio >= appConfigurationService.getAlerteRatioAchatsPilotage()) {
            return List.of();
        }
        return List.of(
            new AlertePilotageDTO(
                "ACHATS_DERAPENT",
                GraviteAlerte.MOYENNE,
                "Les achats dérapent",
                "Sur 30 jours, les ventes ne représentent que " + RequetesAlertes.formaterDecimal(ratio) + " fois les achats : le stock gonfle.",
                "achats-stock"
            )
        );
    }
}
