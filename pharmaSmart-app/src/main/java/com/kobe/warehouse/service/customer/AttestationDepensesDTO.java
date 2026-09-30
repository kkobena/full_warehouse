package com.kobe.warehouse.service.customer;

import java.time.LocalDate;
import java.util.List;

/** Dépenses de santé du client sur une période, pour un remboursement par l'employeur ou la mutuelle. */
public record AttestationDepensesDTO(
    ClientDocumentDTO client,
    LocalDate debut,
    LocalDate fin,
    List<DepenseDTO> depenses,
    long total,
    long totalTiersPayant,
    long totalClient
) {}
