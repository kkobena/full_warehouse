package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDate;

public record ReglementFactureCommand(
    Long factureId,
    LocalDate factureDate,
    Integer montantRegle,
    LocalDate dateReglement,
    String transactionNumber,
    String paymentModeCode,
    Long banqueId,
    String commentaire
) {}
