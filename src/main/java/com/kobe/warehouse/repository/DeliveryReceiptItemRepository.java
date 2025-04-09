package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryReceiptItemRepository extends JpaRepository<DeliveryReceiptItem, Long> {
    List<DeliveryReceiptItem> findAllByDeliveryReceiptId(Long deliveryReceiptId);

    boolean existsByFournisseurProduitProduitIdAndDeliveryReceiptReceiptStatut(Long produitId, ReceiptStatut receiptStatut);
}
