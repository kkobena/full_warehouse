package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.domain.SaleId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTiersPayantTauxInfo(
    @NotNull SaleId saleId,
    @NotNull Integer clientTiersPayantId,
    @Min(0) @Max(100) int taux
) {}
