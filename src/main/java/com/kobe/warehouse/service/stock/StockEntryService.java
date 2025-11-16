package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface StockEntryService {
    CommandeId finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite);

    DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite);

    DeliveryReceiptLiteDTO updateBon(DeliveryReceiptLiteDTO deliveryReceiptLite);

    CommandeResponseDTO importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt, MultipartFile multipartFile) throws IOException;

    void updateQuantityUG(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateQuantityReceived(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateOrderUnitPrice(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateTva(DeliveryReceiptItemLiteDTO deliveryReceiptItem);

    void updateDatePeremption(DeliveryReceiptItemLiteDTO deliveryReceiptItem);
}
