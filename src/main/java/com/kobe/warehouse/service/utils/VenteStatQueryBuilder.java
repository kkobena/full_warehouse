package com.kobe.warehouse.service.utils;

import com.kobe.warehouse.service.dto.records.VenteRecord;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import javax.persistence.Tuple;

public final class VenteStatQueryBuilder {
  public static final String PERIOQIQUE_CA_QUERY =
      "SELECT SUM(s.sales_amount) AS sales_amount,SUM(s.amount_to_be_paid) AS amount_to_be_paid,SUM(s.discount_amount) AS discount_amount,SUM(s.cost_amount) AS cost_amount, SUM(s.marge) AS marge, SUM(s.amount_to_be_taken_into_account) AS amount_to_be_taken_into_account,SUM(s.net_amount) AS net_amount,SUM(s.ht_amount) AS ht_amount,SUM(s.part_assure) AS part_assure,SUM(s.part_tiers_payant) AS part_tiers_payant,SUM(s.tax_amount) AS tax_amount,SUM(s.rest_to_pay) AS rest_to_pay,SUM(s.ht_amount_ug) AS ht_amount_ug,SUM(s.discount_amount_hors_ug) AS discount_amount_hors_ug,SUM(s.discount_amount_ug) AS discount_amount_ug,SUM(s.net_ug_amount) AS net_ug_amount,SUM(s.marge_ug) AS marge_ug,SUM(s.montant_ttc_ug) AS montant_ttc_ug,SUM(s.payroll_amount) AS payroll_amount,SUM(s.montant_tva_ug) AS montant_tva_ug,SUM(s.montant_net_ug) AS montant_net_ug,SUM(payment.paid_amount) AS paid_amount,SUM(payment.real_net_amount) AS real_net_amount,COUNT(s.id) AS sale_count FROM sales s,(SELECT SUM(p.net_amount) AS real_net_amount,SUM(p.paid_amount) AS paid_amount,p.sales_id AS sales_id FROM payment p GROUP BY p.sales_id) as payment WHERE payment.sales_id=s.id   AND s.imported=0 AND  DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut=?3  %s %s %s";
  public static final String PERIOQIQUE_CA_QUERY_GROUPING_BY_PERIODE =
      "SELECT SUM(s.sales_amount) AS sales_amount,SUM(s.amount_to_be_paid) AS amount_to_be_paid,SUM(s.discount_amount) AS discount_amount,SUM(s.cost_amount) AS cost_amount, SUM(s.marge) AS marge, SUM(s.amount_to_be_taken_into_account) AS amount_to_be_taken_into_account,SUM(s.net_amount) AS net_amount,SUM(s.ht_amount) AS ht_amount,SUM(s.part_assure) AS part_assure,SUM(s.part_tiers_payant) AS part_tiers_payant,SUM(s.tax_amount) AS tax_amount,SUM(s.rest_to_pay) AS rest_to_pay,SUM(s.ht_amount_ug) AS ht_amount_ug,SUM(s.discount_amount_hors_ug) AS discount_amount_hors_ug,SUM(s.discount_amount_ug) AS discount_amount_ug,SUM(s.net_ug_amount) AS net_ug_amount,SUM(s.marge_ug) AS marge_ug,SUM(s.montant_ttc_ug) AS montant_ttc_ug,SUM(s.payroll_amount) AS payroll_amount,SUM(s.montant_tva_ug) AS montant_tva_ug,SUM(s.montant_net_ug) AS montant_net_ug,SUM(payment.paid_amount) AS paid_amount,SUM(payment.real_net_amount) AS real_net_amount,COUNT(s.id) AS sale_count FROM sales s,(SELECT SUM(p.net_amount) AS real_net_amount,SUM(p.paid_amount) AS paid_amount,p.sales_id AS sales_id FROM payment p GROUP BY p.sales_id) as payment WHERE payment.sales_id=s.id   AND s.imported=0 AND  DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut=?3  %s %s %s";

  public static final String CA = " AND s.ca='CA' ";
  public static final String CA_DEPOT = " AND s.ca='CA_DEPOT' ";
  public static final String CALLEBASE = " AND s.ca='CALLEBASE' ";
  public static final String TO_IGNORE = " AND s.ca='TO_IGNORE' ";
  public static final String DIFFERE = " AND s.differe=1 ";
  public static final String TYPE_VENTE = " AND s.dtype ='%s' ";

  private VenteStatQueryBuilder() {}

  public static VenteRecord buildVenteRecord(Tuple tuple) {
    if (Objects.isNull(tuple.get("sales_amount", BigDecimal.class))) return null;
    return new VenteRecord(
        tuple.get("sales_amount", BigDecimal.class),
        tuple.get("amount_to_be_paid", BigDecimal.class),
        tuple.get("discount_amount", BigDecimal.class),
        tuple.get("cost_amount", BigDecimal.class),
        tuple.get("marge", BigDecimal.class),
        tuple.get("amount_to_be_taken_into_account", BigDecimal.class),
        tuple.get("net_amount", BigDecimal.class),
        tuple.get("ht_amount", BigDecimal.class),
        tuple.get("part_assure", BigDecimal.class),
        tuple.get("part_tiers_payant", BigDecimal.class),
        tuple.get("tax_amount", BigDecimal.class),
        tuple.get("rest_to_pay", BigDecimal.class),
        tuple.get("ht_amount_ug", BigDecimal.class),
        tuple.get("discount_amount_hors_ug", BigDecimal.class),
        tuple.get("discount_amount_ug", BigDecimal.class),
        tuple.get("net_ug_amount", BigDecimal.class),
        tuple.get("marge_ug", BigDecimal.class),
        tuple.get("montant_ttc_ug", BigDecimal.class),
        tuple.get("payroll_amount", BigDecimal.class),
        tuple.get("montant_tva_ug", BigDecimal.class),
        tuple.get("montant_net_ug", BigDecimal.class),
        tuple.get("paid_amount", BigDecimal.class),
        tuple.get("real_net_amount", BigDecimal.class),
        tuple.get("sale_count", BigInteger.class));
  }
}
