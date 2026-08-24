-- =====================================================================================
-- Correction du calcul du montant hors taxe : division entiere
-- =====================================================================================
--
-- PROBLEME
-- --------
-- sales_line.tax_value est declaree `integer` (V1.0.1__init.sql) et Tva.taux vaut 0, 9 ou 18.
-- Les fonctions de rapport calculaient le HT par `ttc / (1 + (tax_value / 100))`.
-- En PostgreSQL `18 / 100` est une division ENTIERE : elle vaut 0. Le diviseur valait donc 1
-- pour tous les taux, d'ou :
--     montantHt   = montantTtc
--     montantTaxe = montantTtc - montantHt = 0
-- Autrement dit le rapport TVA declarait zero TVA collectee, quel que soit le taux.
--
-- CORRECTION
-- ----------
-- 1. `tva_divisor(taux)` : le diviseur (1 + taux/100) calcule en `numeric`.
--    `numeric` et non `double precision` : sur des montants qui alimentent une declaration
--    fiscale on ne veut pas d'arithmetique binaire approchee, et `sum()` doit etre
--    reproductible independamment de l'ordre de lecture.
-- 2. Les 16 divisions des 10 fonctions ci-dessous passent par cette fonction. La formule
--    n'existe plus qu'a un seul endroit : toute reintroduction du bug est visible par
--    `grep tva_divisor`.
-- 3. `ceiling` -> `round` sur les seules expressions de HT. Arrondir le HT au-dessus
--    revenait a arrondir la TVA (ttc - ht) en dessous, donc a la sous-declarer d'un franc
--    par agregat. Les `ceiling` des montants de remise sont volontairement inchanges.
--
-- Le type des colonnes n'est PAS modifie : `integer` est le type juste pour un taux entier.
-- Un eventuel taux fractionnaire (5,5 %) releverait de `numeric(5,2)`, jamais de `float`.
--
-- FONCTIONS REDEFINIES (elles remplacent les definitions de V1.0.5__procedure.sql, qui
-- reste inchange car deja execute en base). Ce fichier devient la source de verite :
-- toute evolution ulterieure de ces fonctions part d'ICI, pas de V1.0.5.
--
--   sales_summary_json                sales_tva_report                 tableau_pharmacien_month_report
--   sales_balance                     sales_tva_report_journalier      rapport_activite_vente_report
--   sales_summary_by_type_json        tableau_pharmacien_report        get_historique_vente
--                                                                      get_product_sales_summary
--
-- =====================================================================================


-- Diviseur de TVA : 1 + taux/100, en numeric. NULL si le taux rend le diviseur nul.
create or replace function tva_divisor(p_taux integer) returns numeric
  language sql
  immutable
  parallel safe
as
$$
select nullif(1 + p_taux::numeric / 100, 0)
$$;

comment on function tva_divisor(integer) is
  'Diviseur de TVA (1 + taux/100) en numeric. A utiliser partout : ecrire la division en clair '
  'reintroduit la division entiere corrigee par V1.9.0.';


-- Montant hors taxe correspondant a un montant TTC, pour un taux en pourcentage entier.
create or replace function ht_from_ttc(p_ttc numeric, p_taux integer) returns numeric
  language sql
  immutable
  parallel safe
as
$$
select p_ttc / tva_divisor(p_taux)
$$;

comment on function ht_from_ttc(numeric, integer) is
  'Montant HT a partir du TTC. Non arrondi : l''arrondi appartient a l''appelant, arrondir ici '
  'puis sommer accumulerait les erreurs ligne a ligne.';


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
                          round(sum((sl.quantity_requested * sl.regular_unit_price) /
                                      tva_divisor(sl.tax_value)))           as total_sales_excl_tax

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
         'paidAmount', coalesce(pa.total_paid_amount, 0),
         'realNetAmount', coalesce(pa.total_reel_amount, 0)
       )
from sales_agg sa
       cross join sales_line_agg sla
       cross join payment_agg pa;
$$;


