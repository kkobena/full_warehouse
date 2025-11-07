CREATE SEQUENCE id_sale_seq START 1;
alter sequence id_sale_seq owner to pharma_smart;

CREATE SEQUENCE id_sale_assurance_item_seq START 1;
alter sequence id_sale_assurance_item_seq owner to pharma_smart;

CREATE SEQUENCE id_sale_item_seq START 1;
alter sequence id_sale_item_seq owner to pharma_smart;


CREATE SEQUENCE id_order_line_seq START 1;
alter sequence id_order_line_seq owner to pharma_smart;


CREATE SEQUENCE id_commande_seq START 1;
alter sequence id_commande_seq owner to pharma_smart;


CREATE SEQUENCE id_facture_seq START 1;
alter sequence id_facture_seq owner to pharma_smart;

CREATE SEQUENCE id_facture_item_seq START 1;
alter sequence id_facture_item_seq owner to pharma_smart;


CREATE SEQUENCE id_transaction_item_seq START 1;
alter sequence id_transaction_item_seq owner to pharma_smart;

CREATE SEQUENCE id_transaction_seq START 1;
alter sequence id_transaction_seq owner to pharma_smart;

CREATE SEQUENCE id_mvt_produit_seq START 1;
alter sequence id_mvt_produit_seq owner to pharma_smart;


alter sequence magasin_id_seq restart with 5;
alter sequence storage_id_seq restart with 10;
alter sequence tableau_id_seq restart with 10;
alter sequence rayon_id_seq restart with 10;
alter sequence groupe_fournisseur_id_seq restart with 20;
alter sequence form_produit_id_seq restart with 100;
alter sequence tva_id_seq restart with 100;
alter sequence app_user_id_seq restart with 100;
