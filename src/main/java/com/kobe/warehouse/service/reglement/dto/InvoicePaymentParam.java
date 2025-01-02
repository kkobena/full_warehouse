package com.kobe.warehouse.service.reglement.dto;

import java.time.LocalDate;

public record InvoicePaymentParam(String search, Long organismeId, LocalDate dateDebut, LocalDate dateFin, boolean grouped) {}
