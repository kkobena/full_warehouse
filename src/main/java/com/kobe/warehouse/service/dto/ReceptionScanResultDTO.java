package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.stock.DataMatrixParserService.BarcodeType;

/**
 * Résultat d'un scan CIP/DataMatrix pendant la saisie d'entrée en stock.
 *
 * @param found          true si la ligne commande a été identifiée et mise à jour
 * @param orderLineId    identifiant de la ligne mise à jour (null si non trouvée)
 * @param produitLibelle libellé du produit (retour UI)
 * @param produitCip     CIP identifié depuis le scan
 * @param lotAutoCreated true si un lot a été créé automatiquement (DataMatrix avec lot + péremption)
 * @param lotNumero      numéro de lot (auto-créé ou extrait du DataMatrix pour pré-remplissage)
 * @param lot            LotDTO complet (auto-créé si lotAutoCreated=true, ou pré-remplissage si lotAutoCreated=false)
 * @param warningMessage message d'avertissement (CIP absent de commande, substitution, doublon FMD, etc.)
 * @param barcodeType    type de code-barres détecté
 * @param serialNumber   numéro de série FMD (AI 21) — null si scan 1D ou absent du DataMatrix
 * @param fmdStatus      statut FMD : PRESENT, ABSENT, DUPLICATE
 */
public record ReceptionScanResultDTO(
    boolean found,
    Long orderLineId,
    String produitLibelle,
    String produitCip,
    boolean lotAutoCreated,
    String lotNumero,
    LotDTO lot,
    String warningMessage,
    BarcodeType barcodeType,
    String serialNumber,
    FmdStatus fmdStatus,
    int scannedQty
) {
    public enum FmdStatus {
        /** Numéro de série présent et unique — traçabilité FMD assurée. */
        PRESENT,
        /** Scan 1D ou DataMatrix sans numéro de série (AI 21 absent). */
        ABSENT,
        /** Numéro de série déjà enregistré pour ce produit — alerte contrefaçon. */
        DUPLICATE
    }
}
