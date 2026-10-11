package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequeteRapportPilotageDTO;

/** Rapport PDF du pilotage : un chapitre par onglet demandé, graphes compris. */
public interface RapportPilotageService {
    /** Refuse tout le rapport dès qu'un onglet demandé n'est pas exportable par l'utilisateur. */
    byte[] imprimerRapport(RequeteRapportPilotageDTO requete);
}
