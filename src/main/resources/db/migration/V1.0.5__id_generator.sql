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
