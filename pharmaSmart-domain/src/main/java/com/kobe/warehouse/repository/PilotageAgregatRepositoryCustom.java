package com.kobe.warehouse.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Mise à jour des agrégats du pilotage par les fonctions SQL de la migration V2.1.34. */
public interface PilotageAgregatRepositoryCustom {
    /** Remplace les agrégats de ventes, d'achats et d'encaissements des jours de la période. */
    void recalculer(LocalDate du, LocalDate au);

    /** Photographie le stock courant, rangé au dernier jour du mois de {@code jour}. */
    void photographierStock(LocalDate jour);

    /** Jours dont une vente, une réception ou un encaissement a été créé ou modifié depuis {@code depuis}, du plus ancien au plus récent. */
    List<LocalDate> listerJoursModifiesDepuis(LocalDateTime depuis);
}
