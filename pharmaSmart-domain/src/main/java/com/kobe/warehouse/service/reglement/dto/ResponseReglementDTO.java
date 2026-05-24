package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.PaymentId;

public record ResponseReglementDTO(PaymentId id, boolean total) {}
