package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record PrivillegesDTO(
    Object id,
    @NotNull String name,
    String libelle,
    boolean root,
    Integer parentId,
    boolean enable,
    Set<PrivillegesDTO> items,
    boolean isMenu
) {}
