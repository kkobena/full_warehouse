package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ExplicationEcartDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Onglet Analyser : un à trois indicateurs ventilés par un axe, croisés par un second, sur le chemin de descente. */
public interface AnalysePilotageService {
    AnalysePilotageDTO analyser(RequetePilotageDTO requete, RequeteAnalyseDTO analyse);

    /**
     * Pour chaque axe encore libre, la part de l'écart du premier indicateur additif (à défaut, le CA TTC) que concentrent ses
     * trois principaux éléments ; le premier axe rendu est le plus explicatif.
     */
    ExplicationEcartDTO expliquerEcart(RequetePilotageDTO requete, RequeteAnalyseDTO analyse);
}
