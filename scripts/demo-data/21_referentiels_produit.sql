-- ============================================================================
-- 21_referentiels_produit.sql — Laboratoires, gammes, ruptures fournisseur,
--                               ventes mensuelles agrégées, classes de criticité
--
-- Tout ce qui s'appuie sur les ventes et les achats des trois ans, et ne peut donc se
-- poser qu'une fois ceux-ci stabilisés (après 16b et 19) :
--
--   * laboratoires et gammes : le laboratoire d'une spécialité du référentiel est son
--     titulaire BDPM ; pour les produits composés, un laboratoire plausible de la famille ;
--   * ruptures : ce que le grossiste n'a pas pu livrer (quantité commandée supérieure à la
--     quantité reçue) — l'écran des ruptures et le taux de service s'y adossent ;
--   * ventes_mensuelles_agregees : l'agrégat produit × mois que lisent les prévisions de
--     réapprovisionnement (SEMOIS). Les mois révolus sont figés, le mois courant ne l'est pas ;
--   * classification_criticite_log : l'historique des changements de classe ABC d'un produit,
--     recalculée chaque année sur le chiffre d'affaires des douze mois écoulés. La classe
--     portée par le produit est celle du dernier calcul.
-- ============================================================================

\i _header.sql

\echo '>> 21_referentiels_produit : laboratoires, gammes, ruptures, agrégats, classes'

-- ---------------------------------------------------------------------------
-- 1. Laboratoires
-- ---------------------------------------------------------------------------
-- Les titulaires des spécialités réelles de 03c_produits_bdpm.sql.
INSERT INTO laboratoire (libelle)
SELECT DISTINCT left(s.titulaire, 255)
  FROM produit p
  JOIN ref_specialite s ON s.libelle = p.libelle
 WHERE p.libelle IN (SELECT libelle FROM ref_specialite)
   AND s.titulaire IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM laboratoire l WHERE l.libelle = left(s.titulaire, 255));

-- Les laboratoires des produits composés, par famille.
CREATE TEMP TABLE tmp_labo (famille text, rang int, libelle text);
INSERT INTO tmp_labo VALUES
    ('1000', 0, 'SANOFI'), ('1000', 1, 'PFIZER'), ('1000', 2, 'GLAXOSMITHKLINE'), ('1000', 3, 'NOVARTIS'),
    ('1000', 4, 'SERVIER'), ('1000', 5, 'BAYER'),
    ('1050', 0, 'SANOFI'), ('1050', 1, 'PFIZER'), ('1050', 2, 'ASTRAZENECA'), ('1050', 3, 'MSD'),
    ('1050', 4, 'ABBOTT'), ('1050', 5, 'MENARINI'),
    ('1030', 0, 'BIOGARAN'), ('1030', 1, 'SANDOZ'), ('1030', 2, 'ARROW GENERIQUES'), ('1030', 3, 'TEVA SANTE'),
    ('1030', 4, 'MYLAN'), ('1030', 5, 'ZENTIVA'), ('1030', 6, 'EG LABO'), ('1030', 7, 'CRISTERS'),
    ('1040', 0, 'BOIRON'), ('1040', 1, 'LEHNING'), ('1040', 2, 'GUNA'),
    ('5000', 0, 'NESTLE'), ('5000', 1, 'GUIGOZ'), ('5000', 2, 'MODILAC'), ('5000', 3, 'BLEDINA'),
    ('6000', 0, 'ARKOPHARMA'), ('6000', 1, 'PILEJE'), ('6000', 2, 'NUTERGIA'),
    ('3000', 0, 'PIERRE FABRE'), ('3000', 1, 'URIAGE'), ('3000', 2, 'LA ROCHE-POSAY'), ('3000', 3, 'NUXE'),
    ('8000', 0, 'URGO'), ('8000', 1, 'HARTMANN'), ('8000', 2, 'THUASNE');

INSERT INTO laboratoire (libelle)
SELECT DISTINCT t.libelle FROM tmp_labo t
 WHERE NOT EXISTS (SELECT 1 FROM laboratoire l WHERE l.libelle = t.libelle);

