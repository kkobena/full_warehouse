package com.kobe.warehouse.service.dto.ordonnance;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LigneVenteRefDTO(@NotNull Long salesLineId, @NotNull LocalDate salesLineDate) {}
