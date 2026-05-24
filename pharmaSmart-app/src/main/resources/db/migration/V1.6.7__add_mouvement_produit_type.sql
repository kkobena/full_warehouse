ALTER TABLE inventory_transaction
DROP CONSTRAINT inventory_transaction_mouvement_type_check;

ALTER TABLE inventory_transaction
  ADD CONSTRAINT inventory_transaction_mouvement_type_check
    CHECK (
      mouvement_type IN (
                       'SALE',
                       'CANCEL_SALE',
                       'DELETE_SALE',
                       'AJUSTEMENT_IN',
                       'INVENTAIRE',
                       'AJUSTEMENT_OUT',
                       'COMMANDE',
                       'DECONDTION_IN',
                       'DECONDTION_OUT',
                       'MOUVEMENT_STOCK_IN',
                       'RETOUR_DEPOT',
                       'MOUVEMENT_STOCK_OUT',
                       'ENTREE_STOCK',
                       'RETRAIT_PERIME',
                       'RETOUR_FOURNISSEUR',
                       'RETOUR_CLIENT'
        )
      );
