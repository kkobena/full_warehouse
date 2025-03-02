package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record VenteModePaimentRecord(String code, String libelle, BigDecimal netAmount, BigDecimal paidAmount) {}
