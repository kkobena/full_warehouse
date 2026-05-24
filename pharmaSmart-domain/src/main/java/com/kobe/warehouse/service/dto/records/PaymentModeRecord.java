package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.domain.enumeration.PaymentGroup;

public record PaymentModeRecord(String code, String libelle, PaymentGroup group) {}
