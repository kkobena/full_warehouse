CREATE OR REPLACE FUNCTION get_product_sales_summary(
  p_start_date date,
  p_end_date date,
  p_statuts text[],
  p_cas text[],
  p_produit_id bigint,
  p_group_by int DEFAULT 0
)
  RETURNS jsonb AS
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
                   ORDER BY grp.min_date
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
                         END                                                                                         AS mvt_date,
                       MIN(s.sale_date)                                                                              AS min_date,
                       CEIL(SUM((o.quantity_requested * o.regular_unit_price) /
                                NULLIF(1 + (o.tax_value / 100), 0)))                                                 AS montant_ht,
                       SUM(o.quantity_requested)                                                                     AS quantite,
                       SUM(o.quantity_requested * o.cost_amount)                                                     AS montant_achat,
                       SUM(o.quantity_requested * o.regular_unit_price)                                              AS montant_ttc,
                       SUM(o.discount_amount)                                                                        AS montant_remise
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
$$ LANGUAGE plpgsql STABLE;



CREATE OR REPLACE FUNCTION get_product_order_summary(
  p_start_date date,
  p_end_date date,
  p_statut text,
  p_produit_id bigint,
  p_group_by int DEFAULT 0
)
  RETURNS jsonb AS
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'groupType', p_group_by,
                     'groupBy', grp.mvt_date,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat
                   )
                   ORDER BY grp.min_date
                 )
          FROM (SELECT CASE p_group_by
                         WHEN 0 THEN to_char(d.order_date, 'YYYY-MM-DD') -- jour
                         WHEN 1 THEN to_char(d.order_date, 'YYYY-MM') -- mois
                         WHEN 2 THEN to_char(d.order_date, 'YYYY-Q') -- trimestre
                         WHEN 3 THEN to_char(d.order_date, 'YYYY') ||
                                     CASE
                                       WHEN extract(quarter from d.order_date) <= 2
                                         THEN 'S1'
                                       ELSE 'S2' END -- semestre
                         WHEN 4 THEN to_char(d.order_date, 'YYYY') -- année
                         END                                          AS mvt_date,
                       MIN(d.order_date)                              AS min_date,
                       SUM(o.quantity_received)                       AS quantite,
                       SUM(o.order_cost_amount * o.quantity_received) AS montant_achat
                FROM order_line o
                       JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id
                       JOIN commande d ON o.commande_id = d.id
                WHERE o.commande_order_date = d.order_date
                  AND fp.produit_id = p_produit_id
                  AND d.order_status = p_statut
                  AND o.order_date BETWEEN p_start_date AND p_end_date
                GROUP BY CASE p_group_by
                           WHEN 0 THEN to_char(d.order_date, 'YYYY-MM-DD')
                           WHEN 1 THEN to_char(d.order_date, 'YYYY-MM')
                           WHEN 2 THEN to_char(d.order_date, 'YYYY-Q')
                           WHEN 3 THEN to_char(d.order_date, 'YYYY') ||
                                       CASE
                                         WHEN extract(quarter from d.order_date) <= 2
                                           THEN 'S1'
                                         ELSE 'S2' END
                           WHEN 4 THEN to_char(d.order_date, 'YYYY')
                           END) grp);
END;
$$ LANGUAGE plpgsql STABLE;



CREATE OR REPLACE FUNCTION get_product_order_summary_monthly(
  p_start_date date,
  p_end_date date,
  p_statut text,
  p_produit_id bigint
)
  RETURNS jsonb AS
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'annee', grp.annee,
                     'mois', grp.mois,
                     'quantite', grp.quantite,
                     'montantAchat', grp.montant_achat
                   )
                   ORDER BY grp.annee DESC, grp.mois DESC
                 )
          FROM (SELECT EXTRACT(YEAR FROM o.order_date)::int           AS annee,
                       EXTRACT(MONTH FROM o.order_date)::int          AS mois,
                       SUM(o.quantity_received)                       AS quantite,
                       SUM(o.quantity_received * o.order_cost_amount) AS montant_achat
                FROM order_line o
                       JOIN fournisseur_produit fp ON o.fournisseur_produit_id = fp.id
                       JOIN commande d ON o.commande_id = d.id
                WHERE fp.produit_id = p_produit_id
                  AND d.order_status = p_statut
                  AND o.order_date BETWEEN p_start_date AND p_end_date
                  AND o.commande_order_date = d.order_date
                GROUP BY EXTRACT(YEAR FROM o.order_date), EXTRACT(MONTH FROM o.order_date)) grp);
