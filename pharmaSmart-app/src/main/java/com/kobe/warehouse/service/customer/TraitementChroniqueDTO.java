package com.kobe.warehouse.service.customer;

import java.time.LocalDate;

/**
 * Traitement chronique et son suivi, calculé à la lecture.
 *
 * @param derniereDelivrance  dernière vente clôturée d'un produit du traitement au patient
 * @param dernierProduitId    produit alors délivré : celui que « Re-délivrer » propose
 * @param prochaineDelivrance dernière délivrance plus la durée couverte
 * @param joursRestants       jours avant l'échéance ; négatif en cas de retard
 * @param renouvellementExceptionnelJusquau fin du renouvellement exceptionnel possible, si l'ordonnance a expiré
 * @param renouvellementExceptionnelMotif   pourquoi il ne l'est pas, le cas échéant
 */
public record TraitementChroniqueDTO(
    Integer id,
    Integer dciId,
    String dciLibelle,
    Integer produitId,
    String produitLibelle,
    String dosage,
    String posologie,
    int dureeJours,
    LocalDate dateOrdonnance,
    LocalDate dateFinOrdonnance,
    String note,
    boolean actif,
    LocalDate derniereDelivrance,
    Integer dernierProduitId,
    String dernierProduitLibelle,
    LocalDate prochaineDelivrance,
    Long joursRestants,
    Suivi suivi,
    Ordonnance ordonnance,
    LocalDate renouvellementExceptionnelJusquau,
    String renouvellementExceptionnelMotif
) {
    public enum Suivi {
        ARRETE,
        SANS_DELIVRANCE,
        A_JOUR,
        A_RENOUVELER,
        EN_RETARD,
        /** Plus d'un cycle de traitement manqué. */
        RUPTURE,
    }

    public enum Ordonnance {
        NON_RENSEIGNEE,
        VALIDE,
        EXPIREE,
    }
}
