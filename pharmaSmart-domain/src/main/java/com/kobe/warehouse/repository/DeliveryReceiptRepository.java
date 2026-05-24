package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryReceiptRepository extends JpaRepository<Commande, CommandeId> {
    @Query(
        value = "SELECT COALESCE(pf.libelle, f.libelle) AS libelle, " +
                "SUM(a.orderAmount) AS montantTtc, SUM(a.htAmount) AS montantHt, SUM(a.taxAmount) AS montantTva " +
                "FROM Commande a JOIN a.fournisseur f LEFT JOIN f.parent pf " +
                "WHERE FUNCTION('DATE', a.createdAt) BETWEEN :fromDate AND :toDate " +
                "AND a.orderStatus = :orderStatut " +
                "GROUP BY COALESCE(pf.id, f.id), COALESCE(pf.libelle, f.libelle)",
        countQuery = "SELECT COUNT(DISTINCT COALESCE(pf.id, f.id)) FROM Commande a JOIN a.fournisseur f LEFT JOIN f.parent pf " +
                     "WHERE FUNCTION('DATE', a.createdAt) BETWEEN :fromDate AND :toDate AND a.orderStatus = :orderStatut"
    )
    Page<GroupeFournisseurAchat> fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("orderStatut") OrderStatut orderStatut,
        Pageable pageable
    );
}
