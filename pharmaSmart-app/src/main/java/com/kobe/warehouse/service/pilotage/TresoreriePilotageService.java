package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.DifferesTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementsTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import java.time.LocalDate;

/** Onglet « Trésorerie & tiers payant » : mon argent rentre-t-il ? */
public interface TresoreriePilotageService {
    EncaissementsTresorerieDTO analyserEncaissements(RequetePilotageDTO requete);

    /** @param aujourdhui date à laquelle se lisent l'encours, le vieillissement et l'échéancier */
    TiersPayantTresorerieDTO analyserTiersPayant(RequetePilotageDTO requete, LocalDate aujourdhui);

    DifferesTresorerieDTO analyserDifferes(RequetePilotageDTO requete, LocalDate aujourdhui);
}
