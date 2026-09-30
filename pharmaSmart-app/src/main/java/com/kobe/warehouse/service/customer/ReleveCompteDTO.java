package com.kobe.warehouse.service.customer;

import java.time.LocalDate;
import java.util.List;

/**
 * Relevé du compte client sur une période : ventes différées et règlements. Le solde final, pris à
 * aujourd'hui, est l'encours de la fiche.
 */
public record ReleveCompteDTO(
    ClientDocumentDTO client,
    LocalDate debut,
    LocalDate fin,
    long soldeInitial,
    List<LigneReleveDTO> lignes,
    long totalDebit,
    long totalCredit,
    long soldeFinal
) {}
