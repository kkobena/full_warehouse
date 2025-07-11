package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Commande entity.0
 */
@SuppressWarnings("unused")
@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    @Query(
        value = "select a.fournisseur.groupeFournisseur.libelle AS libelle,SUM(a.orderAmount)  AS montantTtc,SUM(a.htAmount)  AS montantHt,SUM(a.taxAmount)  AS montantTva from Commande a where FUNCTION('DATE',a.createdAt)  between :fromDate and :toDate AND a.orderStatus=:orderStatut GROUP BY a.fournisseur.groupeFournisseur.id ",
        countQuery = "select count(a.fournisseur.groupeFournisseur.id) from Commande a where FUNCTION('DATE',a.createdAt)  between :fromDate and :toDate AND a.orderStatus=:receiptStatut "
    )
    Page<GroupeFournisseurAchat> fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("orderStatut") OrderStatut orderStatut,
        Pageable pageable
    );

    @Query(
        value = "select SUM(a.orderAmount)  AS montantTtc,SUM(a.htAmount)  AS montantHt,SUM(a.taxAmount)  AS montantTva from Commande a where FUNCTION('DATE',a.createdAt)  between :fromDate and :toDate AND a.orderStatus=:orderStatut"
    )
    ChiffreAffaireAchat fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("orderStatut") OrderStatut orderStatut
    );
}
