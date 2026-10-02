package com.kobe.warehouse.service.referentiel;

import com.kobe.warehouse.domain.ProduitRefSpecialite;
import com.kobe.warehouse.domain.enumeration.StatutRapprochement;

/** Une ligne de la file de relecture : ce que le rapprochement n'a pas pu trancher seul. */
public record RapprochementDTO(
    Integer produitId,
    String produitLibelle,
    String cis,
    String specialiteLibelle,
    StatutRapprochement statut,
    int score,
    String motif
) {
    public static RapprochementDTO creerDepuis(ProduitRefSpecialite r) {
        return new RapprochementDTO(
            r.getProduit().getId(),
            r.getProduit().getLibelle(),
            r.getSpecialite() == null ? null : r.getSpecialite().getCis(),
            r.getSpecialite() == null ? null : r.getSpecialite().getLibelle(),
            r.getStatut(),
            r.getScore(),
            r.getMotif()
        );
    }
}
