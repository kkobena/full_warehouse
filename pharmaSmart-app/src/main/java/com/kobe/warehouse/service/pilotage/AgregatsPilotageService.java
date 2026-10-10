package com.kobe.warehouse.service.pilotage;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Mise à jour des agrégats journaliers et de la photographie mensuelle du stock du pilotage. */
public interface AgregatsPilotageService {
    /** Reconstruit les agrégats d'une période (reprise de données, correction). */
    void recalculer(LocalDate du, LocalDate au);

    /** Recalcule chaque jour dont une vente, une réception ou un encaissement a changé depuis {@code depuis} ; rend leur nombre. */
    int actualiserJoursModifiesDepuis(LocalDateTime depuis);

    /** Photographie le stock courant dans le mois de {@code jour} : la dernière prise du mois fait le stock de fin de mois. */
    void photographierStock(LocalDate jour);
}
