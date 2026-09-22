-- La colonne « Crédit » du tableau du pharmacien doit dire le montant non encaissé *de ce
-- jour-là*. Elle lisait le rest_to_pay *courant* de la vente, que le règlement ultérieur d'un
-- différé remet à zéro : une journée passée perdait donc son crédit alors que son Montant Net
-- ne bougeait pas, et la décomposition « Net = Comptant + Crédit » cessait de boucler
-- rétroactivement, à mesure que les clients venaient payer leur ardoise.
--
-- Le solde d'un différé n'est pas un SalePayment : il s'inscrit dans differe_payment_item, que
-- les deux fonctions ignoraient. On y relit ce qui a été réglé depuis pour reconstituer le
-- montant différé d'origine -- le règlement lui-même reste une entrée de caisse, comptée à sa
-- propre date par les mouvements de caisse, et n'a pas à revenir dans le comptant de la vente.
--
-- Constaté sur la base de démonstration : 49 journées sur 155 ne bouclaient pas, dont 895 215 F
-- portés par 70 ventes différées soldées.

CREATE OR REPLACE FUNCTION tableau_pharmacien_report(p_start_date date, p_end_date date, p_statuts text[], p_cas text[], p_to_ignore boolean DEFAULT false, p_mode text DEFAULT 'REEL'::text)
 RETURNS jsonb
 LANGUAGE sql
AS $function$
with filtered_sales as (select id, sale_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.sale_date,

                               sum(
                                 ca_pondere_ligne(sl.quantity_requested * sl.cost_amount, sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as cost_amount,

                               sum(
                                 ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 (ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               round(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND sl.sale_date = fs.sale_date
                        group by fs.sale_date),
     payment_agg as (select t.sale_date,
                            jsonb_agg(
                              jsonb_build_object(
                                'code', t.code,
                                'libelle', t.libelle,
                                'paidAmount', t.total_paid_amount,
                                'realAmount', t.total_real_amount
                              )
                            ) as payments
                     from (select fs.sale_date,
                                  pm.code,
                                  pm.libelle,
                                  sum(ca_reglement(p.paid_amount, p.amount_to_be_taken_into_account, p_mode)) as total_paid_amount,
                                  sum(ca_reglement(p.reel_amount, p.amount_to_be_taken_into_account, p_mode)) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND p.sale_date = fs.sale_date
                           group by fs.sale_date, pm.code, pm.libelle) t
                     group by t.sale_date),
     differe_regle as (select dpi.sale_id,
                              dpi.sale_sale_date,
                              sum(dpi.paid_amount) as montant_regle
                       from differe_payment_item dpi
                              join filtered_sales fs
                                   on fs.id = dpi.sale_id and fs.sale_date = dpi.sale_sale_date
                       group by dpi.sale_id, dpi.sale_sale_date),
     sales_agg as (select fs.sale_date,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay + coalesce(dr.montant_regle, 0))         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and s.sale_date = fs.sale_date
                          left join differe_regle dr
                                    on dr.sale_id = s.id and dr.sale_sale_date = s.sale_date
                   group by fs.sale_date)
select jsonb_agg(
         jsonb_build_object(
           'mvtDate', sa.sale_date,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', sa.total_discount_amount,
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantCredit', coalesce(sa.total_part_tiers_payant, 0) + sa.total_rest_to_pay,
           'montantDiffere', sa.total_rest_to_pay,
           'nombreVente', sa.distinct_sales_count,
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
           order by sa.sale_date
       )
from sales_agg sa
       join sales_line_agg sla on sa.sale_date = sla.sale_date
       left join payment_agg pa on sa.sale_date = pa.sale_date;
$function$;

CREATE OR REPLACE FUNCTION tableau_pharmacien_month_report(p_start_date date, p_end_date date, p_statuts text[], p_cas text[], p_to_ignore boolean DEFAULT false, p_mode text DEFAULT 'REEL'::text)
 RETURNS jsonb
 LANGUAGE sql
AS $function$
with filtered_sales as (select id, date_trunc('month', sale_date)::date as month_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.month_date,

                               sum(
                                 ca_pondere_ligne(sl.quantity_requested * sl.cost_amount, sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as cost_amount,

                               sum(
                                 ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               ceiling(sum(
                                 (ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)) * sl.taux_remise
                                       ))                                  as remise_produit,

                               sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

                               round(
                                 sum(
                                   (sl.quantity_ug * sl.regular_unit_price)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_ug_tax,

                               ceiling(sum(
                                 (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                                       ))                                  as remise_ug_produit

                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND date_trunc('month', sl.sale_date)::date = fs.month_date
                        group by fs.month_date),
     payment_agg as (select t.month_date,
                            jsonb_agg(
                              jsonb_build_object(
                                'code', t.code,
                                'libelle', t.libelle,
                                'paidAmount', t.total_paid_amount,
                                'realAmount', t.total_real_amount
                              )
                            ) as payments
                     from (select date_trunc('month', fs.month_date)::date as month_date,
                                  pm.code,
                                  pm.libelle,
                                  sum(ca_reglement(p.paid_amount, p.amount_to_be_taken_into_account, p_mode))                       as total_paid_amount,
                                  sum(ca_reglement(p.reel_amount, p.amount_to_be_taken_into_account, p_mode))                       as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND date_trunc('month', p.sale_date)::date = fs.month_date
                           group by date_trunc('month', fs.month_date), pm.code, pm.libelle) t
                     group by t.month_date),
     differe_regle as (select dpi.sale_id,
                              sum(dpi.paid_amount) as montant_regle
                       from differe_payment_item dpi
                              join filtered_sales fs
                                   on fs.id = dpi.sale_id
                                       and date_trunc('month', dpi.sale_sale_date)::date = fs.month_date
                       group by dpi.sale_id),
     sales_agg as (select fs.month_date,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay + coalesce(dr.montant_regle, 0))         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id
                          left join differe_regle dr on dr.sale_id = s.id
                   WHERE date_trunc('month', s.sale_date)::date = fs.month_date
                   group by fs.month_date)
select jsonb_agg(
         jsonb_build_object(
           'mvtDate', to_char(sa.month_date, 'YYYY-MM-DD'),
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', sa.total_discount_amount,
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantCredit', coalesce(sa.total_part_tiers_payant, 0) + sa.total_rest_to_pay,
           'montantDiffere', sa.total_rest_to_pay,
           'nombreVente', sa.distinct_sales_count,
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         ) order by sa.month_date
       )
from sales_agg sa
       join sales_line_agg sla on sa.month_date = sla.month_date
       left join payment_agg pa on sa.month_date = pa.month_date;
$function$;
