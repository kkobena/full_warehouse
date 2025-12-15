DROP FUNCTION IF EXISTS get_product_sales_summary(date, date, text[], text[], integer, integer);



create function get_product_sales_summary(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                                          p_produit_id integer, p_group_by integer DEFAULT 0) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'groupType', p_group_by,
                     'groupBy', grp.mvt_date,
                     'montantHt', grp.montant_ht,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat,
                     'montantTtc', grp.montant_ttc,
                     'montantRemise', grp.montant_remise
                   )
                   ORDER BY grp.mvt_date
                 )
          FROM (SELECT CASE p_group_by
                         WHEN 0 THEN to_char(s.sale_date, 'YYYY-MM-DD')
                         WHEN 1 THEN to_char(s.sale_date, 'YYYY-MM')
                         WHEN 2 THEN to_char(s.sale_date, 'YYYY-Q')
                         WHEN 3 THEN to_char(s.sale_date, 'YYYY') ||
                                     CASE
                                       WHEN extract(quarter from s.sale_date) <= 2
                                         THEN 'S1'
                                       ELSE 'S2' END
                         WHEN 4 THEN to_char(s.sale_date, 'YYYY')
                         END                                            AS mvt_date,
                       CEIL(SUM((o.quantity_requested * o.regular_unit_price) /
                                NULLIF(1 + (o.tax_value / 100), 0)))    AS montant_ht,
                       SUM(o.quantity_requested)                        AS quantite,
                       SUM(o.quantity_requested * o.cost_amount)        AS montant_achat,
                       SUM(o.quantity_requested * o.regular_unit_price) AS montant_ttc,
                       SUM(o.discount_amount)                           AS montant_remise
                FROM sales_line o
                       JOIN sales s ON o.sales_id = s.id
                WHERE o.sales_sale_date = s.sale_date
                  AND s.sale_date BETWEEN p_start_date AND p_end_date
                  AND o.produit_id = p_produit_id
                  AND s.statut = ANY (p_statuts)
                  AND s.ca = ANY (p_cas)
                GROUP BY CASE p_group_by
                           WHEN 0 THEN to_char(s.sale_date, 'YYYY-MM-DD')
                           WHEN 1 THEN to_char(s.sale_date, 'YYYY-MM')
                           WHEN 2 THEN to_char(s.sale_date, 'YYYY-Q')
                           WHEN 3 THEN to_char(s.sale_date, 'YYYY') ||
                                       CASE
                                         WHEN extract(quarter from s.sale_date) <= 2
                                           THEN 'S1'
                                         ELSE 'S2' END
                           WHEN 4 THEN to_char(s.sale_date, 'YYYY')
                           END) grp);
END;
$$;

