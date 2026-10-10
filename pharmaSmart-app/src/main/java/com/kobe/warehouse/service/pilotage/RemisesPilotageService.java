package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Sous-section Remises de « Rentabilité & remises » : combien, comment accordées, à quelle tranche, par qui. */
public interface RemisesPilotageService {
    RemisesRentabiliteDTO analyserRemises(RequetePilotageDTO requete);
}
