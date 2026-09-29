-- Trace du recrédit de stock quand un avoir est soldé sans remise du produit (remboursement, bon, compensation)
ALTER TABLE logs
  DROP CONSTRAINT logs_transaction_type_check;

ALTER TABLE logs
  ADD CONSTRAINT logs_transaction_type_check
    CHECK ((transaction_type)::text = ANY
  ((ARRAY ['SALE'::character varying, 'DELETE_SALE'::character varying, 'CANCEL_SALE'::character varying, 'REAPPRO'::character varying, 'AJUSTEMENT_IN'::character varying, 'AJUSTEMENT_OUT'::character varying, 'INVENTAIRE'::character varying, 'SUPPRESSION'::character varying, 'COMMANDE'::character varying, 'DECONDTION_IN'::character varying, 'DECONDTION_OUT'::character varying, 'CREATE_PRODUCT'::character varying, 'UPDATE_PRODUCT'::character varying, 'DELETE_PRODUCT'::character varying, 'DISABLE_PRODUCT'::character varying, 'ENABLE_PRODUCT'::character varying, 'MODIFICATION_PRIX_PRODUCT'::character varying, 'MOUVEMENT_STOCK_IN'::character varying, 'MOUVEMENT_STOCK_OUT'::character varying, 'FORCE_STOCK'::character varying, 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE'::character varying, 'ENTREE_STOCK'::character varying, 'ACTIVATION_PRIVILEGE'::character varying, 'RETRAIT_PERIME'::character varying, 'MODIFICATION_DATE_DE_VENTE'::character varying, 'MODIFICATION_INFO_CLIENT'::character varying, 'MERGE_PRODUCT'::character varying, 'AVOIR_SOLDE_SANS_PRODUIT'::character varying])::text[]));