UPDATE produit p
   SET laboratoire_id = l.id
  FROM ref_specialite s, laboratoire l
 WHERE p.libelle IN (SELECT libelle FROM ref_specialite)
   AND s.libelle = p.libelle
   AND l.libelle = left(s.titulaire, 255);

UPDATE produit p
   SET laboratoire_id = l.id
  FROM famille_produit f
  JOIN (SELECT famille, count(*) AS nb FROM tmp_labo GROUP BY famille) n ON n.famille = f.code
  JOIN tmp_labo t ON t.famille = f.code
  JOIN laboratoire l ON l.libelle = t.libelle
 WHERE f.id = p.famille_id
   AND p.laboratoire_id IS NULL
   AND t.rang = p.id % n.nb;

DROP TABLE tmp_labo;

-- ---------------------------------------------------------------------------
-- 2. Gammes (parapharmacie, diététique, accessoires)
-- ---------------------------------------------------------------------------
INSERT INTO gamme_produit (code, libelle)
VALUES ('SOINS',   'SOINS DU CORPS ET DU VISAGE'),
       ('CAPIL',   'SOINS CAPILLAIRES'),
       ('HYGIE',   'HYGIENE ET TOILETTE'),
       ('LAIT1',   'LAITS 1ER AGE'),
       ('LAIT2',   'LAITS 2EME AGE ET CROISSANCE'),
       ('CEREA',   'CEREALES ET ALIMENTATION BEBE'),
       ('COMPL',   'COMPLEMENTS ALIMENTAIRES'),
       ('PANSE',   'PANSEMENTS ET SOINS DES PLAIES'),
       ('MATER',   'PETIT MATERIEL MEDICAL');

UPDATE produit p
   SET gamme_id = g.id
  FROM famille_produit f, gamme_produit g
 WHERE f.id = p.famille_id
   AND g.code = CASE f.code
        WHEN '3000' THEN (ARRAY['SOINS', 'CAPIL', 'HYGIE'])[1 + p.id % 3]
        WHEN '5000' THEN (ARRAY['LAIT1', 'LAIT2', 'CEREA'])[1 + p.id % 3]
        WHEN '6000' THEN 'COMPL'
        WHEN '8000' THEN (ARRAY['PANSE', 'MATER'])[1 + p.id % 2]
        END;

-- ---------------------------------------------------------------------------
-- 3. Ruptures fournisseur
--
-- Une rupture est une ligne que le grossiste n'a pas (entièrement) servie : quantité
-- commandée supérieure à la quantité reçue. Elle porte le manquant. Elle n'est « toujours en
-- rupture » que si elle est récente — passé quelques semaines, le produit a été reçu depuis.
-- ---------------------------------------------------------------------------
INSERT INTO rupture (date_mtv, product_still_out_of_stock, qty, fournisseur_id, produit_id)
SELECT c.receipt_date + TIME '10:15:00',
       c.receipt_date >= CURRENT_DATE - 14,
       ol.quantity_requested - ol.quantity_received,
       c.fournisseur_id,
       fp.produit_id
  FROM order_line ol
  JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
  JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
 WHERE c.order_status IN ('RECEIVED', 'CLOSED')
   AND ol.quantity_received IS NOT NULL
   AND ol.quantity_requested > ol.quantity_received
   AND c.receipt_date IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. Ventes mensuelles agrégées
