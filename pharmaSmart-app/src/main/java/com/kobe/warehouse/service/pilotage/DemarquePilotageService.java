package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.DemarqueRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Sous-section Démarque de « Rentabilité & remises » : la valeur perdue, par motif, comparée à la référence. */
public interface DemarquePilotageService {
    DemarqueRentabiliteDTO analyserDemarque(RequetePilotageDTO requete);
}
