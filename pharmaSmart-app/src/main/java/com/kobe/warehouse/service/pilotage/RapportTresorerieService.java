package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TresorerieRapportDTO;

/** Onglet « Trésorerie & tiers payant » du rapport PDF ; encours, DSO et échéancier sont à date, comme à l'écran. */
public interface RapportTresorerieService {
    TresorerieRapportDTO construireTresorerie(RequetePilotageDTO requete);
}
