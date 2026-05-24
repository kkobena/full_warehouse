ALTER TABLE substitution_proposee
  ADD COLUMN IF NOT EXISTS code_reponse      VARCHAR(10),
  ADD COLUMN IF NOT EXISTS additif           VARCHAR(500),
  ADD COLUMN IF NOT EXISTS type_remplacement VARCHAR(3);


ALTER TABLE ventes_mensuelles_agregees
    ADD COLUMN IF NOT EXISTS est_rupture_fournisseur BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN ventes_mensuelles_agregees.est_rupture_fournisseur
    IS 'TRUE si le produit avait une rupture fournisseur encore active (product_still_out_of_stock=TRUE) '
       'débutée avant la fin du mois. Exclu du calcul VMM SEMOIS. '
       'Remis à FALSE automatiquement quand la rupture est résolue.';


CREATE INDEX IF NOT EXISTS idx_ventes_mensuelles_rupture_produit
    ON ventes_mensuelles_agregees (produit_id, est_rupture_fournisseur, annee_mois);


UPDATE ventes_mensuelles_agregees vma
SET est_rupture_fournisseur = TRUE,
    updated_at              = NOW()
WHERE EXISTS (
    SELECT 1
    FROM   rupture r
    WHERE  r.produit_id                 = vma.produit_id
      AND  r.product_still_out_of_stock = TRUE
      AND  r.date_mtv   < (TO_DATE(vma.annee_mois, 'YYYY-MM') + INTERVAL '1 month')
);
INSERT INTO app_configuration (name, description, value, created, updated, value_type)
VALUES (
         'APP_ACCEPTATION_SUBSTITUTION',
         'Mode d''acceptation des substitutions PharmaML (EP) : AUTO = acceptation implicite et mémorisation, MANUEL = validation pharmacien',
         'MANUEL',
         NOW(),
         NOW(),'STRING'
       )
  ON CONFLICT (name) DO NOTHING;
ALTER TABLE semois_configuration
  ADD COLUMN IF NOT EXISTS facteur_saisonnier_manuel BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN semois_configuration.facteur_saisonnier_manuel IS
    'TRUE si le facteur saisonnier a été saisi manuellement par le pharmacien '
    '(l''auto-calcul SEMOIS ne l''écrasera pas). '
    'FALSE = valeur calculée automatiquement depuis l''historique 24 mois.';
ALTER TABLE groupe_fournisseur
  ADD COLUMN IF NOT EXISTS frequence_commande_jours INTEGER NOT NULL DEFAULT 7;

COMMENT ON COLUMN groupe_fournisseur.frequence_commande_jours IS
    'Fréquence de commande en jours (ex: 1=quotidien, 7=hebdo, 14=bimensuel). '
    'Utilisée par SEMOIS pour calculer le stock de rotation.';

-- Surcharge par fournisseur individuel (nullable = utiliser le groupe)
-- Même logique que delai_livraison_jours sur fournisseur
ALTER TABLE fournisseur
  ADD COLUMN IF NOT EXISTS frequence_commande_jours INTEGER NULL;

COMMENT ON COLUMN fournisseur.frequence_commande_jours IS
    'Fréquence de commande en jours — surcharge la fréquence du groupe si renseignée.';

-- Surcharge par produit dans semois_configuration (nullable = pas de surcharge)
ALTER TABLE semois_configuration
  ADD COLUMN IF NOT EXISTS frequence_commande_jours INTEGER NULL;

COMMENT ON COLUMN semois_configuration.frequence_commande_jours IS
    'Surcharge de la fréquence de commande pour ce produit spécifique. '
    'NULL = utiliser la fréquence du fournisseur ou du groupe (ou défaut 7 jours).';
ALTER TABLE commande
DROP CONSTRAINT commande_order_status_check;

ALTER TABLE commande
  ADD CONSTRAINT commande_order_status_check
    CHECK (
      order_status IN (
                 'REQUESTED',
                 'RECEIVED',
                 'CLOSED',
                 'ARCHIVED'
        )
      );
