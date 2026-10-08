package com.kobe.warehouse.service.dto.ordonnance;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PrescripteurVenteDTO(@NotNull Long salesId, @NotNull LocalDate salesDate, @NotNull Integer prescripteurId) {}