END;
$$ LANGUAGE plpgsql STABLE;



CREATE OR REPLACE FUNCTION get_product_sales_summary_monthly(
  p_start_date date,
  p_end_date date,
  p_statuts text[],
  p_cas text[],
  p_produit_id bigint
)
  RETURNS jsonb AS
$$
BEGIN
  RETURN (SELECT jsonb_agg(
                   jsonb_build_object(
                     'annee', grp.annee,
                     'mois', grp.mois,
                     'quantite', grp.quantite,
                     'montantTtc', grp.sales_amount
                   )
                   ORDER BY grp.annee DESC, grp.mois DESC
                 )
          FROM (SELECT EXTRACT(YEAR FROM s.sale_date)::int  AS annee,
                       EXTRACT(MONTH FROM s.sale_date)::int AS mois,
                       SUM(o.quantity_requested)            AS quantite,
                       SUM(o.sales_amount)                  AS sales_amount
                FROM sales_line o
                       JOIN sales s ON o.sales_id = s.id
                WHERE o.produit_id = p_produit_id
                  AND s.statut = ANY (p_statuts)
                  AND s.ca = ANY (p_cas)
                  AND s.sale_date BETWEEN p_start_date AND p_end_date
                  AND o.sales_sale_date = s.sale_date
                GROUP BY EXTRACT(YEAR FROM s.sale_date), EXTRACT(MONTH FROM s.sale_date)) grp);
END;
$$ LANGUAGE plpgsql STABLE;


CREATE OR REPLACE FUNCTION get_historique_vente(
  p_produit_id bigint,
  p_start_date date,
  p_end_date date,
  p_statuts text[],
  p_cas text[],
  p_offset int DEFAULT 0,
  p_limit int DEFAULT 20
)
  RETURNS jsonb AS
$$
BEGIN
  RETURN (
    WITH data AS (
      SELECT
        s.updated_at,
        s.number_transaction,
        o.quantity_requested,
        o.regular_unit_price,
        CEIL((o.quantity_requested * o.regular_unit_price) /
             NULLIF(1 + (o.tax_value / 100), 0))   AS ht_amount,
        o.sales_amount,
        o.discount_amount,
        u.first_name,
        u.last_name
      FROM sales_line o
             JOIN sales s ON o.sales_id = s.id
             JOIN app_user u ON s.caissier_id = u.id
      WHERE o.produit_id = p_produit_id
        AND s.statut = ANY (p_statuts)
        AND s.ca = ANY (p_cas)
        AND o.sales_sale_date = s.sale_date
        AND s.sale_date BETWEEN p_start_date AND p_end_date
      ORDER BY s.updated_at DESC
      LIMIT p_limit OFFSET p_offset
    )
    SELECT jsonb_build_object(
             'totalElements', (
        SELECT COUNT(*)
        FROM sales_line o
               JOIN sales s ON o.sales_id = s.id
        WHERE o.produit_id = p_produit_id
          AND s.statut = ANY (p_statuts)
          AND s.ca = ANY (p_cas)
          AND o.sales_sale_date = s.sale_date
          AND s.sale_date BETWEEN p_start_date AND p_end_date
      ),
             'content', jsonb_agg(
               jsonb_build_object(
                 'mvtDate', data.updated_at,
                 'reference', data.number_transaction,
                 'quantite', data.quantity_requested,
                 'prixUnitaire', data.regular_unit_price,
                 'montantHt', data.ht_amount,
                 'montantNet', data.sales_amount - data.discount_amount,
                 'montantTtc', data.sales_amount,
                 'montantRemise', data.discount_amount,
                 'montantTva', data.sales_amount - data.ht_amount,
                 'firstName', data.first_name,
                 'lastName', data.last_name
               )
                        )
           )
    FROM data
  );
END;
$$ LANGUAGE plpgsql STABLE;


alter table commande
  drop constraint IF EXISTS uk61ev37mxhvp6daoqsh7s10ok1;

alter table commande
  add constraint uk61ev37mxhvp6daoqsh7s10ok1
    unique (receipt_reference, fournisseur_id, order_date, order_status);
