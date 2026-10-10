package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.service.dto.pilotage.MargeRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Sous-section Marge de « Rentabilité & remises » : où se fait la marge, et pourquoi son taux bouge. */
public interface MargePilotageService {
    /** @param axe ventilation de la marge (famille, laboratoire, produit…) : un axe des lignes de vente */
    MargeRentabiliteDTO analyserMarge(RequetePilotageDTO requete, AxeAnalyse axe);
}
