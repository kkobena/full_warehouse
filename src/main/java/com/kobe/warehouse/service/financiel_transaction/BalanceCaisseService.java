package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import java.net.MalformedURLException;
import org.springframework.core.io.Resource;

public interface BalanceCaisseService extends MvtCommonService {
    String SALE_QUERY =
        """
        SELECT s.dtype AS typeSale,COUNT(s.id) as numberCount,SUM(s.discount_amount) as montantDiscount,
        SUM(s.sales_amount) as montantTtc,SUM(p.paid_amount) as montantPaye,SUM(p.reel_amount) as reelAmount, SUM(s.ht_amount) as montantHt,
        SUM(s.net_amount) as  montantNet,p.payment_mode_code as modePaiement,pm.libelle as libelleModePaiement,SUM(s.tax_amount) as montantTaxe,
        SUM(s.cost_amount) as montantAchat, SUM(s.rest_to_pay) as montantDiffere,SUM(s.amount_to_be_paid) as amountToBePaid,SUM(s.amount_to_be_taken_into_account) as amountToBeTakenIntoAccount,
        SUM(s.montant_net_ug) as montantNetUg,SUM(s.montant_ttc_ug) as montantTtcUg, SUM(s.montant_tva_ug) as montantHtUg,SUM(s.part_tiers_payant) AS partTiersPayant ,SUM(s.part_assure) AS partAssure
        FROM sales s LEFT JOIN  payment_transaction p  ON p.sale_id=s.id JOIN payment_mode pm ON pm.code=p.payment_mode_code
        """;

    String SALE_QUERY_GROUP_BY =
        """
         GROUP BY s.dtype, p.payment_mode_code
        """;
    String MVT_QUERY =
        """
        SELECT SUM(p.reel_amount) as amount,p.payment_mode_code AS modePaiement,pm.libelle as libelleModePaiement,p.type_transaction as typeTransaction  FROM  payment_transaction p  join payment_mode pm ON  p.payment_mode_code = pm.code
        WHERE DATE(p.created_at) BETWEEN ?1 AND ?2 AND p.categorie_ca in (%s) group by p.payment_mode_code,p.type_transaction
        """;
    String WHERE_CLAUSE = " WHERE DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (%s) AND s.dtype IN (%s) AND s.ca IN (%s) ";

    BalanceCaisseWrapper getBalanceCaisse(MvtParam mvtParam);

    default String getWhereClause(MvtParam mvtParam) {
        return this.buildWhereClause(WHERE_CLAUSE, mvtParam);
    }

    Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException;
}
