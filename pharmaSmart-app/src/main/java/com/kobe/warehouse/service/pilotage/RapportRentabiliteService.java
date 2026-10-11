package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.RentabiliteRapportDTO;

/** Onglet « Rentabilité & remises » du rapport PDF : marge, remises et démarque, sur la période de la barre. */
public interface RapportRentabiliteService {
    RentabiliteRapportDTO construireRentabilite(RequetePilotageDTO requete);
}
