package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.domain.PaymentId;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface PaymentFournisseurRepository extends JpaRepository<PaymentFournisseur, PaymentId> {

    @Query(
        "SELECT pf.commande.id, SUM(pf.paidAmount) " +
        "FROM PaymentFournisseur pf " +
        "WHERE pf.commande.id IN :commandeIds " +
        "GROUP BY pf.commande.id"
    )
    List<Object[]> sumPaidAmountsByCommandeIds(@Param("commandeIds") List<Integer> commandeIds);

    @Query(
        "SELECT pf FROM PaymentFournisseur pf " +
        "WHERE pf.commande.id = :commandeId " +
        "AND pf.commande.fournisseur.id = :fournisseurId " +
        "ORDER BY pf.transactionDate ASC"
    )
    List<PaymentFournisseur> findByCommandeIdAndFournisseurId(
        @Param("commandeId") Integer commandeId,
        @Param("fournisseurId") Integer fournisseurId
    );
}
