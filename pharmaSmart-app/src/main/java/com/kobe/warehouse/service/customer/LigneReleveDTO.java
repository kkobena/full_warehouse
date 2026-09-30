package com.kobe.warehouse.service.customer;

import java.time.LocalDate;

/** Un mouvement du relevé : vente différée au débit, règlement au crédit, et le solde qui en résulte. */
public record LigneReleveDTO(LocalDate date, String libelle, String reference, long debit, long credit, long solde) {}
