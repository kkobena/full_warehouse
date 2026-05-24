
CREATE TABLE semois_classe_config (
                                    classe_criticite VARCHAR(10) NOT NULL,
                                    coefficient_securite NUMERIC(3,2) NOT NULL,
                                    nb_mois_historique INTEGER NOT NULL,
                                    limite_peremption BOOLEAN NOT NULL DEFAULT false,
                                    CONSTRAINT pk_semois_classe_config PRIMARY KEY (classe_criticite)
);

INSERT INTO semois_classe_config (classe_criticite, coefficient_securite, nb_mois_historique, limite_peremption) VALUES
                                                                                                                   ('A_PLUS', 1.5, 6, false),
                                                                                                                   ('A',      1.0, 6, false),
                                                                                                                   ('B',      0.7, 6, false),
                                                                                                                   ('C',      0.4, 3, true),
                                                                                                                   ('D',      0.2, 3, true);
-- Supprimer la vue matérialisée en premier : elle dépend de coefficient_securite / nb_mois_historique
DROP MATERIALIZED VIEW IF EXISTS mv_semois_suggestion CASCADE;

ALTER TABLE semois_configuration
DROP COLUMN IF EXISTS coefficient_securite,
    DROP COLUMN IF EXISTS nb_mois_historique;

-- limite_peremption: false = valeur par défaut héritée de la classe → mettre NULL
UPDATE semois_configuration SET limite_peremption = NULL WHERE limite_peremption = false;

COMMENT ON TABLE semois_classe_config IS 'Configuration SEMOIS par classe de criticité (coefficient, nb mois, limite péremption). Remplace les champs par-produit dans semois_configuration.';
-- Valeur 0 = illimité (aucune contrainte budgétaire)
INSERT INTO app_configuration (name, value, description, value_type, updated)
VALUES ('APP_BUDGET_MENSUEL_COMMANDE', '0', 'Budget mensuel alloué aux commandes fournisseurs (0 = illimité)', 'NUMBER', NOW())
  ON CONFLICT (name) DO NOTHING;

ALTER TABLE suggestion
DROP CONSTRAINT suggestion_statut_check;
ALTER TABLE suggestion ALTER COLUMN statut TYPE VARCHAR(30);


UPDATE suggestion SET statut = 'GENEREE'   WHERE statut = 'OPEN';
UPDATE suggestion SET statut = 'COMMANDEE' WHERE statut = 'CLOSED';


ALTER TABLE suggestion
  ADD COLUMN IF NOT EXISTS valide_par_id   INTEGER,
  ADD COLUMN IF NOT EXISTS date_validation TIMESTAMP;

ALTER TABLE suggestion
  ADD CONSTRAINT fk_suggestion_valide_par
    FOREIGN KEY (valide_par_id) REFERENCES app_user(id);

ALTER TABLE suggestion
  ADD CONSTRAINT suggestion_statut_check
    CHECK (
      statut IN (
                 'OPEN',
                 'CLOSED',
                 'GENEREE',
                 'EN_ATTENTE_VALIDATION',
                 'VALIDEE',
                 'COMMANDEE'
        )
      );
INSERT INTO app_configuration (name, value, description, value_type, updated)
VALUES (
         'APP_COUVERTURE_MOIS_CLASSIQUE',
         '2',
         'Nombre de mois de couverture cible pour le calcul P2 des suggestions (produits sans configuration SEMOIS)',
         'NUMBER',
         NOW()
       )
  ON CONFLICT (name) DO NOTHING;

ALTER TABLE groupe_fournisseur
  ADD COLUMN IF NOT EXISTS delai_livraison_jours INTEGER NOT NULL DEFAULT 7;

ALTER TABLE fournisseur
  ADD COLUMN IF NOT EXISTS delai_livraison_jours INTEGER;

COMMENT ON COLUMN groupe_fournisseur.delai_livraison_jours IS 'Délai de livraison par défaut (jours) pour tous les fournisseurs du groupe';
COMMENT ON COLUMN fournisseur.delai_livraison_jours        IS 'Délai de livraison (jours) — surcharge le délai du groupe si renseigné';
ALTER TABLE semois_configuration
  ALTER COLUMN delai_livraison_jours DROP NOT NULL,
ALTER COLUMN delai_livraison_jours DROP DEFAULT;
UPDATE semois_configuration SET delai_livraison_jours = NULL WHERE delai_livraison_jours = 7;




