package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.PriceHistoryDTO;
import com.kobe.warehouse.service.dto.PutawayPreviewItemDTO;
import com.kobe.warehouse.service.dto.ReceptionScanResultDTO;
import com.kobe.warehouse.service.dto.StockEntryResultDTO;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface StockEntryService {
    StockEntryResultDTO finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite);

    DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite);

    DeliveryReceiptLiteDTO updateBon(DeliveryReceiptLiteDTO deliveryReceiptLite);

    CommandeResponseDTO importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt, MultipartFile multipartFile) throws IOException;

    void updateQuantityUG(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateQuantityReceived(DeliveryReceiptItemLiteDTO deliveryReceiptItem);
    void batchUpdateQuantityReceived(List<DeliveryReceiptItemLiteDTO> deliveryReceiptItems);

    void updateOrderUnitPrice(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateTva(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateDatePeremption(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    /**
     * Retourne les lignes de commande dont la variation de prix d'achat dépasse le seuil
     * configuré dans APP_SEUIL_VARIATION_PRIX (défaut 20%).
     * Opère sur TOUTES les lignes (sans pagination) pour ne pas dépendre de la vue courante.
     */
    List<OrderLineDTO> findLignesAvecEcartPrix(Integer commandeId, LocalDate orderDate);

    /**
     * Retourne les 24 dernières entrées d'historique de prix pour un produit fournisseur donné,
     * triées par date décroissante.
     */
    List<PriceHistoryDTO> getPriceHistory(Integer fournisseurProduitId);

    /**
     * Calcule la prévisualisation de répartition rayon → réserve pour une commande donnée.
     * Retourne uniquement les produits dont le stock rayon dépasse {@code stockMaxi} ET
     * qui ont un stock réserve configuré.
     * Utilisé par le frontend (mode MANUAL) pour afficher le modal de confirmation.
     *
     * @param commandeId identifiant de la commande (BL en cours de réception)
     * @return liste des produits concernés avec les quantités à déplacer
     */
    List<PutawayPreviewItemDTO> getPutawayPreview(Integer commandeId,LocalDate orderDate);

    /**
     * Traite un scan CIP ou DataMatrix reçu pendant la saisie d'une entrée en stock.
     * <ul>
     *   <li>Identifie la ligne de commande correspondant au code scanné.</li>
     *   <li>Incrémente la quantité reçue de 1.</li>
     *   <li>Crée automatiquement un lot si le DataMatrix contient numéro de lot + date de
     *       péremption ET que {@code APP_GESTION_LOT} est actif.</li>
     * </ul>
     *
     * @param commandeId identifiant de la commande (BL en cours)
     * @param rawScan    chaîne brute retournée par la douchette (1D ou 2D)
     * @return résultat du traitement avec feedback pour l'UI
     */
    ReceptionScanResultDTO processScanReception(Integer commandeId, String rawScan);
}
