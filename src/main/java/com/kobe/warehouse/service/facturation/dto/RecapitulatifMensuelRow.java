package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecapitulatifMensuelRow(
    String numFacture,
    LocalDate invoiceDate,
    LocalDate echeance,
    BigDecimal montantNet,
    BigDecimal montantRegle,
    BigDecimal restantDu,
    InvoiceStatut statut
) {}
