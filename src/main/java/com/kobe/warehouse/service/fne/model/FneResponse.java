package com.kobe.warehouse.service.fne.model;

import jakarta.validation.constraints.NotNull;

public record FneResponse(@NotNull String reference, @NotNull String token) {


}
