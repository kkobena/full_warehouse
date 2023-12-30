package com.kobe.warehouse.service.dto;

import java.util.Set;
import jakarta.validation.constraints.NotNull;

public record PrivillegesDTO(
    int id,
    @NotNull String name,
    String libelle,
    boolean root,
    Integer parentId,
    boolean enable,
    Set<PrivillegesDTO> items) {}
