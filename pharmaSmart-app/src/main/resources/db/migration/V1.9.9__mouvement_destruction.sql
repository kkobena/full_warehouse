-- ============================================================================
-- V1.9.9 — Le mouvement DESTRUCTION manquait à la contrainte
--
-- `MouvementProduit` déclare DESTRUCTION depuis l'ajout du retour client : un
-- produit thermosensible, ou dont l'état constaté est défaillant, est remboursé
-- au client SANS revenir en stock — le mouvement enregistré est sa destruction.
--
-- La contrainte CHECK de `inventory_transaction.mouvement_type`, elle, n'avait
-- pas suivi : tout retour de ce type échouait à l'insertion, et l'écran
-- répondait « Une erreur est survenue » sans dire laquelle. Seuls les retours
-- remis en stock passaient — c'est-à-dire exactement ceux qui ne posent pas de
-- question.
--
-- La liste est reconstruite à partir de l'énumération, pour qu'elles ne
-- divergent plus.
-- ============================================================================

ALTER TABLE inventory_transaction
    DROP CONSTRAINT IF EXISTS inventory_transaction_mouvement_type_check;

ALTER TABLE inventory_transaction
    ADD CONSTRAINT inventory_transaction_mouvement_type_check
    CHECK (mouvement_type IN (
        'SALE', 'DELETE_SALE', 'CANCEL_SALE',
        'AJUSTEMENT_IN', 'AJUSTEMENT_OUT', 'INVENTAIRE', 'COMMANDE',
        'DECONDTION_IN', 'DECONDTION_OUT',
        'MOUVEMENT_STOCK_IN', 'MOUVEMENT_STOCK_OUT',
        'ENTREE_STOCK', 'RETRAIT_PERIME',
        'RETOUR_DEPOT', 'RETOUR_FOURNISSEUR', 'RETOUR_CLIENT',
        'DESTRUCTION'
    ));
