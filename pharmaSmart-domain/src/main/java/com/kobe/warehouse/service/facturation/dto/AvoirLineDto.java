package com.kobe.warehouse.service.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AvoirLineDto(
    Long id,
    Long saleLineId,
    LocalDate saleLineDate,
    BigDecimal montantAvoir,
    String motifRejet
) {}
