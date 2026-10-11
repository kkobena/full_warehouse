package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauDeBordRapportDTO;

/** Onglet Tableau de bord du rapport PDF : les mêmes calculs que l'écran, mis en forme pour l'impression. */
public interface RapportTableauDeBordService {
    TableauDeBordRapportDTO construireTableauDeBord(RequetePilotageDTO requete);
}
