package com.kobe.warehouse.service.dto.records;

/**
 * Une ligne d'inventaire présentant un écart (gap ≠ 0).
 *
 * @param lineId            identifiant de la StoreInventoryLine
 * @param produitLibelle    libellé du produit
 * @param quantityInit      stock théorique (avant inventaire)
 * @param quantityOnHand    stock compté
 * @param gap               écart = compté - théorique (négatif = manque)
 * @param valeurEcart       valeur financière de l'écart (|gap| * lastUnitPrice), en centimes
 * @param existingCause     cause déjà renseignée (null si non qualifiée)
 * @param existingComment   commentaire déjà renseigné
 */
public record GapLineRecord(
    Long lineId,
    String produitLibelle,
    Integer quantityInit,
    Integer quantityOnHand,
    Integer gap,
    Integer valeurEcart,
    String existingCause,
    String existingComment
) {}
