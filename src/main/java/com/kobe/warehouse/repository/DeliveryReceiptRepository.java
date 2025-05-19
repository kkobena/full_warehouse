package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceipt;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
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
public interface DeliveryReceiptRepository extends JpaRepository<DeliveryReceipt, Long> {
    @Query(
        value = "select a.fournisseur.groupeFournisseur.libelle AS libelle,SUM(a.receiptAmount)  AS montantTtc,SUM(a.netAmount)  AS montantHt,SUM(a.taxAmount)  AS montantTva from DeliveryReceipt a where FUNCTION('DATE',a.createdDate)  between :fromDate and :toDate AND a.receiptStatut=:receiptStatut GROUP BY a.fournisseur.groupeFournisseur.id ",
        countQuery = "select count(a.fournisseur.groupeFournisseur.id) from DeliveryReceipt a where FUNCTION('DATE',a.createdDate)  between :fromDate and :toDate AND a.receiptStatut=:receiptStatut "
    )
    Page<GroupeFournisseurAchat> fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("receiptStatut") ReceiptStatut receiptStatut,
        Pageable pageable
    );

    @Query(
        value = "select SUM(a.receiptAmount)  AS montantTtc,SUM(a.netAmount)  AS montantHt,SUM(a.taxAmount)  AS montantTva from DeliveryReceipt a where FUNCTION('DATE',a.createdDate)  between :fromDate and :toDate AND a.receiptStatut=:receiptStatut"
    )
    ChiffreAffaireAchat fetchAchats(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("receiptStatut") ReceiptStatut receiptStatut
    );
}
