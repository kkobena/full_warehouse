package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record ProductStatQuantityRecord(int id, String codeCip, String codeEan, BigDecimal quantity) {}
