create or replace function sales_tva_report(
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
with filtered_sales as (
  select id, sale_date
  from sales
  where sale_date between p_start_date and p_end_date
    and imported = false
    and statut = any (p_statuts)
    and ca = any (p_cas)
),
     sales_line_agg as (
       select sl.tax_value,
              sum(
                (case
                   when p_exclude_free_qty
                     then (sl.quantity_requested - sl.quantity_ug)
                   else sl.quantity_requested
                  end) * sl.regular_unit_price
              ) as sales_amount,

              ceiling(
                sum(
                  (case
                     when p_exclude_free_qty
                       then (sl.quantity_requested - sl.quantity_ug)
                     else sl.quantity_requested
                    end) * sl.regular_unit_price
                    / nullif(1 + (sl.tax_value / 100), 0)
                )
              ) as total_sales_excl_tax,

              ceiling(sum(
                ((case
                    when p_exclude_free_qty
                      then (sl.quantity_requested - sl.quantity_ug)
                    else sl.quantity_requested
                  end) * sl.regular_unit_price) * sl.taux_remise
                      )) as remise_produit,

              sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

              ceiling(
                sum(
                  (sl.quantity_ug * sl.regular_unit_price)
                    / nullif(1 + (sl.tax_value / 100), 0)
                )
              ) as total_sales_ug_tax,

              ceiling(sum(
                (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                      )) as remise_ug_produit

       from sales_line sl
              join filtered_sales fs on fs.id = sl.sales_id
       where sl.to_ignore = p_to_ignore AND sl.sale_date= fs.sale_date
       group by sl.tax_value
     )
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



create or replace function sales_tva_report_journalier(
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
with filtered_sales as (
  select id, sale_date
  from sales
  where sale_date between p_start_date and p_end_date
    and imported = false
    and statut = any (p_statuts)
    and ca = any (p_cas)
),
     sales_line_agg as (
       select
         fs.sale_date,
         sl.tax_value,

         sum(
           (case
              when p_exclude_free_qty
                then (sl.quantity_requested - sl.quantity_ug)
              else sl.quantity_requested
             end) * sl.regular_unit_price
         ) as sales_amount,

         ceiling(
           sum(
             (case
                when p_exclude_free_qty
                  then (sl.quantity_requested - sl.quantity_ug)
                else sl.quantity_requested
               end) * sl.regular_unit_price
               / nullif(1 + (sl.tax_value / 100), 0)
           )
         ) as total_sales_excl_tax,

         ceiling(sum(
           ((case
               when p_exclude_free_qty
                 then (sl.quantity_requested - sl.quantity_ug)
               else sl.quantity_requested
             end) * sl.regular_unit_price) * sl.taux_remise
                 )) as remise_produit,

         sum(sl.quantity_ug * sl.regular_unit_price) as sales_ug_amount,

         ceiling(
           sum(
             (sl.quantity_ug * sl.regular_unit_price)
               / nullif(1 + (sl.tax_value / 100), 0)
           )
         ) as total_sales_ug_tax,

         ceiling(sum(
           (sl.quantity_ug * sl.regular_unit_price) * sl.taux_remise
                 )) as remise_ug_produit

       from sales_line sl
              join filtered_sales fs on fs.id = sl.sales_id
       where sl.to_ignore = p_to_ignore AND sl.sale_date= fs.sale_date
       group by fs.sale_date, sl.tax_value
     )
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

