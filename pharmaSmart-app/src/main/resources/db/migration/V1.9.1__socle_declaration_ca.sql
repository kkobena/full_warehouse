-- =====================================================================================
-- Socle de la declaration du chiffre d'affaires : mode de lecture REEL / DECLARE
-- =====================================================================================
--
-- Le pharmacien doit pouvoir consulter deux chiffres sur les memes ecrans :
--   REEL    : le chiffre d'affaires encaisse, sans aucun retraitement.
--   DECLARE : celui qu'il declare a la comptabilite, une fois appliquees les exclusions
--             (rayon, tiers-payant, unites gratuites) et l'eventuelle ponction.
--
-- Plutot que de dupliquer les fonctions, les services et les ecrans -- deux implementations
-- a maintenir en parallele, qui divergeraient a la premiere evolution -- les fonctions
-- recoivent un parametre `p_mode`. Un seul jeu de SQL, deux lectures.
--
-- TROIS MONTANTS, UN SEUL RATIO
-- -----------------------------
-- Le montant declarable d'une ligne est un TTC. Le tableau pharmacien a besoin du cout
-- d'achat, le rapport TVA du HT : aucun ne se deduit d'un TTC seul. Plutot que de
-- materialiser trois colonnes qui pourraient diverger, les montants derives se calculent
-- a partir d'un unique ratio declarable / reel :
--
--     UG exclues   -> ratio = (qte - ug) / qte  -> cout ampute des unites gratuites
--     rayon exclu  -> ratio = 0                 -> cout nul
--     ponction     -> ratio partiel             -> taux de marge preserve
--
-- LES REGLEMENTS SUIVENT LE CHIFFRE D'AFFAIRES
-- --------------------------------------------
-- Sur une vente de 1 000 F dont 200 F d'UG, reglee 1 000 F en especes, afficher un CA de
-- 800 et un encaissement de 1 000 rend l'etat indefendable : l'ecart de 200 ne se rattache
-- a rien. Le montant declarable est donc porte aussi par payment_transaction, et les
-- rapports agregent cette colonne en mode DECLARE.
--
-- Les colonnes declarables sont nullables : NULL signifie « aucun retraitement ». Cela evite
-- une reprise de donnees et rend le cas courant lisible.
--
-- L'EXCLUSION DES UNITES GRATUITES N'EST PLUS UN PARAMETRE DE LECTURE
-- ------------------------------------------------------------------
-- Elle est appliquee une fois pour toutes a la cloture de la vente, et portee par
-- sales_line.amount_to_be_taken_into_account. En DECLARE les rapports lisent cette colonne :
-- la retrancher une seconde fois la compterait deux fois. En REEL, le chiffre encaisse inclut
-- les unites gratuites par definition. Aucun des deux modes n'a donc de raison de la recalculer,
-- et le parametre qui le permettait a ete retire des signatures plutot que laisse inerte.
--
-- POURQUOI LE DEFAUT VAUT REEL
-- ----------------------------
-- DECLARE rend un chiffre diminue. Un appelant qui oublie le parametre sous-declarerait en
-- silence -- et un chiffre trop bas ne se remarque pas, alors qu'un chiffre trop haut saute
-- aux yeux. L'etat neutre d'une fonction de rapport, c'est la donnee non retraitee : le
-- defaut vaut donc REEL, et DECLARE se demande explicitement, la ou l'intention metier
-- existe. Un defaut ne doit pas porter une decision fiscale.
--
-- Ce defaut ne gouverne que les appels SQL directs : la couche Java transmet toujours le
-- mode. C'est ModeChiffreAffaire.DEFAUT qui fait foi pour les appelants applicatifs.
--
-- DROP + CREATE
-- -------------
-- V1.9.0 a cree ces fonctions avec six parametres. Ajouter p_mode change la signature : un
-- simple CREATE OR REPLACE laisserait coexister l'ancienne fonction a six arguments et la
-- nouvelle a sept, et tout appel a six arguments deviendrait ambigu -- erreur a l'execution,
-- pas au deploiement. L'ancienne signature est donc supprimee explicitement.
--
-- Les cinq fonctions redefinies ici alimentent app-balance-mvt-caisse, app-taxe-report et
-- app-tableau-pharmacien.
-- =====================================================================================


