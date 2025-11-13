package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Spring Data repository for the Commande entity.0
 */
@SuppressWarnings("unused")
@Repository
public interface CommandeRepository extends JpaRepository<Commande, CommandeId>, JpaSpecificationExecutor<Commande>, CustomCommandeRepository {
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

    @Query(
        value = "SELECT tableau_pharmacien_commandes_report(:startDate, :endDate, :orderStatut)",
        nativeQuery = true
    )
    String fetchTableauPharmacienReport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("orderStatut") String statuts
    );

    @Query(
        value = "SELECT tableau_pharmacien_commandes_mois_report(:startDate, :endDate, :orderStatut)",
        nativeQuery = true
    )
    String fetchTableauPharmacienReportMensuel(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("orderStatut") String statuts
    );
    @Query(
        value = "SELECT c.id AS id, c.orderDate AS orderDate, c.receiptReference AS receiptReference, c.receiptDate AS receiptDate, c.orderAmount AS orderAmount,c.finalAmount AS receiptAmount, f.libelle AS fournisseurLibelle FROM Commande c JOIN c.fournisseur f WHERE LOWER(c.receiptReference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND c.orderDate>=:orderDateLimit  ORDER BY c.orderDate DESC"
    )
    Slice<DeliveryReceiptProjection> fetchAllReceipts(@Param("searchTerm") String searchTerm,@Param("orderDateLimit")  LocalDate orderDateLimit, Pageable pageable);

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
}
