package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for cash movement in mobile activity report.
 *
 * @param libelle  Movement label/description
 * @param montant  Movement amount
 * @param type     Movement type (ENTREE or SORTIE)
 */
public record MouvementCaisseMobileDTO(
    String libelle,
    long montant,
    String type
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
