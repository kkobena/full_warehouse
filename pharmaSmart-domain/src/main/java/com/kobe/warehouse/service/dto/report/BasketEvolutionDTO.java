package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record BasketEvolutionDTO(
    List<String> labels,
    List<BigDecimal> values,
    BigDecimal currentValue,
    BigDecimal previousValue,
    BigDecimal evolutionPct,
    BigDecimal evolutionAmount,
    String bestMonthLabel,
    BigDecimal bestMonthValue,
    BigDecimal trend6MPct
) {}
