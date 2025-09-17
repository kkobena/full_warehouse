package com.kobe.warehouse.domain.enumeration;

import java.util.Set;

/**
 * The SalesStatut enumeration.
 */
public enum SalesStatut {
    PROCESSING,
    PENDING,
    CLOSED,
    ACTIVE,
    DESABLED,
    CANCELED,
    REMOVED;

    public static Set<SalesStatut> getStatutForFacturation() {
        return Set.of(CLOSED, CANCELED);
    }

}
