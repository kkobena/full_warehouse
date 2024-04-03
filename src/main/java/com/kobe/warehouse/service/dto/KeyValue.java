package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;

public record KeyValue(@NotNull Long key, @NotNull Long value) {}
