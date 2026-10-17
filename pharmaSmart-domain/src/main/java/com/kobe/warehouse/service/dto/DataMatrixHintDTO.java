package com.kobe.warehouse.service.dto;

import java.time.LocalDate;

/**
 * Données lues sur la boîte quand le scan n'a pas permis de retrouver le produit : elles
 * pré-remplissent l'écran de résolution manuelle au lieu d'être perdues.
 *
 * @param scannedCode  code produit retenu par le parser, sous sa forme normalisée
 * @param numLot       numéro de lot (AI 10), null si absent
 * @param expiryDate   date de péremption (AI 17), null si absente
 * @param serialNumber numéro de série (AI 21), null si absent
 * @param qty          quantité scannée (AI 37), 1 par défaut
 * @param proposition  true si le lot et la péremption sont déduits d'un code sans AI GS1 : ils
 *                     doivent alors être présentés comme proposés, et confirmés par un humain
 */
public record DataMatrixHintDTO(
    String scannedCode,
    String numLot,
    LocalDate expiryDate,
    String serialNumber,
    int qty,
    boolean proposition
) {
    public static DataMatrixHintDTO fromParsed(DataMatrixInfo parsed) {
        return new DataMatrixHintDTO(
            parsed.getProductCode(),
            parsed.batchNumber(),
            parsed.expiryDate(),
            parsed.serialNumber(),
            parsed.scannedQty(),
            parsed.proposition()
        );
    }
}