-- Motif de reduction du montant declarable d'une ligne : RAYON, TIERS_PAYANT, UG, PONCTION, MANUEL.
alter table sales_line
  add column if not exists exclusion_motif varchar(20);

comment on column sales_line.exclusion_motif is
  'Pourquoi le montant declarable de la ligne est inferieur a son montant reel. NULL = aucun retraitement.';


-- Index couvrant : en mode DECLARE les rapports agregent amount_to_be_taken_into_account et
-- tax_value sur une plage de dates, sans lire le reste de la ligne -- qui est large.
create index if not exists sales_line_declare_idx
  on sales_line (sale_date)
  include (amount_to_be_taken_into_account, tax_value)
  where to_ignore = false;


alter table payment_transaction
  add column if not exists amount_to_be_taken_into_account integer;

comment on column payment_transaction.amount_to_be_taken_into_account is
  'Montant du reglement a declarer. NULL = aucun retraitement, donc paid_amount.';


-- Montant TTC d'une ligne selon le mode de lecture.
create or replace function ca_ttc_ligne(p_quantity_requested integer,
                                        p_regular_unit_price integer,
                                        p_declarable integer,
                                        p_mode text) returns numeric
  language sql
  immutable
  parallel safe
as
$$
select case
         when p_mode = 'DECLARE'
           then coalesce(p_declarable, p_quantity_requested * p_regular_unit_price)::numeric
         else (p_quantity_requested * p_regular_unit_price)::numeric
       end
$$;

comment on function ca_ttc_ligne(integer, integer, integer, text) is
  'Montant TTC d''une ligne : declarable en DECLARE, brut en REEL.';


-- Montant derive (cout d'achat, remise) pondere par le ratio declarable / reel.
create or replace function ca_pondere_ligne(p_montant numeric,
                                            p_quantity_requested integer,
                                            p_regular_unit_price integer,
                                            p_declarable integer,
                                            p_mode text) returns numeric
  language sql
  immutable
  parallel safe
as
$$
select case
         when p_mode <> 'DECLARE' or p_declarable is null
           then p_montant
         else p_montant * (p_declarable::numeric
                           / nullif(p_quantity_requested * p_regular_unit_price, 0))
       end
$$;

comment on function ca_pondere_ligne(numeric, integer, integer, integer, text) is
  'Applique a un montant derive le ratio declarable / reel de la ligne. Hors DECLARE, rend le montant tel quel.';


-- Montant d'un reglement selon le mode de lecture.
create or replace function ca_reglement(p_montant integer,
                                        p_declarable integer,
                                        p_mode text) returns integer
  language sql
  immutable
  parallel safe
as
$$
select case when p_mode = 'DECLARE' then coalesce(p_declarable, p_montant) else p_montant end
$$;

comment on function ca_reglement(integer, integer, text) is
  'Montant d''un reglement a declarer. Hors DECLARE, ou sans retraitement, rend le montant encaisse.';


drop function if exists sales_balance(date, date, text[], text[], boolean, boolean);

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
           'payments', coalesce(pa.payments, '[]'::jsonb)
         )
       )
from sales_agg sa
       join sales_line_agg sla on sa.dtype = sla.dtype
       left join payment_agg pa on sa.dtype = pa.dtype;
$$;


drop function if exists sales_tva_report(date, date, text[], text[], boolean, boolean);

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


drop function if exists sales_tva_report_journalier(date, date, text[], text[], boolean, boolean);

create or replace function sales_tva_report_journalier(p_start_date date, p_end_date date, p_statuts text[],
                                            p_cas text[],                                            p_to_ignore boolean DEFAULT false,
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


drop function if exists tableau_pharmacien_report(date, date, text[], text[], boolean, boolean);

create or replace function tableau_pharmacien_report(p_start_date date, p_end_date date, p_statuts text[],
                                          p_cas text[],                                          p_to_ignore boolean DEFAULT false,
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


drop function if exists tableau_pharmacien_month_report(date, date, text[], text[], boolean, boolean);

create or replace function tableau_pharmacien_month_report(p_start_date date, p_end_date date,
                                                p_statuts text[], p_cas text[],
                                                                  p_to_ignore boolean DEFAULT false,
                              p_mode text DEFAULT 'REEL') returns jsonb
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
