package com.kobe.warehouse.domain.enumeration;

/**
 * Laquelle des deux contraintes du fournisseur a décidé de la quantité commandée.
 *
 * <p>Savoir qui a tranché est une information de gestion, pas une étiquette : les deux contraintes
 * ne se corrigent pas de la même façon — un colisage se négocie avec le grossiste, un minimum de
 * commande s'absorbe en regroupant les lignes. La formulation destinée au pharmacien appartient à
 * l'affichage.
 */
public enum ContrainteColisage {
    /** La quantité a été portée au multiple supérieur du colis. */
    COLISAGE,
    /** La quantité a été portée au minimum de commande du fournisseur. */
    QUANTITE_MINIMALE,
}
