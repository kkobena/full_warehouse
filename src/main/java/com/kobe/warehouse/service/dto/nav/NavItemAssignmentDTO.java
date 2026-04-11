package com.kobe.warehouse.service.dto.nav;

public record NavItemAssignmentDTO(
    Integer navItemId,
    boolean canDisplay,
    boolean canAccess,
    boolean canCreate,
    boolean canEdit,
    boolean canDelete,
    boolean canExport,
    boolean canExecute
) {}

