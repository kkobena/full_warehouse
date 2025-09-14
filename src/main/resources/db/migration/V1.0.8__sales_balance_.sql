create or replace function sales_balance(
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
  select id, sale_date, dtype
  from sales
  where sale_date between p_start_date and p_end_date
    and imported = false
    and statut = any (p_statuts)
    and ca = any (p_cas)
),
     sales_line_agg as (
       select fs.dtype,
              sum(
                (case
                   when p_exclude_free_qty
                     then (sl.quantity_requested - sl.quantity_ug)
                   else sl.quantity_requested
                  end) * sl.cost_amount
              ) as cost_amount,

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
       group by fs.dtype
     ),
     payment_agg as (
       select dtype,
              jsonb_agg(
                jsonb_build_object(
                  'code', code,
                  'libelle', libelle,
                  'paidAmount', total_paid_amount,
                  'realAmount', total_real_amount
                )
              ) as payments
       from (
              select fs.dtype,
                     pm.code,
                     pm.libelle,
                     sum(p.paid_amount) as total_paid_amount,
                     sum(p.reel_amount) as total_real_amount
              from payment_transaction p
                     join filtered_sales fs on fs.id = p.sale_id
                     join payment_mode pm on pm.code = p.payment_mode_code
              where p.dtype = 'SalePayment' AND p.sale_date= fs.sale_date
              group by fs.dtype, pm.code, pm.libelle
            ) t
       group by dtype
     ),
     sales_agg as (
       select fs.dtype,
              sum(s.amount_to_be_paid)     as total_amount_to_be_paid,
              sum(s.discount_amount)       as total_discount_amount,
              sum(s.part_assure)           as total_part_assure,
              sum(s.part_tiers_payant)     as total_part_tiers_payant,
              sum(s.rest_to_pay)           as total_rest_to_pay,
              count(distinct s.id)         as distinct_sales_count
       from sales s
              join filtered_sales fs on fs.id = s.id and s.sale_date= fs.sale_date
       group by fs.dtype
     )
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
