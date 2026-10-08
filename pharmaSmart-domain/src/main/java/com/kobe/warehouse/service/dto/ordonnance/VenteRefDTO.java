package com.kobe.warehouse.service.dto.ordonnance;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VenteRefDTO(@NotNull Long salesId, @NotNull LocalDate salesDate) {}