-- Reconstruire mv_semois_suggestion avec la cascade COALESCE sur le délai.
-- coefficient_securite vient désormais de semois_classe_config (scc), plus de semois_configuration.
CREATE MATERIALIZED VIEW mv_semois_suggestion AS
SELECT p.id                                                                    AS produit_id,
       p.libelle,
       fp.code_cip,
       fp.fournisseur_id                                                       AS fournisseur_id,
       f.libelle                                                               AS fournisseur_libelle,
       sc.classe_criticite,
       scc.coefficient_securite,
       COALESCE(sc.delai_livraison_jours,
                f.delai_livraison_jours,
                gf.delai_livraison_jours,
                7)                                                             AS delai_livraison_jours,
       COALESCE((SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                        NULLIF(sum(7 - vma.row_num), 0::numeric)
                 FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                              row_number()
                                OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                       FROM ventes_mensuelles_agregees
                       WHERE ventes_mensuelles_agregees.produit_id = p.id
                         AND ventes_mensuelles_agregees.annee_mois::text >=
                             to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
                 WHERE vma.row_num <= 6), 0::numeric)::integer                 AS vmm,
  (COALESCE((SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                    NULLIF(sum(7 - vma.row_num), 0::numeric)
             FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                          row_number()
                            OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                   FROM ventes_mensuelles_agregees
                   WHERE ventes_mensuelles_agregees.produit_id = p.id
                     AND ventes_mensuelles_agregees.annee_mois::text >=
                              to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
             WHERE vma.row_num <= 6), 0::numeric) *
   (COALESCE(sc.delai_livraison_jours, f.delai_livraison_jours, gf.delai_livraison_jours, 7)::numeric
       * scc.coefficient_securite / 30.0))::integer                           AS marge_securite,
  (COALESCE((SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                    NULLIF(sum(7 - vma.row_num), 0::numeric)
             FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                          row_number()
                            OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                   FROM ventes_mensuelles_agregees
                   WHERE ventes_mensuelles_agregees.produit_id = p.id
                     AND ventes_mensuelles_agregees.annee_mois::text >=
                              to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
             WHERE vma.row_num <= 6), 0::numeric) * (1::numeric +
     COALESCE(sc.delai_livraison_jours, f.delai_livraison_jours, gf.delai_livraison_jours, 7)::numeric
       * scc.coefficient_securite / 30.0))::integer                           AS stock_objectif,
  COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)                          AS stock_actuel,
       GREATEST(0::bigint, (COALESCE(
                              (SELECT sum(vma.quantite_vendue * (7 - vma.row_num)) /
                                      NULLIF(sum(7 - vma.row_num), 0::numeric)
                               FROM (SELECT ventes_mensuelles_agregees.quantite_vendue,
                                            row_number()
                                              OVER (ORDER BY ventes_mensuelles_agregees.annee_mois DESC) AS row_num
                                     FROM ventes_mensuelles_agregees
                                     WHERE ventes_mensuelles_agregees.produit_id = p.id
                                       AND ventes_mensuelles_agregees.annee_mois::text >=
                                           to_char(now() - '6 mons'::interval, 'YYYY-MM'::text)) vma
                               WHERE vma.row_num <= 6), 0::numeric) * (1::numeric +
             COALESCE(sc.delai_livraison_jours, f.delai_livraison_jours, gf.delai_livraison_jours, 7)::numeric
               * scc.coefficient_securite / 30.0))::integer -
         COALESCE(sum(sp.qty_stock + sp.qty_ug), 0::bigint)) AS quantite_a_commander,
       sc.date_dernier_calcul,
       now()                                                                   AS vue_refresh_date
FROM produit p
       LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
       LEFT JOIN fournisseur f ON f.id = fp.fournisseur_id
       LEFT JOIN groupe_fournisseur gf ON gf.id = f.groupe_pournisseur_id
       LEFT JOIN semois_configuration sc ON sc.produit_id = p.id
       LEFT JOIN semois_classe_config scc ON scc.classe_criticite = sc.classe_criticite
       LEFT JOIN stock_produit sp ON sp.produit_id = p.id
WHERE p.status::text = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
  AND sc.id IS NOT NULL
GROUP BY p.id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
  f.delai_livraison_jours, gf.delai_livraison_jours,
  sc.classe_criticite, scc.coefficient_securite, sc.delai_livraison_jours,
  sc.date_dernier_calcul;

COMMENT ON MATERIALIZED VIEW mv_semois_suggestion IS
  'Vue matérialisée des suggestions SEMOIS — délai livraison en cascade : semois_config → fournisseur → groupe_fournisseur → 7j';

CREATE INDEX idx_mv_semois_produit      ON mv_semois_suggestion (produit_id);
CREATE INDEX idx_mv_semois_classe       ON mv_semois_suggestion (classe_criticite);
CREATE INDEX idx_mv_semois_fournisseur  ON mv_semois_suggestion (fournisseur_id);
CREATE INDEX idx_mv_semois_qte_commander ON mv_semois_suggestion (quantite_a_commander)
  WHERE quantite_a_commander > 0;