-- ---------------------------------------------------------------------------
INSERT INTO ventes_mensuelles_agregees (
    annee_mois, produit_id, quantite_vendue, nombre_ventes, montant_ca,
    is_frozen, freeze_date, est_rupture_fournisseur, created_at, updated_at
)
SELECT to_char(m.mois, 'YYYY-MM'),
       m.produit_id,
       m.qte, m.nb, m.ca,
       m.mois < date_trunc('month', CURRENT_DATE),
       CASE WHEN m.mois < date_trunc('month', CURRENT_DATE)
            THEN (m.mois + INTERVAL '1 month' + TIME '02:00:00') END,
       EXISTS (SELECT 1 FROM rupture r
                WHERE r.produit_id = m.produit_id
                  AND date_trunc('month', r.date_mtv) = m.mois),
       (m.mois + INTERVAL '1 month')::timestamp,
       LEAST((m.mois + INTERVAL '1 month')::timestamp, NOW())
  FROM (
      SELECT date_trunc('month', sl.sale_date) AS mois, sl.produit_id,
             sum(sl.quantity_sold)::int AS qte,
             count(DISTINCT sl.sales_id)::int AS nb,
             sum(sl.sales_amount - sl.discount_amount)::int AS ca
        FROM sales_line sl
       GROUP BY date_trunc('month', sl.sale_date), sl.produit_id
  ) m;

-- ---------------------------------------------------------------------------
-- 5. Classes de criticité (ABC)
--
-- Trois calculs, à deux ans, un an et un mois avant aujourd'hui : sur le chiffre d'affaires des douze mois précédents, le premier
-- vingtile en A+, les 15 % suivants en A, 30 % en B, 30 % en C, le reste en D. Les
-- produits sans vente restent en D.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_abc AS
SELECT r.run, r.jour, p.id AS produit_id,
       COALESCE(v.ca, 0)::bigint AS ca, COALESCE(v.qte, 0) AS qte,
       COALESCE(v.mois_vente, 0) AS mois_vente,
       row_number() OVER (PARTITION BY r.run ORDER BY COALESCE(v.ca, 0) DESC, p.id) AS rang,
       count(*) OVER (PARTITION BY r.run) AS total
  FROM (VALUES (1, CURRENT_DATE - 730), (2, CURRENT_DATE - 365), (3, CURRENT_DATE - 30)) r(run, jour)
 CROSS JOIN produit p
  LEFT JOIN LATERAL (
      SELECT sum(sl.sales_amount - sl.discount_amount) AS ca, sum(sl.quantity_sold) AS qte,
             count(DISTINCT date_trunc('month', sl.sale_date))::int AS mois_vente
        FROM sales_line sl
       WHERE sl.produit_id = p.id
         AND sl.sale_date > r.jour - 365 AND sl.sale_date <= r.jour
  ) v ON true
 WHERE p.status = 'ENABLE' AND p.type_produit = 'PACKAGE'
   AND p.created_at < r.jour;

ALTER TABLE tmp_abc ADD COLUMN classe text;
UPDATE tmp_abc
   SET classe = CASE WHEN ca = 0                         THEN 'D'
                     WHEN rang <= total * 0.05           THEN 'A_PLUS'
                     WHEN rang <= total * 0.20           THEN 'A'
                     WHEN rang <= total * 0.50           THEN 'B'
                     WHEN rang <= total * 0.80           THEN 'C'
                     ELSE 'D' END;

