-- ============================================================================
-- 04b_remises_plafonds_tarifs.sql — Remises, plafonds et tarifs négociés
--
-- Trois mécanismes de prix qu'aucune donnée n'exerçait, et qui font pourtant
-- toute la difficulté de la vente en officine :
--
--   1. LES REMISES PRODUIT. Le produit porte un « code remise » qui renvoie à
--      deux taux — l'un pour les ventes non ordonnancées, l'autre pour les
--      ordonnancées. C'est le SEUL mécanisme de remise appliqué à la vente :
--      la remise attachée au client (personnel, fidélité) existe encore dans le
--      modèle et son écran d'administration, mais plus rien ne la lit dans le
--      calcul d'une vente. On ne l'alimente donc pas : des données qu'aucun
--      écran n'exerce ne feraient qu'égarer.
--   2. LES PLAFONDS de prise en charge — traités dans 13b_plafonds.sql, et
--      non ici : la consommation mensuelle des assurés est RECALCULÉE par
--      13_consommations.sql depuis les ventes. Toute valeur posée avant lui
--      serait écrasée sans bruit.
--   3. LES TARIFS NÉGOCIÉS. Un assureur peut imposer un prix de référence,
--      inférieur au prix catalogue, sur lequel sa part se calcule.
--
-- PIÈGES DU MODÈLE, vérifiés sur la base :
--   * `remise` est une table à héritage SINGLE_TABLE : la colonne `dtype`
--     distingue « RemiseClient » de « RemiseProduit », et la contrainte CHECK
--     n'accepte que ces valeurs.
--   * `grille_remise.remise_value` est un POURCENTAGE (10 pour 10 %) : c'est
--     `GrilleRemise.getTauxRemise()` qui divise par 100. Y écrire 0,10 ferait
--     une remise de 0,1 %.
--   * `grille_remise.code` est contraint aux valeurs CODE_12 à CODE_29, et
--     chaque code est soit VNO soit VO (cf. `CodeGrilleRemise`) : un produit
--     porte un `code_remise` qui pointe une grille VNO **et** une grille VO.
--   * le plafond mensuel appliqué à un client vient de
--     `tiers_payant.plafond_conso_client`, tandis que la consommation déjà
--     engagée vit sur `client_tiers_payant.conso_mensuelle` — deux tables.
-- ============================================================================

\i _header.sql

\echo '>> 04b_remises_plafonds_tarifs : remises, plafonds et tarifs négociés'

-- ---------------------------------------------------------------------------
-- 1. Grille de remise produit
--
-- Le mécanisme est celui des officines ivoiriennes : le produit porte un code
-- (1 à 9), et le code renvoie à deux taux — l'un pour les ventes non
-- ordonnancées, l'autre pour les ordonnancées.
-- ---------------------------------------------------------------------------
INSERT INTO remise (dtype, libelle, remise_value, enable)
VALUES ('RemiseProduit', 'Grille de remise officine', 0, true)
ON CONFLICT DO NOTHING;

INSERT INTO grille_remise (code, remise_value, enable, remise_produit_id)
SELECT g.code, g.valeur, true, rp.id
FROM (SELECT id FROM remise WHERE dtype = 'RemiseProduit' ORDER BY id LIMIT 1) rp
CROSS JOIN (VALUES
    -- Ventes non ordonnancées (comptant). Les taux retenus sont ceux qu'une officine
    -- accorde vraiment — 5, 10, 15 % : à 1 ou 2 %, la remise ne se voit ni sur le ticket
    -- ni dans les rapports, et une démonstration ne démontre rien.
    ('CODE_12', 5::real), ('CODE_13', 10), ('CODE_14', 15), ('CODE_15', 10), ('CODE_16', 15),
    -- Ventes ordonnancées (assurance) : taux plus mesurés, la part assurance étant déjà
    -- négociée — mais assez francs pour rester lisibles.
    ('CODE_17', 5),       ('CODE_18', 5),  ('CODE_19', 10), ('CODE_20', 10), ('CODE_21', 15)
) AS g(code, valeur)
ON CONFLICT DO NOTHING;

