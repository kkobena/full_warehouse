-- La TVA d'une vente se calcule sur ce que la cliente paie : une remise accordée à la vente
-- (remise produit, promotion) sort de la base imposable. Les états la calculaient sur le brut
-- (quantité × prix catalogue) et montraient la remise à part : HT et TVA étaient surévalués dès
-- qu'une ligne était remisée. Les tickets et les montants enregistrés de la vente passent au net
-- dans le même temps (SaleAmountCalculator.computeTva).
--
-- Chaque état calcule désormais son HT sur le net de ligne, et rend la TVA collectée
-- (« montantTva » : net TTC moins HT) plutôt que de la laisser déduire en TTC − HT, qui
-- y remettrait la remise. Le montant TTC et la remise affichés ne changent pas.
--
-- Pas de reprise des ventes passées : elles sont recalculées depuis leurs lignes par ces états ;
-- seuls les montants enregistrés sur la vente (sales.tax_amount, tva_embeded) gardent l'ancien
-- calcul pour les ventes déjà clôturées.

-- Montant net TTC d'une ligne : brut moins remise, l'un et l'autre selon le mode (réel / déclaré).
create or replace function ca_net_ligne(p_quantity_requested integer,
                                        p_regular_unit_price integer,
                                        p_discount_amount integer,
                                        p_declarable integer,
                                        p_mode text) returns numeric
  language sql
  immutable
  parallel safe
as
$$
select ca_ttc_ligne(p_quantity_requested, p_regular_unit_price, p_declarable, p_mode)
         - ca_pondere_ligne(coalesce(p_discount_amount, 0), p_quantity_requested, p_regular_unit_price,
                            p_declarable, p_mode)
$$;

comment on function ca_net_ligne(integer, integer, integer, integer, text) is
  'Montant net TTC d''une ligne (remise deduite) : declarable en DECLARE, reel en REEL.';


-- sales_tva_report (dernière définition : V2.1.0__tva_report_montant_achat.sql)
create or replace function sales_tva_report(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                                            p_to_ignore boolean DEFAULT false,
                                            p_mode text DEFAULT 'REEL') returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select sl.tax_value,
                               sum(
                                 ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode))) - round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes,

                               round(
                                 sum(
                                   ca_pondere_ligne(sl.quantity_requested * sl.cost_amount, sl.quantity_requested,
                                                    sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                                 )
                               )                                           as cost_amount,

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
                        group by sl.tax_value)
select jsonb_agg(
         jsonb_build_object(
           'codeTva', sla.tax_value,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', coalesce(sla.remise_produit, 0),
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'montantTva', coalesce(sla.montant_tva_lignes, 0)
         )
           order by sla.tax_value
       )
from sales_line_agg sla;
$$;


-- sales_tva_report_journalier (dernière définition : V2.1.0__tva_report_montant_achat.sql)
create or replace function sales_tva_report_journalier(p_start_date date, p_end_date date, p_statuts text[],
                                                       p_cas text[], p_to_ignore boolean DEFAULT false,
                                                       p_mode text DEFAULT 'REEL') returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.sale_date,
                               sl.tax_value,

                               sum(
                                 ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode))) - round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes,

                               round(
                                 sum(
                                   ca_pondere_ligne(sl.quantity_requested * sl.cost_amount, sl.quantity_requested,
                                                    sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                                 )
                               )                                           as cost_amount,

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
                        group by fs.sale_date, sl.tax_value)
select jsonb_agg(
         jsonb_build_object(
           'mvtDate', sla.sale_date,
           'codeTva', sla.tax_value,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'montantRemise', coalesce(sla.remise_produit, 0),
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'montantTva', coalesce(sla.montant_tva_lignes, 0)
         )
           order by sla.sale_date, sla.tax_value
       )
from sales_line_agg sla;
$$;


