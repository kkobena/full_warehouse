package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.EcartsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;

/** Explication de l'écart de CA entre une période et sa référence. */
public interface EcartsPilotageService {
    /** {@code nombreContributions} : hausses et baisses rendues, de chaque côté. */
    EcartsPilotageDTO expliquerEcarts(RequetePilotageDTO requete, int nombreContributions);
}
