package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.AchatsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AchatsVentesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Sous-sections Achats et Achats / ventes de l'onglet « Achats & stock ». */
public interface AchatsPilotageService {
    AchatsPilotageDTO analyserAchats(RequetePilotageDTO requete);

    AchatsVentesPilotageDTO analyserAchatsVentes(RequetePilotageDTO requete);
}