INSERT INTO classification_criticite_log (
    ancienne_classe, ca_12_mois, classification_type, created_at, frequence_vente_mois,
    nouvelle_classe, raison_changement, rotation_annuelle, score_total, vmm_12_mois,
    produit_id, user_id
)
SELECT
    prec.classe,
    a.ca,
    CASE WHEN a.run = 1 AND prec.classe IS NULL THEN 'INITIAL' ELSE 'AUTO' END,
    a.jour + TIME '03:00:00',
    a.mois_vente,
    a.classe,
    CASE WHEN prec.classe IS NULL THEN 'Classification initiale du catalogue'
         ELSE 'Recalcul annuel sur le chiffre d''affaires des 12 derniers mois' END,
    round((a.qte / GREATEST(COALESCE((SELECT sp.qty_stock FROM stock_produit sp
                                        JOIN storage s ON s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL'
                                       WHERE sp.produit_id = a.produit_id LIMIT 1), 1), 1))::numeric, 2),
    round((100 - 100.0 * a.rang / a.total)::numeric, 2),
    (a.qte / 12),
    a.produit_id,
    (SELECT id FROM app_user WHERE login = 'admin')
  FROM tmp_abc a
  LEFT JOIN tmp_abc prec ON prec.produit_id = a.produit_id AND prec.run = a.run - 1
 -- Seuls les changements de classe (et l'initialisation) sont tracés.
 WHERE prec.classe IS DISTINCT FROM a.classe;

-- La classe portée par le produit est celle du dernier calcul.
UPDATE produit p
   SET classe_criticite = a.classe
  FROM tmp_abc a
 WHERE a.run = 3 AND a.produit_id = p.id;

DROP TABLE tmp_abc;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_labo int; v_sans_labo int; v_gamme int; v_rupt int; v_agg int; v_ecart int;
    v_log int; v_incoh int; v_gelees int;
BEGIN
    SELECT count(*) INTO v_labo FROM laboratoire;
    SELECT count(*) INTO v_sans_labo FROM produit
     WHERE laboratoire_id IS NULL AND type_produit = 'PACKAGE'
       AND famille_id IN (SELECT id FROM famille_produit WHERE code IN ('1000','1030','1040','1050','5000','6000','3000','8000'));
    SELECT count(*) INTO v_gamme FROM produit WHERE gamme_id IS NOT NULL;
    SELECT count(*) INTO v_rupt FROM rupture;
    SELECT count(*) INTO v_agg FROM ventes_mensuelles_agregees;

    -- L'agrégat doit retomber sur les lignes de vente, produit par produit et mois par mois.
    SELECT count(*) INTO v_ecart FROM ventes_mensuelles_agregees a
     WHERE a.quantite_vendue <> (SELECT COALESCE(sum(sl.quantity_sold), 0) FROM sales_line sl
                                  WHERE sl.produit_id = a.produit_id
                                    AND to_char(sl.sale_date, 'YYYY-MM') = a.annee_mois);

    SELECT count(*) INTO v_gelees FROM ventes_mensuelles_agregees
     WHERE is_frozen <> (annee_mois < to_char(CURRENT_DATE, 'YYYY-MM'));

    SELECT count(*) INTO v_log FROM classification_criticite_log;
    -- La classe du produit est la dernière tracée.
    SELECT count(*) INTO v_incoh FROM produit p
     WHERE EXISTS (SELECT 1 FROM classification_criticite_log l WHERE l.produit_id = p.id)
       AND p.classe_criticite <> (SELECT l.nouvelle_classe FROM classification_criticite_log l
                                   WHERE l.produit_id = p.id ORDER BY l.created_at DESC, l.id DESC LIMIT 1);

    IF v_labo < 40 THEN RAISE EXCEPTION 'Laboratoires : % (attendu >= 40)', v_labo; END IF;
    IF v_sans_labo > 0 THEN RAISE EXCEPTION '% produit(s) sans laboratoire', v_sans_labo; END IF;
    IF v_gamme < 50 THEN RAISE EXCEPTION 'Produits en gamme : % (attendu >= 50)', v_gamme; END IF;
    IF v_rupt < 50 THEN RAISE EXCEPTION 'Ruptures fournisseur : % (attendu >= 50)', v_rupt; END IF;
    IF v_agg < 5000 THEN RAISE EXCEPTION 'Ventes mensuelles agrégées : % (attendu >= 5000)', v_agg; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% agrégat(s) qui contredisent les lignes de vente', v_ecart; END IF;
    IF v_gelees > 0 THEN RAISE EXCEPTION '% agrégat(s) dont l''état figé contredit le mois', v_gelees; END IF;
    IF v_log < 100 THEN RAISE EXCEPTION 'Changements de classe tracés : % (attendu >= 100)', v_log; END IF;
    IF v_incoh > 0 THEN RAISE EXCEPTION '% produit(s) dont la classe contredit le dernier calcul', v_incoh; END IF;

    RAISE NOTICE '% laboratoires, % produits en gamme, % ruptures, % agrégats, % changements de classe.',
                 v_labo, v_gamme, v_rupt, v_agg, v_log;
END $$;

\echo '<< 21_referentiels_produit : terminé'
