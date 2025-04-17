package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryReceiptItemRepository extends JpaRepository<DeliveryReceiptItem, Long> {
    List<DeliveryReceiptItem> findAllByDeliveryReceiptId(Long deliveryReceiptId);

    boolean existsByFournisseurProduitProduitIdAndDeliveryReceiptReceiptStatut(Long produitId, ReceiptStatut receiptStatut);

    @Query(
        value = "SELECT MAX(o.updated_date) AS updatedAt FROM delivery_receipt_item o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN delivery_receipt d ON o.delivery_receipt_id = d.id WHERE fp.produit_id = ?1 AND d.receipt_status=?2",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtByFournisseurProduitProduitId(Long produitId, String receiptStatut);
}
