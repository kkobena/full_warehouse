package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.AchatsStockRapportDTO;

/** Onglet « Achats & stock » du rapport PDF, sur la période de la barre. */
public interface RapportAchatsStockService {
    AchatsStockRapportDTO construireAchatsStock(RequetePilotageDTO requete);
}
