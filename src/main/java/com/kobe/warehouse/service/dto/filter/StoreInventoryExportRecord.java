package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.service.dto.enumeration.StoreInventoryExportGroupBy;

public record StoreInventoryExportRecord(
    StoreInventoryExportGroupBy exportGroupBy,
    StoreInventoryLineFilterRecord filterRecord,
    Boolean gestionLot
) {
    /** Retourne true si l'inventaire utilise la gestion de lot. */
    public boolean isGestionLot() {
        return Boolean.TRUE.equals(gestionLot);
    }
}
