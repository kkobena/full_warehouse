package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AuthorityDTO(@NotNull String name, String libelle, Set<String> privilleges) {}
