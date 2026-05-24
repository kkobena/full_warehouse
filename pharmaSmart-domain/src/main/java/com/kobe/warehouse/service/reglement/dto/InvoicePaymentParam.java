package com.kobe.warehouse.service.reglement.dto;

import java.time.LocalDate;

public record InvoicePaymentParam(String search, Integer organismeId, LocalDate dateDebut, LocalDate dateFin, boolean grouped) {}
