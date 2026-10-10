package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Exports CSV des tableaux du pilotage, valeurs complètes (non abrégées) pour être retravaillées dans un tableur. */
public interface ExportPilotageService {
    /** Détail par période du tableau de bord : une ligne par tranche, la plus récente d'abord. */
    byte[] exporterSeries(RequetePilotageDTO requete);

    /** Ventilation de l'onglet Analyser, autres et total compris. */
    byte[] exporterAnalyse(RequetePilotageDTO requete, RequeteAnalyseDTO analyse);

    /** Mois × années de l'onglet « Comparer les années ». */
    byte[] exporterAnnees(RequeteAnneesDTO requete);
}
