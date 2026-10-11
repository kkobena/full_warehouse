package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.ComparerAnneesRapportDTO;

/** Onglet « Comparer les années » du rapport PDF, sur le réglage de l'écran. */
public interface RapportComparerAnneesService {
    /** @param libelleFiltre nom de la famille ou du type de vente filtré, {@code null} sans filtre */
    ComparerAnneesRapportDTO construireComparaison(RequeteAnneesDTO reglage, String libelleFiltre);
}
