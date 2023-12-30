package com.kobe.warehouse.service.dto;

import java.util.Set;
import jakarta.validation.constraints.NotNull;

public record AuthorityDTO(@NotNull String name, String libelle, Set<String> privilleges) {}
