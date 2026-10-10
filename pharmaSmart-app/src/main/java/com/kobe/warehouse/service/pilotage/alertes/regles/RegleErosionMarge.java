package com.kobe.warehouse.service.pilotage.alertes.regles;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
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

/** Taux de marge du mois, à date, en baisse de plus du seuil (en points) sur le même mois N-1. */
@Component
@Order(3)
public class RegleErosionMarge implements RegleAlerte {

    private final SeriesPilotageService seriesPilotageService;
    private final AppConfigurationService appConfigurationService;

    public RegleErosionMarge(SeriesPilotageService seriesPilotageService, AppConfigurationService appConfigurationService) {
        this.seriesPilotageService = seriesPilotageService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.RENTABILITE_REMISES;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        List<SerieIndicateurDTO> series = seriesPilotageService
            .calculerSeries(RequetesAlertes.lireMoisADate(aujourdhui, List.of(IndicateurPilotage.TAUX_MARGE)))
            .series();
        if (series.isEmpty() || series.getFirst().ecart() == null || series.getFirst().ecart() >= -appConfigurationService.getAlerteErosionMargePilotage()) {
            return List.of();
        }
        SerieIndicateurDTO taux = series.getFirst();
        return List.of(
            new AlertePilotageDTO(
                "EROSION_MARGE",
                GraviteAlerte.HAUTE,
                "La marge s'érode",
                "Taux de marge du mois à date : " + RequetesAlertes.formaterDecimal(taux.valeur()) + " %, soit " +
                RequetesAlertes.formaterDecimal(-taux.ecart()) + " point(s) de moins que l'an dernier.",
                "rentabilite-remises"
            )
        );
    }
}
