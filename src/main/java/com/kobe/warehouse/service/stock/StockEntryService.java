package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.PriceHistoryDTO;
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
}
