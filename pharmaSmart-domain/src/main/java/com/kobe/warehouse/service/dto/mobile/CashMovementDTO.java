package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDateTime;

/**
 * DTO for cash movement in cash balance report.
 */
public record CashMovementDTO(
    long id,
    String libelle,
    long montant,
    String type,  // ENTREE | SORTIE
    LocalDateTime date,
    String comment
) {
    public static final String TYPE_ENTREE = "ENTREE";
    public static final String TYPE_SORTIE = "SORTIE";

    public boolean isEntree() {
        return TYPE_ENTREE.equals(type);
    }

    public boolean isSortie() {
        return TYPE_SORTIE.equals(type);
    }
}
