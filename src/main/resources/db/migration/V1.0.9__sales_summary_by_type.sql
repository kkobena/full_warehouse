create or replace function sales_summary_by_type_json(
  p_start_date date,
  p_end_date date,
  p_statuts text[],
  p_cas text[],
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
    and statut = any(p_statuts)
    and ca = any(p_cas)
),
     sales_line_agg as (
       select fs.dtype,
              sum(sl.quantity_requested * sl.cost_amount) as cost_amount,
              sum(sl.quantity_requested * sl.regular_unit_price) as sales_amount,
              ceiling(sum((sl.quantity_requested * sl.regular_unit_price) / nullif(1 + (sl.tax_value / 100),0))) as total_sales_excl_tax
       from sales_line sl
              join filtered_sales fs on fs.id = sl.sales_id
       where sl.to_ignore = p_to_ignore AND fs.sale_date=sl.sale_date
       group by fs.dtype
     ),
     payment_agg as (
       select fs.dtype,
              sum(p.paid_amount) as total_paid_amount,
              sum(p.reel_amount) as total_reel_amount
       from payment_transaction p
              join filtered_sales fs on fs.id = p.sale_id
       where p.dtype = 'SalePayment' AND fs.sale_date=p.sale_date
       group by fs.dtype
     ),
     sales_agg as (
       select fs.dtype,
              sum(s.amount_to_be_paid) as total_amount_to_be_paid,
              sum(s.discount_amount) as total_discount_amount,
              sum(s.amount_to_be_taken_into_account) as total_amount_to_account,
              sum(s.part_assure) as total_part_assure,
              sum(s.part_tiers_payant) as total_part_tiers_payant,
              sum(s.rest_to_pay) as total_rest_to_pay,
              sum(s.payroll_amount) as total_payroll_amount,
              count(distinct s.id) as distinct_sales_count
       from sales s
              join filtered_sales fs on fs.id = s.id and fs.sale_date=s.sale_date
       group by fs.dtype
     )
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
