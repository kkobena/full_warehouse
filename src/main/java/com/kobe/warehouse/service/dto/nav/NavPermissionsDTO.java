package com.kobe.warehouse.service.dto.nav;

public record NavPermissionsDTO(
    boolean canDisplay,
    boolean canAccess,
    boolean canCreate,
    boolean canEdit,
    boolean canDelete,
    boolean canExport,
    boolean canExecute
) {

    public static NavPermissionsDTO fullAccess() {
        return new NavPermissionsDTO(true, true, true, true, true, true, true);
    }

    public static NavPermissionsDTO readOnly() {
        return new NavPermissionsDTO(true, true, false, false, false, false, false);
    }

    public static NavPermissionsDTO noAccess() {
        return new NavPermissionsDTO(false, false, false, false, false, false, false);
    }

    /** Fusionne (OR logique) deux permissions — union des droits. */
    public NavPermissionsDTO merge(NavPermissionsDTO other) {
        if (other == null) return this;
        return new NavPermissionsDTO(
            this.canDisplay || other.canDisplay,
            this.canAccess  || other.canAccess,
            this.canCreate  || other.canCreate,
            this.canEdit    || other.canEdit,
            this.canDelete  || other.canDelete,
            this.canExport  || other.canExport,
            this.canExecute || other.canExecute
        );
    }
}

