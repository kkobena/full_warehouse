package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.ClientsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.EquipePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Onglet « Clients & équipe » : qui vient, qui vend ? (la fréquentation se lit sur l'analyse heure × jour). */
public interface ClientsEquipePilotageService {
    ClientsPilotageDTO analyserClients(RequetePilotageDTO requete);

    EquipePilotageDTO analyserEquipe(RequetePilotageDTO requete);
}
