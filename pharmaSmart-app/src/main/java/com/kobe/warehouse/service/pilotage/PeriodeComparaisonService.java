package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import java.time.LocalDate;

/** Période analysée et période de référence d'une requête du pilotage. */
public interface PeriodeComparaisonService {
    /** {@code aujourdhui} arrête une période en cours quand la requête est « à date ». */
    ComparaisonPeriodesDTO comparer(RequetePilotageDTO requete, LocalDate aujourdhui);
}
