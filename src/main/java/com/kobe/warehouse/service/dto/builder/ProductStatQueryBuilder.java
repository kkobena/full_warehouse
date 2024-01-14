package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

public final class ProductStatQueryBuilder {
  public static final String LIKE_STATEMENT =
      " AND (p.code_ean LIKE '%s' OR fp.code_cip LIKE '%s' OR p.libelle LIKE '%s') "; // ORDER BY

  public static final String ORDER_BY_STATEMENT = " ORDER BY %s DESC ";
  public static final String LIMIT_STATEMENT = " LIMIT %d, %d";
  public static final String PRODUIT_QUERY =
      """
SELECT COUNT(p.id) AS produit_count,SUM(l.ht_amount) AS ht_amount, p.id AS produit_id,p.code_ean,fp.code_cip,p.libelle,SUM(l.quantity_sold) AS quantity_sold,SUM(l.quantity_ug) AS quantity_ug,  SUM(l.net_amount) AS net_amount,SUM(l.cost_amount) AS cost_amount,SUM(l.sales_amount) AS sales_amount,SUM(l.discount_amount) AS discount_amount,
SUM(l.montant_tva_ug) AS montant_tva_ug,SUM(l.discount_amount_hors_ug) AS discount_amount_hors_ug,SUM(l.amount_to_be_taken_into_account) AS amount_to_be_taken_into_account,SUM(l.tax_amount) AS tax_amount
FROM sales_line l,sales s,produit p,(SELECT fp.code_cip,fp.produit_id  FROM fournisseur_produit fp WHERE fp.principal GROUP BY  fp.produit_id LIMIT 1) fp WHERE l.sales_id=s.id AND l.produit_id=p.id
AND fp.produit_id=p.id AND DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut='CLOSED'  AND s.canceled=false AND s.imported=0 %s %s %s {like_statement} GROUP BY p.id {order_by_statement} {limit_statement}
""";

  public static final String TOTAL_AMOUNT_QUERY =
      """
(SELECT SUM(l.ht_amount) FROM  sales_line l,sales s WHERE s.id=l.sales_id AND DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut='CLOSED'  AND s.canceled=false AND s.imported=0 %s %s %s)
""";
  public static final String TOTAL_QUNATITY_QUERY =
      """
(SELECT SUM(l.quantity_sold) FROM  sales_line l,sales s WHERE s.id=l.sales_id AND DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut='CLOSED'  AND s.canceled=false AND s.imported=0 %s %s %s)
""";
  public static final String PARETO_20x80_QUERY =
      """

SELECT (SUM(l.quantity_sold)/{quantity_query})*100 AS quantity_avg,SUM(l.ht_amount) AS ht_amount,
       (SUM(l.ht_amount)/{amount_query})*100 AS amount_avg,
       p.id AS produit_id,p.code_ean,fp.code_cip,p.libelle,SUM(l.quantity_sold) AS quantity_sold, SUM(l.net_amount) AS net_amount,SUM(l.sales_amount) AS sales_amount,SUM(l.tax_amount) AS tax_amount FROM sales_line l,sales s,produit p,(SELECT fp.code_cip,fp.produit_id  FROM fournisseur_produit fp WHERE fp.principal GROUP BY  fp.produit_id LIMIT 1) fp
WHERE l.sales_id=s.id AND l.produit_id=p.id AND fp.produit_id=p.id AND DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut='CLOSED'  AND s.canceled=false AND s.imported=0 %s %s %s GROUP BY p.id  ORDER BY amount_avg DESC
""";
  public static final String COUNT_QUERY =
      "SELECT COUNT(DISTINCT l.produit_id) AS produit_count FROM  sales_line l,sales s WHERE s.id=l.sales_id AND DATE(s.updated_at)  BETWEEN ?1 AND ?2 AND s.statut='CLOSED'  AND s.canceled=false AND s.imported=0 %s %s %s";

  public static ProductStatRecord buildProductStatRecord(Tuple tuple) {
    if (Objects.isNull(tuple.get("produit_id", Long.class))) return null;
    return new ProductStatRecord(
        tuple.get("produit_id", Long.class).intValue(),
        tuple.get("produit_count", Long.class).intValue(),
        tuple.get("code_cip", String.class),
        tuple.get("code_ean", String.class),
        tuple.get("libelle", String.class),
        tuple.get("quantity_sold", BigDecimal.class).intValue(),
        tuple.get("quantity_ug", BigDecimal.class).intValue(),
        tuple.get("net_amount", BigDecimal.class),
        tuple.get("cost_amount", BigDecimal.class),
        tuple.get("sales_amount", BigDecimal.class),
        tuple.get("discount_amount", BigDecimal.class),
        tuple.get("montant_tva_ug", BigDecimal.class),
        tuple.get("discount_amount_hors_ug", BigDecimal.class),
        tuple.get("amount_to_be_taken_into_account", BigDecimal.class),
        tuple.get("tax_amount", BigDecimal.class),
        tuple.get("ht_amount", BigDecimal.class));
  }

  public static ProductStatParetoRecord buildProductStatParetoRecord(Tuple tuple) {
    if (Objects.isNull(tuple.get("produit_id", Long.class))) return null;
    return new ProductStatParetoRecord(
        tuple.get("produit_id", Long.class).intValue(),
        tuple.get("code_cip", String.class),
        tuple.get("code_ean", String.class),
        tuple.get("libelle", String.class),
        tuple.get("quantity_sold", BigDecimal.class).intValue(),
        tuple
            .get("quantity_avg", BigDecimal.class)
            .round(new MathContext(2, RoundingMode.HALF_UP))
            .doubleValue(),
        tuple
            .get("amount_avg", BigDecimal.class)
            .round(new MathContext(2, RoundingMode.HALF_UP))
            .doubleValue(),
        tuple.get("net_amount", BigDecimal.class),
        tuple.get("sales_amount", BigDecimal.class),
        tuple.get("ht_amount", BigDecimal.class),
        tuple.get("tax_amount", BigDecimal.class));
  }
}