create or replace function sales_balance(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                              p_exclude_free_qty boolean DEFAULT false,
                              p_to_ignore boolean DEFAULT false) returns jsonb
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

                               round(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / tva_divisor(sl.tax_value)
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
                                  sum(p.paid_amount) as total_paid_amount,
                                  sum(p.reel_amount) as total_real_amount
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
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


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
                               round(sum((sl.quantity_requested * sl.regular_unit_price) /
                                           tva_divisor(sl.tax_value)))  as total_sales_excl_tax
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
           'paidAmount', coalesce(pa.total_paid_amount, 0),
           'realNetAmount', coalesce(pa.total_reel_amount, 0)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


create or replace function sales_tva_report(p_start_date date, p_end_date date, p_statuts text[], p_cas text[],
                                 p_exclude_free_qty boolean DEFAULT false,
                                 p_to_ignore boolean DEFAULT false) returns jsonb
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
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / tva_divisor(sl.tax_value)
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
           'montantHt', coalesce(sla.total_sales_excl_tax, 0)
         )
           order by sla.tax_value
       )
from sales_line_agg sla;
$$;


create or replace function sales_tva_report_journalier(p_start_date date, p_end_date date, p_statuts text[],
                                            p_cas text[], p_exclude_free_qty boolean DEFAULT false,
                                            p_to_ignore boolean DEFAULT false) returns jsonb
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
                                 (case
                                    when p_exclude_free_qty
                                      then (sl.quantity_requested - sl.quantity_ug)
                                    else sl.quantity_requested
                                   end) * sl.regular_unit_price
                               )                                           as sales_amount,

                               round(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / tva_divisor(sl.tax_value)
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
           'montantHt', coalesce(sla.total_sales_excl_tax, 0)
         )
           order by sla.sale_date, sla.tax_value
       )
from sales_line_agg sla;
$$;


create or replace function tableau_pharmacien_report(p_start_date date, p_end_date date, p_statuts text[],
                                          p_cas text[], p_exclude_free_qty boolean DEFAULT false,
                                          p_to_ignore boolean DEFAULT false) returns jsonb
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

                               sum(
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

                               round(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / tva_divisor(sl.tax_value)
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
                                  sum(p.paid_amount) as total_paid_amount,
                                  sum(p.reel_amount) as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND p.sale_date = fs.sale_date
                           group by fs.sale_date, pm.code, pm.libelle) t
                     group by t.sale_date),
     sales_agg as (select fs.sale_date,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay)                                         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id and s.sale_date = fs.sale_date
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
$$;


create or replace function tableau_pharmacien_month_report(p_start_date date, p_end_date date,
                                                p_statuts text[], p_cas text[],
                                                p_exclude_free_qty boolean DEFAULT false,
                                                p_to_ignore boolean DEFAULT false) returns jsonb
  language sql
as
$$
with filtered_sales as (select id, date_trunc('month', sale_date)::date as month_date
                        from sales
                        where sale_date between p_start_date and p_end_date
                          and imported = false
                          and statut = any (p_statuts)
                          and ca = any (p_cas)),
     sales_line_agg as (select fs.month_date,

                               sum(
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

                               round(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / tva_divisor(sl.tax_value)
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
                                  sum(p.paid_amount)                       as total_paid_amount,
                                  sum(p.reel_amount)                       as total_real_amount
                           from payment_transaction p
                                  join filtered_sales fs on fs.id = p.sale_id
                                  join payment_mode pm on pm.code = p.payment_mode_code
                           where p.dtype = 'SalePayment'
                             AND date_trunc('month', p.sale_date)::date = fs.month_date
                           group by date_trunc('month', fs.month_date), pm.code, pm.libelle) t
                     group by t.month_date),
     sales_agg as (select fs.month_date,
                          sum(s.discount_amount)                                     as total_discount_amount,
                          sum(s.part_tiers_payant)                                   as total_part_tiers_payant,
                          sum(s.rest_to_pay)                                         as total_rest_to_pay,
                          count(distinct case when s.canceled = false then s.id end) as distinct_sales_count
                   from sales s
                          join filtered_sales fs on fs.id = s.id
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
$$;


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

                               round(
                                 sum(
                                   (case
                                      when p_exclude_free_qty
                                        then (sl.quantity_requested - sl.quantity_ug)
                                      else sl.quantity_requested
                                     end) * sl.regular_unit_price
                                     / tva_divisor(sl.tax_value)
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
         'payments', coalesce(pa.payments, '[]'::jsonb)
       )
from sales_agg sa
       cross join sales_line_agg sla
       left join payment_agg pa on true;
$$;


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
                               ROUND((o.quantity_requested * o.regular_unit_price) /
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
                       'montantTva', data.sales_amount - data.ht_amount,
                       'firstName', data.first_name,
                       'lastName', data.last_name
                     )
                              )
                 )
          FROM data);
END;
$$;


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
                       ROUND(SUM((o.quantity_requested * o.regular_unit_price) /
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
