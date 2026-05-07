package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data repository for the Commande entity.0
 */
@SuppressWarnings("unused")
@Repository
public interface CommandeRepository
    extends JpaRepository<Commande, CommandeId>, JpaSpecificationExecutor<Commande>, CustomCommandeRepository {
    @Query(
        "SELECT c FROM Commande c " +
        "JOIN FETCH c.fournisseur f " +
        "LEFT JOIN FETCH f.groupeFournisseur " +
        "WHERE c.orderStatus IN :statuts " +
        "AND c.paimentStatut != :paid " +
        "ORDER BY c.orderDate ASC"
    )
    List<Commande> findUnpaidCommandesAp(
        @Param("statuts") Set<OrderStatut> statuts,
        @Param("paid") PaimentStatut paid
    );

    @Query(
        "SELECT c FROM Commande c " +
        "JOIN FETCH c.fournisseur f " +
        "LEFT JOIN FETCH f.groupeFournisseur " +
        "WHERE c.orderStatus IN :statuts " +
        "AND c.paimentStatut != :paid " +
        "AND f.id = :fournisseurId " +
        "ORDER BY c.orderDate ASC"
    )
    List<Commande> findUnpaidCommandesApByFournisseur(
        @Param("statuts") Set<OrderStatut> statuts,
        @Param("paid") PaimentStatut paid,
        @Param("fournisseurId") Integer fournisseurId
    );

    @Query(
        "SELECT c FROM Commande c " +
        "JOIN FETCH c.fournisseur f " +
        "LEFT JOIN FETCH f.groupeFournisseur " +
        "WHERE c.orderStatus IN :statuts " +
        "AND c.paimentStatut != :paid " +
        "AND (:fromDate IS NULL OR c.receiptDate >= :fromDate) " +
        "AND (:toDate IS NULL OR c.receiptDate <= :toDate) " +
        "ORDER BY c.receiptDate ASC"
    )
    List<Commande> findUnpaidCommandesApByPeriod(
        @Param("statuts") Set<OrderStatut> statuts,
        @Param("paid") PaimentStatut paid,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    @Query(
        "SELECT c FROM Commande c " +
        "JOIN FETCH c.fournisseur f " +
        "LEFT JOIN FETCH f.groupeFournisseur " +
        "WHERE c.orderStatus IN :statuts " +
        "AND c.paimentStatut != :paid " +
        "AND f.id = :fournisseurId " +
        "AND (:fromDate IS NULL OR c.receiptDate >= :fromDate) " +
        "AND (:toDate IS NULL OR c.receiptDate <= :toDate) " +
        "ORDER BY c.receiptDate ASC"
    )
    List<Commande> findUnpaidCommandesApByFournisseurAndPeriod(
        @Param("statuts") Set<OrderStatut> statuts,
        @Param("paid") PaimentStatut paid,
        @Param("fournisseurId") Integer fournisseurId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    @Query(
        "SELECT f.id, f.libelle, SUM(c.finalAmount), " +
        "COALESCE(f.palierRfa, gf.palierRfa), " +
        "COALESCE(f.tauxRfa, gf.tauxRfa) " +
        "FROM Commande c " +
        "JOIN c.fournisseur f " +
        "LEFT JOIN f.groupeFournisseur gf " +
        "WHERE c.orderStatus = :statut AND c.receiptDate >= :start AND c.receiptDate < :end " +
        "GROUP BY f.id, f.libelle, f.palierRfa, gf.palierRfa, f.tauxRfa, gf.tauxRfa " +
        "ORDER BY SUM(c.finalAmount) DESC"
    )
    List<Object[]> sumCaByFournisseur(
        @Param("statut") OrderStatut statut,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    int countByOrderStatusAndType(OrderStatut orderStatut,TypeDeliveryReceipt type);

    long countByTypeAndOrderDate(TypeDeliveryReceipt type, LocalDate orderDate);

    default Specification<Commande> byReceiptType(TypeDeliveryReceipt type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    default Specification<Commande> byMotifBed(MotifBed motif) {
        return (root, query, cb) -> cb.equal(root.get("motifBed"), motif);
    }

    default Specification<Commande> byBedSearchRef(String searchRef) {
        if (!StringUtils.hasLength(searchRef)) {
            return null;
        }
        String like = searchRef.toUpperCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.upper(root.get(Commande_.receiptReference)), like),
            cb.like(cb.upper(root.get(Commande_.orderReference)), like)
        );
    }
    @Query(
        value = "select a.fournisseur.groupeFournisseur.libelle AS libelle,SUM(a.orderAmount)  AS montantTtc,SUM(a.htAmount)  AS montantHt,SUM(a.taxAmount)  AS montantTva from Commande a where a.orderDate  between :fromDate and :toDate AND a.orderStatus=:orderStatut GROUP BY a.fournisseur.groupeFournisseur.id,a.fournisseur.groupeFournisseur.libelle ",
        countQuery = "select count(a.fournisseur.groupeFournisseur.id) from Commande a where a.orderDate  between :fromDate and :toDate AND a.orderStatus=:receiptStatut "
    )
    Page<GroupeFournisseurAchat> fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("orderStatut") OrderStatut orderStatut,
        Pageable pageable
    );

    @Query(
        value = "select SUM(a.orderAmount)  AS montantTtc,SUM(a.htAmount)  AS montantHt,SUM(a.taxAmount)  AS montantTva from Commande a where a.orderDate  between :fromDate and :toDate AND a.orderStatus=:orderStatut"
    )
    ChiffreAffaireAchat fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("orderStatut") OrderStatut orderStatut
    );

    @Query(value = "SELECT tableau_pharmacien_commandes_report(:startDate, :endDate, :orderStatut)", nativeQuery = true)
    String fetchTableauPharmacienReport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("orderStatut") String statuts
    );

    @Query(value = "SELECT tableau_pharmacien_commandes_mois_report(:startDate, :endDate, :orderStatut)", nativeQuery = true)
    String fetchTableauPharmacienReportMensuel(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("orderStatut") String statuts
    );

    @Query(
        value = "SELECT c.id AS id, c.orderDate AS orderDate, c.receiptReference AS receiptReference, c.receiptDate AS receiptDate, c.orderAmount AS orderAmount,c.finalAmount AS receiptAmount, f.libelle AS fournisseurLibelle FROM Commande c JOIN c.fournisseur f WHERE LOWER(c.receiptReference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND c.orderDate>=:orderDateLimit AND c.orderStatus=:orderStatus ORDER BY c.orderDate DESC"
    )
    Slice<DeliveryReceiptProjection> fetchAllReceipts(
        @Param("searchTerm") String searchTerm,
        @Param("orderDateLimit") LocalDate orderDateLimit,
        @Param("orderStatus") OrderStatut orderStatut,
        Pageable pageable
    );

    Optional<Commande> findByOrderReference(String orderReference);

    /**
     * Calcule le taux de service et le délai moyen de livraison pour un fournisseur donné
     * sur une période glissante. Retourne un tableau Object[] à deux colonnes :
     * [0] taux_service DOUBLE (ratio qty_reçue / qty_commandée × 100),
     * [1] delai_moyen  DOUBLE (nb jours moyen entre order_date et updated_at).
     */
    @Query(value = """
        SELECT
            COALESCE(
                SUM(ol.quantity_received)::float / NULLIF(SUM(ol.quantity_requested), 0) * 100,
                0
            )                                                                           AS taux_service,
            COALESCE(
                AVG(EXTRACT(EPOCH FROM (c.updated_at - c.order_date::timestamp)) / 86400.0),
                0
            )                                                                           AS delai_moyen
        FROM commande c
             JOIN order_line ol ON ol.commande_id = c.id
                                AND ol.commande_order_date = c.order_date
        WHERE c.fournisseur_id = :fournisseurId
          AND c.order_status   = 'CLOSED'
          AND c.order_date     >= :fromDate
        """, nativeQuery = true)
    Object[] fetchStatsService(
        @Param("fournisseurId") Integer fournisseurId,
        @Param("fromDate") LocalDate fromDate
    );

    default Specification<Commande> hasOrderStatut(OrderStatut orderStatut) {
        return (root, query, cb) -> cb.equal(root.get(Commande_.orderStatus), orderStatut);
    }

    default Specification<Commande> between(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get(Commande_.orderDate), startDate, endDate);
    }

    default Specification<Commande> between(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(root.get(Commande_.updatedAt), startDate, endDate);
    }

    default Specification<Commande> dateReceiptBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get(Commande_.receiptDate), startDate, endDate);
    }

    default Specification<Commande> byStatut(EnumSet<OrderStatut> statuts) {
        return (root, query, cb) -> root.get(Commande_.orderStatus).in(statuts);
    }

    default Specification<Commande> byFournisseur(Integer fournisseurId) {
        return (root, query, cb) -> {
            Join<Commande, Fournisseur> fournisseurJoin = root.join(Commande_.fournisseur);
            return cb.equal(fournisseurJoin.get(Fournisseur_.id), fournisseurId);
        };
    }
    default Specification<Commande> byUser(Integer userId) {
        return (root, query, cb) -> {
            Join<Commande, AppUser> appUserJoin = root.join(Commande_.user);
            return cb.equal(appUserJoin.get(AppUser_.id), userId);
        };
    }

    default Specification<Commande> bySearchRef(String searchRef) {
        if (!StringUtils.hasLength(searchRef)) {
            return null;
        }
        String search = searchRef.toUpperCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.upper(root.get(Commande_.receiptReference)), search),
            cb.like(cb.upper(root.get(Commande_.orderReference)), search)
        );
    }

    /**
     * Compte les commandes fournisseurs dont l'échéance de paiement AP est dépassée.
     * Échéance = COALESCE(receipt_date, order_date) + COALESCE(f.jours_credit, gf.jours_credit, :defaultCreditDays).
     * Seules les commandes CLOSED et non PAID sont comptées.
     */
    @Query(
        value = """
            SELECT COUNT(*)
            FROM commande c
            LEFT JOIN fournisseur f       ON f.id  = c.fournisseur_id
            LEFT JOIN groupe_fournisseur gf ON gf.id = f.groupe_pournisseur_id
            WHERE c.order_status   = 'CLOSED'
              AND c.paiment_status != 'PAID'
              AND COALESCE(c.receipt_date, c.order_date)
                  + make_interval(days => COALESCE(f.jours_credit, gf.jours_credit, :defaultCreditDays))
                  < CURRENT_DATE
            """,
        nativeQuery = true
    )
    long countOverdueCommandesAp(@Param("defaultCreditDays") int defaultCreditDays);

    default Specification<Commande> bySearchTerm(String searchTerm) {
        if (!StringUtils.hasLength(searchTerm)) {
            return null;
        }
        String search = searchTerm.toUpperCase().toUpperCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Commande, OrderLine> linesJoin = root.join(Commande_.orderLines);
            Join<OrderLine, FournisseurProduit> fpJoin = linesJoin.join(OrderLine_.fournisseurProduit);
            Join<FournisseurProduit, Produit> produitJoin = fpJoin.join(FournisseurProduit_.produit);
            Join<Commande, Fournisseur> fournisseurJoin = root.join(Commande_.fournisseur);
            return cb.or(
                cb.like(cb.upper(root.get(Commande_.receiptReference)), search),
                cb.like(cb.upper(root.get(Commande_.orderReference)), search),
                cb.like(cb.upper(fpJoin.get(FournisseurProduit_.codeCip)), search),
                cb.like(cb.upper(fpJoin.get(FournisseurProduit_.codeEan)), search),
                cb.like(cb.upper(produitJoin.get(Produit_.codeEanLaboratoire)), search),
                cb.like(cb.upper(produitJoin.get(Produit_.libelle)), search),
                cb.like(cb.upper(fournisseurJoin.get(Fournisseur_.libelle)), search)
            );
        };
    }

    // ── Requête dashboard préparateur ─────────────────────────────────────────

    /**
     * Livraisons fournisseurs attendues aujourd'hui :
     * commandes mises à jour aujourd'hui (updated_at) avec order_status IN (:orderStatus).
     *
     * Le filtre sur order_date (30 derniers jours) est indispensable pour le partition pruning
     * PostgreSQL — sans lui, toutes les partitions seraient scannées.
     * Une commande passée il y a &lt;= 90 jours et mise à jour aujourd'hui couvre tous les cas réels.
     *
     * Retourne : [commande_id, fournisseur_nom, nombre_references]
     */
    @Query(
        value = """
            SELECT
                c.id          AS commande_id,
                f.libelle     AS fournisseur_nom,
                COUNT(DISTINCT ol.id) AS nombre_references
            FROM commande c
            INNER JOIN fournisseur f ON c.fournisseur_id = f.id
            LEFT JOIN order_line ol
                   ON ol.commande_id         = c.id
                  AND ol.commande_order_date = c.order_date
            WHERE c.order_date   >= CURRENT_DATE - INTERVAL '90 days'
              AND c.updated_at::date = CURRENT_DATE
              AND c.order_status    IN (:orderStatus)
            GROUP BY c.id, c.order_date, f.libelle
            ORDER BY c.id
            """,
        nativeQuery = true
    )
    List<Object[]> findLivraisonsAttenduesAujourdhui(@Param("orderStatus") Set<String> orderStatus);

}