-- Les codes remise sur les produits.
--
-- PIÈGE : la colonne stocke le NOM de l'énumération (« CODE_0 »), pas sa valeur
-- (« 1 »). Tous les produits partent à CODE_0, qui pointe déjà les grilles
-- CODE_12 (VNO) et CODE_17 (VO) : la remise s'appliquerait donc partout au même
-- taux. On répartit un produit sur quatre sur les codes suivants, pour que les
-- cinq lignes de la grille servent réellement à quelque chose.
UPDATE produit p
   SET code_remise = codes.code
  FROM (VALUES (0, 'CODE_1'), (1, 'CODE_2'), (2, 'CODE_3'), (3, 'CODE_4')) AS codes(reste, code)
 WHERE (p.id % 4) = 0
   AND (p.id / 4) % 4 = codes.reste;

-- Le produit que le manuel utilise pour ILLUSTRER la remise doit en porter une,
-- et une franche. La repartition ci-dessus est arithmetique — un produit sur
-- seize — et rien ne garantissait qu'elle tombe sur lui : selon l'ordre
-- d'insertion du catalogue, ATORVASTATINE se retrouvait a CODE_0 puis, une fois
-- sur deux, sans remise visible du tout. La capture montrait alors une remise
-- de zero, ce qui ne demontre rien.
UPDATE produit
   SET code_remise = 'CODE_2'   -- grille CODE_14 : 15 % en vente comptant
 WHERE libelle LIKE 'ATORVASTATINE 100MG%';

-- ---------------------------------------------------------------------------
-- 2. Tarifs négociés (prix de référence assurance)
--
-- Trois formes existent, et l'écran de vente les traite différemment :
--   * REFERENCE                    : la part assurance se calcule sur un prix
--                                    imposé, inférieur au prix catalogue ;
--   * POURCENTAGE                  : l'assureur applique son propre taux sur
--                                    la ligne, quel que soit son taux général ;
--   * MIXED_REFERENCE_POURCENTAGE  : les deux à la fois.
--
-- On les pose sur des produits fréquents, faute de quoi il faudrait chercher
-- longtemps un cas à montrer.
-- ---------------------------------------------------------------------------
INSERT INTO produit_tiers_payant_prix (
    produit_id, tiers_payant_id, price, rate, prix_type, enabled, created, updated, user_id
)
SELECT
    p.id,
    tp.id,
    -- 85 % du prix catalogue, arrondi au multiple de 5 : la plus petite pièce en FCFA
    -- vaut 5 francs. Un rabais crédible, et assez marqué pour se voir sur la ligne.
    GREATEST((5 * round((fp.prix_uni * 0.85) / 5.0))::int, 5),
    1.0,
    'REFERENCE',
    true,
    now(), now(),
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1)
FROM produit p
JOIN LATERAL (
    SELECT x.prix_uni
      FROM fournisseur_produit x
      JOIN fournisseur f ON f.id = x.fournisseur_id AND f.parent_id IS NULL
     WHERE x.produit_id = p.id
     ORDER BY x.id LIMIT 1
) fp ON true
CROSS JOIN (SELECT id FROM tiers_payant WHERE name IN ('MUGEFCI', 'CNAM')) tp
WHERE fp.prix_uni > 0
  AND (p.id % 11) = 0
ON CONFLICT DO NOTHING;

-- Un tiers payant qui impose son propre taux sur quelques produits, sans
-- toucher au prix : c'est le cas POURCENTAGE.
INSERT INTO produit_tiers_payant_prix (
    produit_id, tiers_payant_id, price, rate, prix_type, enabled, created, updated, user_id
)
SELECT
    p.id,
    tp.id,
    0,
    0.6,                       -- 60 % pris en charge sur ces lignes
    'POURCENTAGE',
    true,
    now(), now(),
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1)
FROM produit p
CROSS JOIN (SELECT id FROM tiers_payant WHERE name = 'COLINA') tp
WHERE (p.id % 23) = 0
ON CONFLICT DO NOTHING;

\echo '   remises, plafonds et tarifs négociés chargés'
