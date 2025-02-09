package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import org.springframework.core.io.Resource;

public interface TableauPharmacienService extends MvtCommonService {
    String SALE_QUERY =
        """
        SELECT %s,
               COUNT(s.id) as numberCount,SUM(s.discount_amount) as montantDiscount,
               SUM(s.sales_amount) as montantTtc,SUM(p.paid_amount) as montantPaye, SUM(s.ht_amount) as montantHt,
               SUM(s.net_amount) as  montantNet,SUM(s.tax_amount) as montantTaxe,
               SUM(s.cost_amount) as montantAchat,SUM(s.rest_to_pay) as montantDiffere,SUM(s.amount_to_be_paid) as amountToBePaid,SUM(s.amount_to_be_taken_into_account) as amountToBeTakenIntoAccount,
               SUM(s.montant_net_ug) as montantNetUg,SUM(s.montant_ttc_ug) as montantTtcUg, SUM(s.montant_tva_ug) as montantHtUg,SUM(s.part_tiers_payant) AS partTiersPayant ,SUM(s.part_assure) AS partAssure
        FROM sales s LEFT JOIN  payment p  ON p.sales_id=s.id

        """;
    String DATE_COLUMN = "DATE_FORMAT(s.updated_at, '%Y-%m-%d') AS mvtDate";
    String DATE_AS_MONTH = "DATE_FORMAT(s.updated_at, '%Y-%m') AS mvtDate";

    String WHERE_CLAUSE = " WHERE DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (%s) AND s.dtype IN (%s) AND s.ca IN (%s) ";

    String SALE_QUERY_GROUP_BY =
        """
         GROUP BY mvtDate ORDER BY mvtDate
        """;
    String ACHAT_QUERY =
        """
        SELECT %s ,SUM(dr.net_amount) as montantNet,
        SUM(dr.tax_amount) AS montantTaxe,SUM(dr.receipt_amount) as montantTtc,SUM(dr.discount_amount) as montantRemise,
        gf.id as groupeGrossisteId,gf.libelle as groupeGrossiste,gf.odre as ordreAffichage FROM  delivery_receipt dr
        JOIN fournisseur f ON f.id=dr.fournisseur_id  JOIN  groupe_fournisseur gf ON gf.id=f.groupe_fournisseur_id
        WHERE dr.modified_date BETWEEN ?1 AND ?2 AND dr.receipt_status='CLOSE' group by mvtDate,groupeGrossisteId ORDER BY mvtDate
        """;
    String ACHAT_GROUP_BY_DATE = " DATE_FORMAT(dr.modified_date, '%Y-%m-%d') AS mvtDate";
    String ACHAT_GROUP_BY_MONTH = " DATE_FORMAT(dr.modified_date, '%Y-%m') AS mvtDate";

    TableauPharmacienWrapper getTableauPharmacien(MvtParam mvtParam);

    Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException;

    Resource exportToExcel(MvtParam mvtParam) throws IOException;

    List<GroupeFournisseurDTO> fetchGroupGrossisteToDisplay();

    default Pair getGroupBy(String groupeBy) {
        if ("month".equals(groupeBy)) {
            return new Pair(DATE_AS_MONTH, ACHAT_GROUP_BY_MONTH);
        }
        return new Pair(DATE_COLUMN, ACHAT_GROUP_BY_DATE);
    }

    default String buildQuery(MvtParam mvtParam) {
        return (
            String.format(SALE_QUERY, getGroupBy(mvtParam.getGroupeBy()).key().toString()) +
            buildWhereClause(mvtParam) +
            SALE_QUERY_GROUP_BY
        );
    }

    private String buildWhereClause(MvtParam mvtParam) {
        return this.buildWhereClause(WHERE_CLAUSE, mvtParam);
    }

    default String buildAchatQuery(String groupeBy) {
        return String.format(ACHAT_QUERY, getGroupBy(groupeBy).value().toString());
    }
}
