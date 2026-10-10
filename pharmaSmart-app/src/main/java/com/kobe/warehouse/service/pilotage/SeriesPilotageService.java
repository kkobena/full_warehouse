package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;

/** Indicateurs du pilotage sur une période et sa référence, en total et par tranche. */
public interface SeriesPilotageService {
    SeriesPilotageDTO calculerSeries(RequetePilotageDTO requete);
}
