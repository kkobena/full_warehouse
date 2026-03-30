package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatsSummary;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the OrderLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, OrderLineId> {
    Optional<OrderLine> findFirstByFournisseurProduitIdAndCommandeIdAndCommandeOrderDate(
        Integer fournisseurProduitId,
        Integer commandeId,
        LocalDate orderDate
    );

    Page<OrderLine> findByCommandeIdAndCommandeOrderDate(Integer commandeId, LocalDate orderDate, Pageable pageable);

    int countByCommandeIdAndCommandeOrderDate(Integer commandeId, LocalDate orderDate);

    boolean existsByFournisseurProduitProduitIdAndCommandeOrderStatusAndCommandeOrderDateGreaterThan(
        Integer produitId,
        OrderStatut orderStatus,
        LocalDate periodeBegin
    );

    int countByFournisseurProduitProduitIdAndCommandeOrderStatusAndCommandeOrderDateGreaterThan(
        Integer produitId,
        OrderStatut orderStatus,
        LocalDate periodeBegin
    );

    List<OrderLine> findAllByCommandeIdAndCommandeOrderDate(Integer deliveryReceiptId, LocalDate orderDate);

    @Query(
        "SELECT o.fournisseurProduit.produit.libelle AS produitLibelle,o.fournisseurProduit.codeCip AS produitCip,o.id AS id,o.orderDate AS orderDate,o.quantityReceived AS quantityReceived,o.quantityRequested AS quantityRequested,o.freeQty AS freeQty,o.fournisseurProduit.produit.id AS produitId,l AS lots FROM OrderLine o LEFT JOIN o.lots l WHERE o.commande.id = ?1 AND o.commande.orderDate = ?2 ORDER BY  o.fournisseurProduit.produit.libelle ASC"
    )
    List<DeliveryReceiptItemProjection> findDetailAllByCommandeIdAndCommandeOrderDate(Integer commandeId, LocalDate orderDate);

    @Query(
        value = "SELECT MAX(o.updated_at) AS updatedAt FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id WHERE fp.produit_id = ?1 AND d.order_status=?2",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtByFournisseurProduitProduitId(Integer produitId, String receiptStatut);

    @Query(
        value = "SELECT o.updated_at AS mvtDate,d.receipt_reference AS reference,o.quantity_received AS quantite,o.order_cost_amount AS prixAchat,u.first_name AS firstName,u.last_name AS lastName FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id AND o.commande_order_date=d.order_date  JOIN app_user u ON d.user_id = u.id WHERE fp.produit_id =:produitId AND d.order_status=:statut AND d.order_date BETWEEN :startDate AND :endDate ORDER BY o.updated_at  ",
        nativeQuery = true,
        countQuery = "SELECT COUNT(o.id) as orderCount  FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id AND o.commande_order_date=d.order_date  JOIN app_user u ON d.user_id = u.id WHERE fp.produit_id =:produitId AND d.order_status=:statut AND d.order_date BETWEEN :startDate AND :endDate ORDER BY o.updated_at  "
    )
    Page<HistoriqueProduitAchats> getHistoriqueAchat(
        @Param("produitId") Integer produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut,
        Pageable pageable
    );

    // List<HistoriqueProduitAchatMensuelle>
    @Query(value = "SELECT get_product_order_summary_monthly(:startDate, :endDate, :statut,:produitId)", nativeQuery = true)
    String getHistoriqueAchatMensuelle(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut,
        @Param("produitId") Integer produitId
    );

    @Query(
        value = "SELECT coalesce(SUM(o.quantity_received),0) AS quantite,coalesce(SUM(o.order_cost_amount*o.quantity_received),0) AS montantAchat  FROM order_line o JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id JOIN commande d ON o.commande_id = d.id WHERE fp.produit_id =:produitId AND d.order_status=:statut AND o.order_date BETWEEN :startDate AND :endDate AND o.commande_order_date = d.order_date",
        nativeQuery = true
    )
    HistoriqueProduitAchatsSummary getHistoriqueAchatSummary(
        @Param("produitId") Integer produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statut") String statut
    );

    int countByCommande(Commande commande);

    /**
     * Recherche la première ligne d'une commande dont le CIP fournisseur correspond au code scanné.
     * Utilisé pour la réception par scan CIP/DataMatrix.
     */
    @Query("SELECT o FROM OrderLine o WHERE o.commande.id = :commandeId AND o.fournisseurProduit.codeCip = :codeCip")
    Optional<OrderLine> findFirstByCommandeIdAndCip(
        @Param("commandeId") Integer commandeId,
        @Param("codeCip") String codeCip
    );

    /**
     * Retourne les quantités en attente de livraison par produit (stock virtuel — Axe 1).
     * <p>
     * Sélectionne les lignes de commandes dont le statut est REQUESTED (envoyée au fournisseur,
     * non encore réceptionnée). La quantité en attente = quantité commandée - quantité déjà reçue.
     * </p>
     * <p>
     * Ce résultat est utilisé dans le calcul des suggestions de réapprovisionnement pour éviter
     * de commander à nouveau des produits déjà en cours de livraison.
     * </p>
     *
     * @param produitIds IDs des produits à interroger (batch)
     * @return Tableau de [produit_id, qty_en_attente] — seuls les produits avec qty > 0 sont retournés
     */
    @Query(value = """
        SELECT fp.produit_id                                                            AS produit_id,
               SUM(ol.quantity_requested - COALESCE(ol.quantity_received, 0))           AS qty_en_attente
        FROM order_line ol
             JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
             JOIN commande c ON c.id = ol.commande_id
                             AND c.order_date = ol.commande_order_date
        WHERE fp.produit_id IN :produitIds
          AND c.order_status = 'REQUESTED'
          AND ol.quantity_requested > COALESCE(ol.quantity_received, 0)
        GROUP BY fp.produit_id
        HAVING SUM(ol.quantity_requested - COALESCE(ol.quantity_received, 0)) > 0
        """, nativeQuery = true)
    List<Object[]> findPendingQtyByProduitIds(@Param("produitIds") Collection<Integer> produitIds);
}
