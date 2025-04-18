package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query(
        value = "SELECT o.updated_date AS mvtDate,d.receipt_reference AS reference,o.quantity_received AS quantite,o.cost_amount AS prixAchat,u.first_name AS firstName,u.last_name AS lastName FROM delivery_receipt_item o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN delivery_receipt d ON o.delivery_receipt_id = d.id JOIN user u ON d.modified_user_id = u.id WHERE fp.produit_id =:produitId AND d.receipt_status=:statut AND DATE(o.updated_date) BETWEEN :startDate AND :endDate ORDER BY o.updated_date DESC ",
        nativeQuery = true
    )
    Page<HistoriqueProduitAchats> getHistoriqueAchat(@Param("produitId") long produitId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("statut") String statut, Pageable pageable);

    @Query(
        value = "SELECT YEAR(o.updated_date) AS annee,SUM(o.quantity_received) AS quantite,MONTH(o.updated_date) AS mois   FROM delivery_receipt_item o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN delivery_receipt d ON o.delivery_receipt_id = d.id WHERE fp.produit_id =:produitId AND d.receipt_status=:statut AND DATE(o.updated_date) BETWEEN :startDate AND :endDate GROUP BY YEAR(o.updated_date) ,MONTH(o.updated_date) ORDER BY YEAR(o.updated_date) DESC",
        nativeQuery = true
    )
    List<HistoriqueProduitAchatMensuelle> getHistoriqueAchatMensuelle(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut
        );
    @Query(value = "SELECT SUM(o.quantity_received) AS quantite,SUM(o.cost_amount*o.quantity_received) AS montantAchat  FROM delivery_receipt_item o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN delivery_receipt d ON o.delivery_receipt_id = d.id WHERE fp.produit_id =:produitId AND d.receipt_status=:statut AND DATE(o.updated_date) BETWEEN :startDate AND :endDate",
        nativeQuery = true
    )
    HistoriqueProduitAchatsSummary getHistoriqueAchatSummary(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut
    );

}
