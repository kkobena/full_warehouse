
ALTER TABLE lot
    ALTER COLUMN order_line_id     DROP NOT NULL;

ALTER TABLE lot
    ALTER COLUMN commande_order_date DROP NOT NULL;

COMMENT ON COLUMN lot.order_line_id IS
    'Ligne de commande source. NULL pour les lots saisis hors commande '
    '(inventaire initial, correction d''inventaire, import CSV).';

COMMENT ON COLUMN lot.commande_order_date IS
    'Date de la commande source (clé composite avec order_line_id). '
    'NULL si order_line_id est NULL.';



ALTER TABLE retour_bon
    ALTER COLUMN commande_id          DROP NOT NULL;

ALTER TABLE retour_bon
    ALTER COLUMN commande_order_date  DROP NOT NULL;

-- Fournisseur direct : renseigné uniquement quand hors_commande = true
ALTER TABLE retour_bon
    ADD COLUMN IF NOT EXISTS fournisseur_id INTEGER
        REFERENCES fournisseur (id);

-- Flag traçabilité : visible dans l'UI (badge "Hors commande") et dans le PDF BR
ALTER TABLE retour_bon
    ADD COLUMN IF NOT EXISTS hors_commande BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN retour_bon.commande_id IS
    'Commande source. NULL si le retour concerne un lot hors entrée en stock.';

COMMENT ON COLUMN retour_bon.commande_order_date IS
    'Date de la commande source. NULL si hors_commande = true.';

COMMENT ON COLUMN retour_bon.fournisseur_id IS
    'Fournisseur direct du retour (renseigné quand hors_commande = true). '
    'Déduit de FournisseurProduit.principal lors de la création.';

COMMENT ON COLUMN retour_bon.hors_commande IS
    'true = bon de retour créé sans référence à une commande fournisseur '
    '(lot issu d''un inventaire, d''un ajustement ou d''une migration).';

-- Index pour les requêtes de filtrage/suivi UI
CREATE INDEX IF NOT EXISTS idx_retour_bon_fournisseur
    ON retour_bon (fournisseur_id);

CREATE INDEX IF NOT EXISTS idx_retour_bon_hors_commande
    ON retour_bon (hors_commande)
    WHERE hors_commande = TRUE;


ALTER TABLE retour_bon_item
    ALTER COLUMN orderline_id         DROP NOT NULL;

ALTER TABLE retour_bon_item
    ALTER COLUMN orderline_order_date DROP NOT NULL;

COMMENT ON COLUMN retour_bon_item.orderline_id IS
    'Ligne de commande source. NULL pour les items d''un retour hors commande.';

COMMENT ON COLUMN retour_bon_item.orderline_order_date IS
    'Date de la commande source. NULL si orderline_id est NULL.';


