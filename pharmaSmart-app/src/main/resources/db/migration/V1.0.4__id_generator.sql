CREATE SEQUENCE id_sale_seq START 1;


CREATE SEQUENCE id_sale_assurance_item_seq START 1;


CREATE SEQUENCE id_sale_item_seq START 1;



CREATE SEQUENCE id_order_line_seq START 1;



CREATE SEQUENCE id_commande_seq START 1;



CREATE SEQUENCE id_facture_seq START 1;


CREATE SEQUENCE id_facture_item_seq START 1;



CREATE SEQUENCE id_transaction_item_seq START 1;


CREATE SEQUENCE id_transaction_seq START 1;


CREATE SEQUENCE id_mvt_produit_seq START 1;


CREATE SEQUENCE invoice_generation_code_seq START 1;


alter sequence magasin_id_seq restart with 5;
alter sequence storage_id_seq restart with 10;
alter sequence tableau_id_seq restart with 10;
alter sequence rayon_id_seq restart with 10;
alter sequence groupe_fournisseur_id_seq restart with 20;
alter sequence form_produit_id_seq restart with 100;
alter sequence tva_id_seq restart with 100;
alter sequence app_user_id_seq restart with 100;


INSERT INTO scheduled_report (
  report_name,
  report_type,
  frequency,
  execution_time,
  day_of_week,
  email_recipients,
  active,
  next_execution,
  include_pdf,
  include_excel
) VALUES
    (
      'Rapport Quotidien - Alertes Stock',
      'STOCK_ALERTS',
      'DAILY',
      '08:00:00',
      NULL,
      '["badoukobena@gmail.com"]'::jsonb,
      FALSE, -- Disabled by default
      CURRENT_TIMESTAMP + INTERVAL '1 day',
      TRUE,TRUE
    ),
    (
      'Rapport Hebdomadaire - CA',
      'DASHBOARD_CA',
      'WEEKLY',
      '09:00:00',
      1, -- Monday
      '["badoukobena@gmail.com"]'::jsonb,
      FALSE,
      CURRENT_TIMESTAMP + INTERVAL '1 week',
      TRUE,TRUE
    ),
    (
      'Rapport Mensuel - Créances Tiers-Payants',
      'TIERS_PAYANT_CREANCES',
      'MONTHLY',
      '10:00:00',
      NULL,
      '["badoukobena@gmail.com"]'::jsonb,
      FALSE,
      CURRENT_TIMESTAMP + INTERVAL '1 month',
      TRUE,TRUE
    );
