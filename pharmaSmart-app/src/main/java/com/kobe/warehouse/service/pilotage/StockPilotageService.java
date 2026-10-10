package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RupturesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.StockPilotageDTO;

/** Sous-sections Stock et Ruptures & péremptions de l'onglet « Achats & stock ». */
public interface StockPilotageService {
    StockPilotageDTO analyserStock(RequetePilotageDTO requete);

    RupturesPilotageDTO analyserRuptures(RequetePilotageDTO requete);
}