-- sales_balance (dernière définition : V1.9.1__socle_declaration_ca.sql)
create or replace function sales_balance(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                              p_to_ignore boolean DEFAULT false,
                              p_mode text DEFAULT 'REEL') returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date, dtype
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.dtype,
                               sum(
                                 ca_pondere_ligne(sl.quantity_requested * sl.cost_amount, sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as cost_amount,

                               sum(
                                 ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode))) - round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes,

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
                        group by fs.dtype),
     payment_agg as (select dtype,
                            jsonb_agg(
                              jsonb_build_object(
                                'code', code,
                                'libelle', libelle,
                                'paidAmount', total_paid_amount,
                                'realAmount', total_real_amount
                              )
                            ) as payments
                     from (select fs.dtype,
                                  pm.code,
                                  pm.libelle,
                                  sum(ca_reglement(p.paid_amount, p.amount_to_be_taken_into_account, p_mode)) as total_paid_amount,
                                  sum(ca_reglement(p.reel_amount, p.amount_to_be_taken_into_account, p_mode)) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND p.sale_date = fs.sale_date
                           group by fs.dtype, pm.code, pm.libelle) t
                     group by dtype),
     sales_agg as (select fs.dtype,
                          sum(s.amount_to_be_paid)                                   as total_amount_to_be_paid,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_assure)                                         as total_part_assure,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay)                                         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and s.sale_date = fs.sale_date
                   group by fs.dtype)
select jsonb_agg(
         jsonb_build_object(
           'typeSale', sa.dtype,
           'montantTtc', coalesce(sla.sales_amount, 0),
           'discountAmount', sa.total_discount_amount,
           'montantRemiseUg', coalesce(sla.remise_ug_produit, 0),
           'partAssure', coalesce(sa.total_part_assure, 0),
           'partTiersPayant', coalesce(sa.total_part_tiers_payant, 0),
           'montantDiffere', sa.total_rest_to_pay,
           'count', sa.distinct_sales_count,
           'montantTtcUg', sla.sales_ug_amount,
           'montantHtUg', sla.total_sales_ug_tax,
           'montantAchat', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'montantTva', coalesce(sla.montant_tva_lignes, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


-- tableau_pharmacien_report (dernière définition : V2.1.1__tableau_pharmacien_credit_differe.sql)
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
                                   ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode))) - round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes,

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
           'montantTva', coalesce(sla.montant_tva_lignes, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
           order by sa.sale_date
       )
from sales_agg sa
       join sales_line_agg sla on sa.sale_date = sla.sale_date
       left join payment_agg pa on sa.sale_date = pa.sale_date;
$function$;


-- tableau_pharmacien_month_report (dernière définition : V2.1.1__tableau_pharmacien_credit_differe.sql)
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
                                   ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode))) - round(sum(ca_net_ligne(sl.quantity_requested, sl.regular_unit_price, sl.discount_amount, sl.amount_to_be_taken_into_account, p_mode) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes,

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
           'montantTva', coalesce(sla.montant_tva_lignes, 0),
           'payments', coalesce(pa.payments, '[]'::jsonb)
         ) order by sa.month_date
       )
from sales_agg sa
       join sales_line_agg sla on sa.month_date = sla.month_date
       left join payment_agg pa on sa.month_date = pa.month_date;
$function$;


-- sales_summary_json (dernière définition : V1.9.0__fix_tva_division_entiere.sql)
create or replace function sales_summary_json(p_start_date date, p_end_date date, p_statuts text[],
                                   p_cas text[], p_canceled boolean) returns jsonb
  language sql
as
$$
with
-- Filtrer les ventes selon les paramètres
filtered_sales as (select id, sale_date
                   from sales
                   where sale_date between p_start_date and p_end_date
                     and imported = false
                     and statut = any (p_statuts)
                     and ca = any (p_cas)
                     and canceled = p_canceled),

