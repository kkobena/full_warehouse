package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LigneRapprochementDto(
    Long factureId,
    String numFacture,
    LocalDate invoiceDate,
    LocalDate echeance,
    BigDecimal montantFacture,
    BigDecimal montantRegle,
    BigDecimal ecart,
    InvoiceStatut statut,
    List<ReglementDto> reglements
) {}
