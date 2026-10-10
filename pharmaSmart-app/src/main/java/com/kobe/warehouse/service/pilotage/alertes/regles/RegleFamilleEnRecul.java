package com.kobe.warehouse.service.pilotage.alertes.regles;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.alertes.RegleAlerte;
import com.kobe.warehouse.service.pilotage.calcul.RequetesAlertes;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Familles dont le CA du mois, à date, recule au-delà du seuil ; les familles marginales (moins de 2 % du CA N-1) sont ignorées. */
@Component
@Order(4)
public class RegleFamilleEnRecul implements RegleAlerte {

    private static final double PART_MINIMALE = 0.02;
    private static final int FAMILLES_CITEES = 3;

    private final AnalysePilotageService analysePilotageService;
    private final AppConfigurationService appConfigurationService;

    public RegleFamilleEnRecul(AnalysePilotageService analysePilotageService, AppConfigurationService appConfigurationService) {
        this.analysePilotageService = analysePilotageService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.TABLEAU_DE_BORD;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        AnalysePilotageDTO analyse = analysePilotageService.analyser(
            RequetesAlertes.lireMoisADate(aujourdhui, List.of(IndicateurPilotage.CA_TTC)),
            new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, null, 0, TriAnalyse.ECART_BAISSE, null)
        );
        Double referenceTotale = analyse.total().getFirst().valeurReference();
        if (referenceTotale == null || referenceTotale <= 0) {
            return List.of();
        }
        double seuil = -appConfigurationService.getAlerteFamilleReculPilotage();
        List<String> familles = new ArrayList<>();
        int nombre = 0;
        for (ElementAnalyseDTO famille : analyse.elements()) {
            CelluleAnalyseDTO ca = famille.cellules().getFirst();
            boolean significative = ca.valeurReference() != null && ca.valeurReference() >= PART_MINIMALE * referenceTotale;
            if (significative && ca.ecartPct() != null && ca.ecartPct() < seuil) {
                nombre++;
                if (familles.size() < FAMILLES_CITEES) {
                    familles.add(famille.libelle() + " (" + Math.round(ca.ecartPct()) + " %)");
                }
            }
        }
        if (nombre == 0) {
            return List.of();
        }
        String autres = nombre > familles.size() ? " et " + (nombre - familles.size()) + " autre(s)" : "";
        return List.of(
            new AlertePilotageDTO(
                "FAMILLE_EN_RECUL",
                GraviteAlerte.MOYENNE,
                nombre == 1 ? "Une famille en recul" : nombre + " familles en recul",
                "Sur le mois à date, face au même mois l'an dernier : " + String.join(", ", familles) + autres + ".",
                "analyser"
            )
        );
    }
}
