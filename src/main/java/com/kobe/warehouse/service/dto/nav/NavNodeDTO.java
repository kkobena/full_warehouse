package com.kobe.warehouse.service.dto.nav;

import java.util.List;

public record NavNodeDTO(
    Integer id,
    String code,
    String libelle,
    String icon,
    String routerLink,
    int ordre,
    String badgeType,
    String targetType,
    List<NavNodeDTO> children,
    NavPermissionsDTO permissions
) {}

