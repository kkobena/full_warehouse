package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.ClientsEquipeRapportDTO;

/** Onglet « Clients & équipe » du rapport PDF, sur la période de la barre. */
public interface RapportClientsEquipeService {
    ClientsEquipeRapportDTO construireClientsEquipe(RequetePilotageDTO requete);
}
