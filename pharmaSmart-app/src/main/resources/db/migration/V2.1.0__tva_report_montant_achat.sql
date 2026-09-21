-- L'etat de TVA n'emettait pas le cout d'achat des produits vendus.
--
-- Consequence : TaxeDTO.montantAchat restait a zero, la TVA deductible calculee par
-- TaxeServiceImpl.fetchDeclarationTva valait donc toujours zero, et la TVA nette se confondait
-- avec la TVA collectee. La declaration reclamait ainsi la totalite de la taxe encaissee sans
-- deduire celle payee aux grossistes.
--
-- Le cout retenu est le meme que celui de la balance de caisse : ca_pondere_ligne, qui applique
-- au cout d'achat la meme ponderation de mode que le chiffre d'affaires de la ligne. Deux etats
-- qui portent le meme nom doivent porter le meme nombre.

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
           'montantHt', coalesce(sla.total_sales_excl_tax, 0)
         )
           order by sla.tax_value
       )
from sales_line_agg sla;
$$;


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
                                   ca_ttc_ligne(sl.quantity_requested, sl.regular_unit_price, sl.amount_to_be_taken_into_account, p_mode)
                                     / tva_divisor(sl.tax_value)
                                 )
                               )                                           as total_sales_excl_tax,

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
           'montantHt', coalesce(sla.total_sales_excl_tax, 0)
         )
           order by sla.sale_date, sla.tax_value
       )
from sales_line_agg sla;
$$;
