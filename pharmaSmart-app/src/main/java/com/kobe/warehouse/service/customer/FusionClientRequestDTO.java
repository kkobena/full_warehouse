package com.kobe.warehouse.service.customer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Fiches {@code sourceIds} à fondre dans la fiche {@code targetId}, qui est conservée. */
public record FusionClientRequestDTO(@NotNull Integer targetId, @NotEmpty List<Integer> sourceIds) {}
