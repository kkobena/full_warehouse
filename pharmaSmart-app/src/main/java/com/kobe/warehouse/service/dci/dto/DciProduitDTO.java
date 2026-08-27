package com.kobe.warehouse.service.dci.dto;

/**
 * Produit rattaché à une substance active, tel que présenté dans le détail d'une DCI.
 *
 * <p>Projection volontairement étroite : l'écran ne sert qu'à répondre à « quels produits portent
 * cette molécule ? ». Renvoyer le DTO produit complet chargerait des dizaines de champs inutiles
 * pour chaque ligne.
 *
 * <p>Le code CIP provient du <em>fournisseur principal</em> du produit : il n'existe pas sur
 * {@code produit} mais sur {@code fournisseur_produit}, et un produit peut être référencé chez
 * plusieurs grossistes avec des codes distincts.
 */
public record DciProduitDTO(
    Integer id,
    String libelle,
    String codeCip,
    String famille,
    Integer prixVente,
    String statut
) {}
