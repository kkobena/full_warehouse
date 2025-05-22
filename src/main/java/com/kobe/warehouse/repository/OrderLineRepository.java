package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelle;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatsSummary;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the OrderLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findByCommandeIdOrderByFournisseurProduitProduitLibelleAsc(Long commandeId);

    Optional<OrderLine> findFirstByFournisseurProduitIdAndCommandeId(Long fournisseurProduitId, Long commandeId);

    Page<OrderLine> findByCommandeId(Long commandeId, Pageable pageable);

    Long countByCommandeId(Long commandeId);

    int countByCommandeOrderStatusAndFournisseurProduitProduitId(OrderStatut orderStatut, Long produitId);

    boolean existsByFournisseurProduitProduitIdAndCommandeOrderStatus(Long produitId, OrderStatut orderStatus);

    int countByFournisseurProduitProduitIdAndCommandeOrderStatus(Long produitId, OrderStatut orderStatus);

    List<OrderLine> findAllByCommandeId(Long deliveryReceiptId);



    @Query(
        value = "SELECT MAX(o.updated_at) AS updatedAt FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id WHERE fp.produit_id = ?1 AND d.order_status=?2",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtByFournisseurProduitProduitId(Long produitId, String receiptStatut);

    @Query(
        value = "SELECT o.updated_at AS mvtDate,d.receipt_reference AS reference,o.quantity_received AS quantite,o.cost_amount AS prixAchat,u.first_name AS firstName,u.last_name AS lastName FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id JOIN user u ON d.user_id = u.id WHERE fp.produit_id =:produitId AND d.order_status=:statut AND DATE(o.updated_at) BETWEEN :startDate AND :endDate ORDER BY o.updated_at DESC ",
        nativeQuery = true
    )
    Page<HistoriqueProduitAchats> getHistoriqueAchat(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut,
        Pageable pageable
    );

    @Query(
        value = "SELECT YEAR(o.updated_at) AS annee,SUM(o.quantity_received) AS quantite,MONTH(o.updated_at) AS mois   FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id WHERE fp.produit_id =:produitId AND d.order_status=:statut AND DATE(o.updated_at) BETWEEN :startDate AND :endDate GROUP BY YEAR(o.updated_at) ,MONTH(o.updated_at) ORDER BY YEAR(o.updated_at) DESC",
        nativeQuery = true
    )
    List<HistoriqueProduitAchatMensuelle> getHistoriqueAchatMensuelle(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut
    );

    @Query(
        value = "SELECT SUM(o.quantity_received) AS quantite,SUM(o.cost_amount*o.quantity_received) AS montantAchat  FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id WHERE fp.produit_id =:produitId AND d.order_status=:statut AND DATE(o.updated_at) BETWEEN :startDate AND :endDate",
        nativeQuery = true
    )
    HistoriqueProduitAchatsSummary getHistoriqueAchatSummary(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut
    );
}