-- Agrégats des lignes de vente
sales_line_agg as (select sum(sl.quantity_requested * sl.cost_amount)        as cost_amount,
                          sum(sl.quantity_requested * sl.regular_unit_price) as sales_amount,
                          round(sum((sl.quantity_requested * sl.regular_unit_price - coalesce(sl.discount_amount, 0)) /
                                      tva_divisor(sl.tax_value)))           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum((sl.quantity_requested * sl.regular_unit_price - coalesce(sl.discount_amount, 0)))) - round(sum((sl.quantity_requested * sl.regular_unit_price - coalesce(sl.discount_amount, 0)) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes

                   from sales_line sl
                          join filtered_sales fs on fs.id = sl.sales_id
                   WHERE sl.sale_date = fs.sale_date
                     AND sl.to_ignore = false),
-- Agrégats des paiements
payment_agg as (select sum(p.paid_amount) as total_paid_amount,
                       sum(p.reel_amount) as total_reel_amount
                from payment_transaction p
                       join filtered_sales fs on fs.id = p.sale_id

                where p.dtype = 'SalePayment'
                  AND p.sale_date = fs.sale_date),
-- Agrégats de la table sales
sales_agg as (select sum(s.amount_to_be_paid)               as total_amount_to_be_paid,
                     sum(s.discount_amount)                 as total_discount_amount,
                     sum(s.amount_to_be_taken_into_account) as total_amount_to_account,

                     sum(s.part_assure)                     as total_part_assure,
                     sum(s.part_tiers_payant)               as total_part_tiers_payant,
                     sum(s.rest_to_pay)                     as total_rest_to_pay,
                     sum(s.payroll_amount)                  as total_payroll_amount,
                     count(distinct s.id)                   as distinct_sales_count
              from sales s
                     join filtered_sales fs on fs.id = s.id
              WHERE s.sale_date = fs.sale_date)
-- Construction finale du JSONB
select jsonb_build_object(
         'salesAmount', coalesce(sla.sales_amount, 0),
         'amountToBePaid', sa.total_amount_to_be_paid,
         'discountAmount', sa.total_discount_amount,
         'amountToBeTakenIntoAccount', sa.total_amount_to_account,
         'netAmount', coalesce(sla.sales_amount, 0) - coalesce(sa.total_discount_amount, 0),
         'partAssure', coalesce(sa.total_part_assure, 0),
         'partTiersPayant', coalesce(sa.total_part_tiers_payant, 0),
         'restToPay', sa.total_rest_to_pay,
         'payrollAmount', sa.total_payroll_amount,
         'saleCount', sa.distinct_sales_count,
         'costAmount', coalesce(sla.cost_amount, 0),
         'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'montantTva', coalesce(sla.montant_tva_lignes, 0),
         'paidAmount', coalesce(pa.total_paid_amount, 0),
         'realNetAmount', coalesce(pa.total_reel_amount, 0)
       )
from sales_agg sa
       cross join sales_line_agg sla
       cross join payment_agg pa;
$$;


-- sales_summary_by_type_json (dernière définition : V1.9.0__fix_tva_division_entiere.sql)
create or replace function sales_summary_by_type_json(p_start_date date, p_end_date date, p_statuts text[],
                                           p_cas text[],
                                           p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, sale_date, dtype
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)
                          and canceled = false),
     sales_line_agg as (select fs.dtype,
                               sum(sl.quantity_requested * sl.cost_amount)        as cost_amount,
                               sum(sl.quantity_requested * sl.regular_unit_price) as sales_amount,
                               round(sum((sl.quantity_requested * sl.regular_unit_price - coalesce(sl.discount_amount, 0)) /
                                           tva_divisor(sl.tax_value)))  as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum((sl.quantity_requested * sl.regular_unit_price - coalesce(sl.discount_amount, 0)))) - round(sum((sl.quantity_requested * sl.regular_unit_price - coalesce(sl.discount_amount, 0)) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes
                        from sales_line sl
                               join filtered_sales fs on fs.id = sl.sales_id
                        where sl.to_ignore = p_to_ignore
                          AND fs.sale_date = sl.sale_date
                        group by fs.dtype),
     payment_agg as (select fs.dtype,
                            sum(p.paid_amount) as total_paid_amount,
                            sum(p.reel_amount) as total_reel_amount
                     from payment_transaction p
                            join filtered_sales fs on fs.id = p.sale_id
                     where p.dtype = 'SalePayment'
                       AND fs.sale_date = p.sale_date
                     group by fs.dtype),
     sales_agg as (select fs.dtype,
                          sum(s.amount_to_be_paid)               as total_amount_to_be_paid,
                          sum(s.discount_amount)                 as total_discount_amount,
                          sum(s.amount_to_be_taken_into_account) as total_amount_to_account,
                          sum(s.part_assure)                     as total_part_assure,
                          sum(s.part_tiers_payant)               as total_part_tiers_payant,
                          sum(s.rest_to_pay)                     as total_rest_to_pay,
                          sum(s.payroll_amount)                  as total_payroll_amount,
                          count(distinct s.id)                   as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and fs.sale_date = s.sale_date
                   group by fs.dtype)
select jsonb_agg(
         jsonb_build_object(
           'type', sa.dtype,
           'salesAmount', coalesce(sla.sales_amount, 0),
           'amountToBePaid', sa.total_amount_to_be_paid,
           'discountAmount', sa.total_discount_amount,
           'amountToBeTakenIntoAccount', sa.total_amount_to_account,
           'netAmount', coalesce(sla.sales_amount, 0) - coalesce(sa.total_discount_amount, 0),
           'partAssure', coalesce(sa.total_part_assure, 0),
           'partTiersPayant', coalesce(sa.total_part_tiers_payant, 0),
           'restToPay', sa.total_rest_to_pay,
           'payrollAmount', sa.total_payroll_amount,
           'saleCount', sa.distinct_sales_count,
           'costAmount', coalesce(sla.cost_amount, 0),
           'montantHt', coalesce(sla.total_sales_excl_tax, 0),
           'montantTva', coalesce(sla.montant_tva_lignes, 0),
           'paidAmount', coalesce(pa.total_paid_amount, 0),
           'realNetAmount', coalesce(pa.total_reel_amount, 0)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


-- rapport_activite_vente_report (dernière définition : V1.9.0__fix_tva_division_entiere.sql)
create or replace function rapport_activite_vente_report(p_start_date date, p_end_date date, p_statuts text[],
                                              p_cas text[],
                                              p_exclude_free_qty boolean DEFAULT false,
                                              p_to_ignore boolean DEFAULT false) returns jsonb
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

                               round(sum(((case when p_exclude_free_qty then (sl.quantity_requested - sl.quantity_ug) else sl.quantity_requested end) * sl.regular_unit_price - coalesce(sl.discount_amount, 0)) / tva_divisor(sl.tax_value)))                                           as total_sales_excl_tax,

                               -- TVA collectée : le net TTC moins son HT, même arrondi que le HT.
                               round(sum(((case when p_exclude_free_qty then (sl.quantity_requested - sl.quantity_ug) else sl.quantity_requested end) * sl.regular_unit_price - coalesce(sl.discount_amount, 0)))) - round(sum(((case when p_exclude_free_qty then (sl.quantity_requested - sl.quantity_ug) else sl.quantity_requested end) * sl.regular_unit_price - coalesce(sl.discount_amount, 0)) / tva_divisor(sl.tax_value)))
                                                                           as montant_tva_lignes,

                               ceiling(sum(
                                 ((case
                                     when p_exclude_free_qty
                                       then (sl.quantity_requested - sl.quantity_ug)
                                     else sl.quantity_requested
                                   end) * sl.regular_unit_price) * sl.taux_remise
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
           'montantTva', coalesce(sla.montant_tva_lignes, 0),
         'payments', coalesce(pa.payments, '[]'::jsonb)
       )
from sales_agg sa
       cross join sales_line_agg sla
       left join payment_agg pa on true;
$$;


-- get_product_sales_summary (dernière définition : V1.9.0__fix_tva_division_entiere.sql)
create or replace function get_product_sales_summary(p_start_date date, p_end_date date, p_statuts text[],
                                          p_cas text[], p_produit_id integer,
                                          p_group_by integer DEFAULT 0) returns jsonb
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
                       ROUND(SUM((o.quantity_requested * o.regular_unit_price - coalesce(o.discount_amount, 0)) /
                                tva_divisor(o.tax_value)))    AS montant_ht,
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


-- get_historique_vente (dernière définition : V1.9.0__fix_tva_division_entiere.sql)
create or replace function get_historique_vente(p_produit_id integer, p_start_date date, p_end_date date,
                                     p_statuts text[], p_cas text[], p_offset integer DEFAULT 0,
                                     p_limit integer DEFAULT 20) returns jsonb
  stable
  language plpgsql
as
$$
BEGIN
  RETURN (WITH data AS (SELECT s.updated_at,
                               s.number_transaction,
                               o.quantity_requested,
                               o.regular_unit_price,
                               ROUND((o.quantity_requested * o.regular_unit_price - coalesce(o.discount_amount, 0)) /
                                    tva_divisor(o.tax_value)) AS ht_amount,
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
                        LIMIT p_limit OFFSET p_offset)
          SELECT jsonb_build_object(
                   'totalElements', (SELECT COUNT(*)
                                     FROM sales_line o
                                            JOIN sales s ON o.sales_id = s.id
                                     WHERE o.produit_id = p_produit_id
                                       AND s.statut = ANY (p_statuts)
                                       AND s.ca = ANY (p_cas)
                                       AND o.sales_sale_date = s.sale_date
                                       AND s.sale_date BETWEEN p_start_date AND p_end_date),
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
                       'montantTva', data.sales_amount - data.discount_amount - data.ht_amount,
                       'firstName', data.first_name,
                       'lastName', data.last_name
                     )
                              )
                 )
          FROM data);
END;
$$;
