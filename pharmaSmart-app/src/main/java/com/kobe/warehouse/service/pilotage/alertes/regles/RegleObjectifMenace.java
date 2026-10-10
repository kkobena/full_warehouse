package com.kobe.warehouse.service.pilotage.alertes.regles;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviIndicateurDTO;
import com.kobe.warehouse.service.pilotage.ObjectifsPilotageService;
import com.kobe.warehouse.service.pilotage.alertes.RegleAlerte;
import com.kobe.warehouse.service.pilotage.calcul.RequetesAlertes;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Projection de fin de mois sous le seuil de l'objectif ; un plafond (taux de remise) menacé dès qu'il est dépassé. */
@Component
@Order(1)
public class RegleObjectifMenace implements RegleAlerte {

    private final ObjectifsPilotageService objectifsPilotageService;
    private final AppConfigurationService appConfigurationService;

    public RegleObjectifMenace(ObjectifsPilotageService objectifsPilotageService, AppConfigurationService appConfigurationService) {
        this.objectifsPilotageService = objectifsPilotageService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.OBJECTIFS;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        double seuil = appConfigurationService.getAlerteObjectifMenacePilotage();
        List<AlertePilotageDTO> alertes = new ArrayList<>();
        for (SuiviIndicateurDTO suivi : objectifsPilotageService.suivre(aujourdhui.getYear(), aujourdhui).indicateurs()) {
            ProjectionObjectifDTO projection = suivi.projection();
            if (projection == null || projection.objectif() == null || projection.tenu() == null) {
                continue;
            }
            boolean plafond = suivi.indicateur().sensFavorable() == SensFavorable.BAISSE;
            boolean menace = plafond ? !projection.tenu() : projection.atteinteProjetee() != null && projection.atteinteProjetee() < seuil;
            if (menace) {
                alertes.add(
                    new AlertePilotageDTO(
                        "OBJECTIF_MENACE_" + suivi.indicateur().code(),
                        GraviteAlerte.HAUTE,
                        "Objectif menacé : " + suivi.indicateur().libelle(),
                        plafond
                            ? "À date, " + RequetesAlertes.formaterDecimal(projection.projection()) + " % pour un plafond de " +
                              RequetesAlertes.formaterDecimal(projection.objectif()) + " %."
                            : "Fin de mois projetée à " + Math.round(projection.atteinteProjetee()) + " % de l'objectif.",
                        "objectifs"
                    )
                );
            }
        }
        return alertes;
    }
}
