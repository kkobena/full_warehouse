
UPDATE app_configuration set value = '90' ,value_type = 'NUMBER'   where name = 'APP_EXPIRY_ALERT_DAYS_BEFORE';
alter table avoir_fournisseur_line   alter column prix_achat type integer using prix_achat::integer;
