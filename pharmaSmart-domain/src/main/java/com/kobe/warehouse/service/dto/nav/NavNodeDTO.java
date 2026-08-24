package com.kobe.warehouse.service.dto.nav;

import java.util.List;

public record NavNodeDTO(
    Integer id,
    String code,
    String libelle,
    String icon,
    /** Titre de la barre d'outils quand il diffère du libellé ; nul le plus souvent. */
    String titreLong,
    String routerLink,
    int ordre,
    String badgeType,
    String targetType,
    List<NavNodeDTO> children,
    NavPermissionsDTO permissions
) {}

