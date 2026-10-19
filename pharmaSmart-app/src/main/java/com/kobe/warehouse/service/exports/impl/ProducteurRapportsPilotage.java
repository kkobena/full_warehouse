package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.ExportPilotageService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Éditions de l'écran Pilotage. Ses services ne rendent que les indicateurs et les axes que les droits de l'utilisateur courant
 * ouvrent : cette classe doit donc tourner sous l'identité du demandeur, ce dont {@link GenerateurExports} se charge pour un
 * export programmé — une demande du catalogue est déjà sur le fil de son auteur.
 */
@Component
class ProducteurRapportsPilotage {

    private final ExportPilotageService exportPilotageService;

    ProducteurRapportsPilotage(ExportPilotageService exportPilotageService) {
        this.exportPilotageService = exportPilotageService;
    }

    byte[] produire(RapportExport rapport, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return switch (rapport) {
            case PILOTAGE_TABLEAU_DE_BORD -> exportPilotageService.exporterSeries(ecrireRequetePilotage(du, au, filtres));
            case PILOTAGE_ANALYSER -> exportPilotageService.exporterAnalyse(
                ecrireRequetePilotage(du, au, filtres),
                ecrireRequeteAnalyse(filtres)
            );
            case PILOTAGE_ANNEES -> exportPilotageService.exporterAnnees(ecrireRequeteAnnees(filtres));
            default -> throw new IllegalArgumentException("Édition hors du pilotage : " + rapport);
        };
    }

    private static RequetePilotageDTO ecrireRequetePilotage(LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return new RequetePilotageDTO(
            du,
            au,
            ReglagesRapports.lireUnique(filtres, FiltreExport.COMPARAISON, TypeComparaison.class),
            null,
            null,
            null,
            null,
            ReglagesRapports.lireUnique(filtres, FiltreExport.GRANULARITE, Granularite.class),
            ReglagesRapports.lireListe(filtres, FiltreExport.INDICATEURS, IndicateurPilotage.class)
        );
    }

    /** Nombre d'éléments et tri comme à l'écran : sans choix, ses défauts — les vingt premiers, le reste en « autres ». */
    private static RequeteAnalyseDTO ecrireRequeteAnalyse(Map<FiltreExport, List<String>> filtres) {
        return new RequeteAnalyseDTO(
            ReglagesRapports.lireUnique(filtres, FiltreExport.AXE_ANALYSE, AxeAnalyse.class),
            null,
            ReglagesRapports.lireEntier(filtres, FiltreExport.NOMBRE_ELEMENTS),
            ReglagesRapports.lireUnique(filtres, FiltreExport.TRI_ANALYSE, TriAnalyse.class),
            null
        );
    }

    private static RequeteAnneesDTO ecrireRequeteAnnees(Map<FiltreExport, List<String>> filtres) {
        return new RequeteAnneesDTO(
            ReglagesRapports.lireUnique(filtres, FiltreExport.INDICATEUR_COMPARE, IndicateurPilotage.class),
            ReglagesRapports.lireEntier(filtres, FiltreExport.NOMBRE_ANNEES),
            ReglagesRapports.lireUnique(filtres, FiltreExport.MODE_ANNEES, ModeAnnees.class),
            null,
            null,
            null
        );
    }
}
