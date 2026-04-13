package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashRegisterItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository pour {@link CashRegisterItem}.
 * Contient les requêtes natives utilisées par le dashboard préparateur.
 */
@Repository
public interface CashRegisterItemRepository extends JpaRepository<CashRegisterItem, Integer> {

    /**
     * Calcule la somme des encaissements en espèces pour une caisse donnée.
     * Source : payment_transaction (temps réel, session ouverte ou fermée).
     * Filtre sur payment_group='CASH', les type_transaction inclus et les dtype exclus.
     *
     * @param cashRegisterId   identifiant de la caisse
     * @param typeTransactions types de transactions à inclure (ex: CASH_SALE, ENTREE_CAISSE, …)
     * @param excludedDtypes   sous-classes à exclure (ex: PaymentFournisseur, AccountTransaction)
     * @return montant total des espèces encaissées
     */
    @Query(
        value = """
            SELECT COALESCE(SUM(pt.paid_amount), 0)
            FROM payment_transaction pt
            INNER JOIN payment_mode pm ON pt.payment_mode_code = pm.code
            WHERE pt.cash_register_id = :crId
              AND pm.payment_group = 'CASH'
              AND pt.type_transaction IN (:typeTransactions)
              AND pt.dtype NOT IN (:excludedDtypes)
            """,
        nativeQuery = true
    )
    Long sumEncaissementsEspeces(
        @Param("crId") Integer cashRegisterId,
        @Param("typeTransactions") Set<String> typeTransactions,
        @Param("excludedDtypes") Set<String> excludedDtypes
    );

    /**
     * Retourne les encaissements agrégés par mode de paiement pour une caisse.
     * Source : payment_transaction (temps réel, session ouverte ou fermée).
     * Une ligne par mode utilisé : [code, libelle, payment_group, montant]
     *
     * @param cashRegisterId identifiant de la caisse
     * @param excludedDtypes sous-classes à exclure (ex: PaymentFournisseur, AccountTransaction)
     * @return liste de Object[] : [code, libelle, payment_group, montant], triée par ordre_tri
     */
    @Query(
        value = """
            SELECT
                pm.code          AS code,
                pm.libelle       AS libelle,
                pm.payment_group AS payment_group,
                COALESCE(SUM(pt.paid_amount), 0) AS montant
            FROM payment_transaction pt
            INNER JOIN payment_mode pm ON pt.payment_mode_code = pm.code
            WHERE pt.cash_register_id = :crId
              AND pt.dtype NOT IN (:excludedDtypes)
            GROUP BY pm.code, pm.libelle, pm.payment_group, pm.ordre_tri
            HAVING SUM(pt.paid_amount) > 0
            ORDER BY pm.ordre_tri
            """,
        nativeQuery = true
    )
    List<Object[]> findEncaissementsParMode(
        @Param("crId") Integer cashRegisterId,
        @Param("excludedDtypes") Set<String> excludedDtypes
    );
}

