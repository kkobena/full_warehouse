package com.kobe.warehouse.service.pilotage;

import java.time.LocalDate;

/** Jours ouvrés du pilotage : un jour ouvré est un jour où l'officine a vendu. */
public interface CalendrierPilotageService {
    long compterJoursOuvres(LocalDate du, LocalDate au);
}
