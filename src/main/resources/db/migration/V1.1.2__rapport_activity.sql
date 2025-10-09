create or replace function rapport_activite_vente_report(
  p_start_date date,
  p_end_date date,
  p_statuts text[],
  p_cas text[],
  p_exclude_free_qty boolean default false,
  p_to_ignore boolean default false
)
  returns jsonb
  language sql
as
$$
with filtered_sales as (select id
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.cost_amount
                               )                                           as cost_amount,

                               sum(
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               ceiling(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               ceiling(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / nullif(1 + (sl.tax_value / 100), 0)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore),
     payment_agg as (select jsonb_agg(
                              jsonb_build_object(
                                'code', t.code,
                                'libelle', t.libelle,
                                'paidAmount', t.total_paid_amount,
                                'realAmount', t.total_real_amount
                              )
                            ) as payments
                     from (select pm.code,
                                  pm.libelle,
                                  sum(p.paid_amount) as total_paid_amount,
                                  sum(p.reel_amount) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                           group by pm.code, pm.libelle) t),
     sales_agg as (select sum(s.discount_amount)   as total_discount_amount,
                          sum(s.part_tiers_payant) as total_part_tiers_payant,
                          sum(s.rest_to_pay)       as total_rest_to_pay,
                          count(distinct s.id)     as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id)
select jsonb_build_object(
         'montantTtc', coalesce(sla.sales_amount, 0),
         'montantRemise', sa.total_discount_amount,
         'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
         'montantTp', coalesce(sa.total_part_tiers_payant, 0),
         'montantDiffere', sa.total_rest_to_pay,
         'nombreVente', sa.distinct_sales_count,
         'montantTtcUg', sla.sales_ug_amount,
         'montantHtUg', sla.total_sales_ug_tax,
         'montantAchat', coalesce(sla.cost_amount, 0),
         'montantHt', coalesce(sla.total_sales_excl_tax, 0),
         'payments', coalesce(pa.payments, '[]'::jsonb)
       )
from sales_agg sa
       cross join sales_line_agg sla
       left join payment_agg pa on true;
$$;



CREATE OR REPLACE FUNCTION find_reglement_tierspayant(
  p_from_date DATE,
  p_to_date DATE,
  p_search TEXT DEFAULT NULL,
  p_offset INT DEFAULT 0,
  p_limit INT DEFAULT 20
)
  RETURNS JSONB AS
$$
BEGIN
  RETURN (WITH data AS (SELECT tp.full_name             AS libelle,
                               tp.categorie             AS type,
                               f.num_facture            AS num_facture,
                               SUM(it.montantReglement) AS montant_reglement,
                               SUM(it.montantFacture)   AS montant_facture
                        FROM payment_transaction p
                               JOIN facture_tiers_payant f ON p.facture_tierspayant_id = f.id
                               JOIN tiers_payant tp ON f.tiers_payant_id = tp.id
                               JOIN (SELECT s.facture_tiers_payant_id,
                                            SUM(s.montant_regle) AS montantReglement,
                                            SUM(s.montant)       AS montantFacture
                                     FROM third_party_sale_line s
                                     GROUP BY s.facture_tiers_payant_id, s.sale_date) it
                                    ON it.facture_tiers_payant_id = f.id

                        WHERE p.transaction_date BETWEEN p_from_date AND p_to_date
                          AND (
                          p_search IS NULL
                            OR tp.name ILIKE p_search
                            OR tp.full_name ILIKE p_search
                          )
                        GROUP BY tp.id, tp.full_name, tp.categorie, f.num_facture
                        ORDER BY tp.full_name
                        LIMIT p_limit OFFSET p_offset)
          SELECT jsonb_build_object(
                   'totalElements', (SELECT COUNT(*)
                                     FROM (SELECT 1
                                           FROM payment_transaction p
                                                  JOIN facture_tiers_payant f ON p.facture_tierspayant_id = f.id
                                                  JOIN tiers_payant tp ON f.tiers_payant_id = tp.id
                                           WHERE p.transaction_date BETWEEN p_from_date AND p_to_date
                                             AND (
                                             p_search IS NULL
                                               OR tp.name ILIKE p_search
                                               OR tp.full_name ILIKE p_search
                                             )
                                           GROUP BY tp.id, tp.full_name, tp.categorie,
                                                    f.num_facture) sub),
                   'content', jsonb_agg(
                     jsonb_build_object(
                       'libelle', data.libelle,
                       'type', data.type,
                       'factureNumber', data.num_facture,
                       'montantReglement', data.montant_reglement,
                       'montantFacture', data.montant_facture
                     )
                              )
                 )
          FROM data);
END;
$$ LANGUAGE plpgsql STABLE;



