package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReportPeriode(@NotNull LocalDate from, @NotNull LocalDate to) {}
