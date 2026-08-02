package com.kobe.warehouse.service.dto.records;

public record StoreInventoryLineRecord(
    int produitId,
    String produitCip,
    String produitEan,
    String produitLibelle,
    Long id,
    Integer gap,
    Integer quantityOnHand,
    int quantityInit,
    boolean updated,
    Integer prixAchat,
    Integer prixUni,
    Integer storageId,
    Integer seuilMini,
    int lotCount,
    String classePareto,
    /** Traçabilité : abréviation du compteur (null si la ligne n'a pas été comptée) */
    String countedBy,
    /** Traçabilité : date du dernier comptage */
    java.time.LocalDateTime updatedAt,
    /** Verrou optimiste : version lue, à renvoyer lors de l'écriture */
    Long version
) {

    /**
     * Constructeur de compatibilité pour le code existant (sans la traçabilité).
     */
    public StoreInventoryLineRecord(
        int produitId,
        String produitCip,
        String produitEan,
        String produitLibelle,
        Long id,
        Integer gap,
        Integer quantityOnHand,
        int quantityInit,
        boolean updated,
        Integer prixAchat,
        Integer prixUni,
        Integer storageId,
        Integer seuilMini,
        int lotCount,
        String classePareto
    ) {
        this(produitId, produitCip, produitEan, produitLibelle, id, gap, quantityOnHand,
            quantityInit, updated, prixAchat, prixUni, storageId, seuilMini, lotCount,
            classePareto, null, null, null);
    }
}
