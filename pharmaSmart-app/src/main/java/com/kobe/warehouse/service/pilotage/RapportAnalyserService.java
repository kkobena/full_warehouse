package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.ReglageAnalyseRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.AnalyserRapportDTO;

/** Onglet Analyser du rapport PDF : la ventilation affichée à l'écran, avec son réglage et son chemin de descente. */
public interface RapportAnalyserService {
    AnalyserRapportDTO construireAnalyse(RequetePilotageDTO requete, ReglageAnalyseRapportDTO reglage);
}
