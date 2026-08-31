-- ============================================================================
-- 17_suggestions.sql — Propositions d'achat (réapprovisionnement)
--
-- Sans elles, l'onglet « Propositions d'achat » de /commande s'ouvre sur
-- « Aucun fournisseur » : la moitié du module d'achat — consultation, ajout de
-- ligne, validation, rejet, commande totale ou partielle (ACH-11 à ACH-22) —
-- n'a alors rien à montrer.
--
-- Ce que l'application produirait elle-même par son batch SEMOIS est ici posé
-- directement, pour que le jeu de démonstration soit reproductible : trois
-- propositions GÉNÉRÉES chez trois grossistes principaux, et une VALIDÉE, prête
-- à devenir commande.
--
-- Les lignes portent des produits réellement référencés chez le fournisseur
-- (fournisseur_produit), sans quoi la suggestion s'afficherait sans prix.
-- ============================================================================

\i _header.sql

\echo '>> 17_suggestions : propositions de réapprovisionnement'

-- ---------------------------------------------------------------------------
-- Une proposition par grossiste principal qui référence au moins 8 produits.
-- ---------------------------------------------------------------------------
WITH principaux AS (
    SELECT f.id, f.libelle,
           row_number() OVER (ORDER BY f.id) AS rang
      FROM fournisseur f
     WHERE f.parent_id IS NULL
       AND EXISTS (SELECT 1 FROM fournisseur_produit fp WHERE fp.fournisseur_id = f.id)
     LIMIT 4
)
INSERT INTO suggestion (
    created_at, updated_at, statut, type_suggession, suggession_reference,
    fournisseur_id, magasin_id
)
SELECT
    NOW() - (p.rang || ' days')::interval,
    NOW() - (p.rang || ' days')::interval,
    -- La dernière est VALIDÉE : elle illustre l'état « prête à commander ».
    CASE WHEN p.rang = 4 THEN 'VALIDEE' ELSE 'GENEREE' END,
    'AUTO',
    'SUG' || to_char(NOW(), 'YYMM') || lpad(p.rang::text, 4, '0'),
    p.id,
    1
FROM principaux p
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Les lignes : huit produits par proposition, quantité déterministe.
-- ---------------------------------------------------------------------------
INSERT INTO suggestion_line (
    created_at, updated_at, quantity, fournisseur_produit_id, suggestion_id,
    quantite_modifiee_manuel
)
SELECT
    s.created_at,
    s.updated_at,
    -- Entre 5 et 40, par pas de 5 : des quantités qui ressemblent à des colis.
    5 * (1 + (fp.rn % 8)),
    fp.id,
    s.id,
    false
FROM suggestion s
JOIN LATERAL (
    SELECT fp.id, row_number() OVER (ORDER BY fp.id) AS rn
      FROM fournisseur_produit fp
      JOIN produit p ON p.id = fp.produit_id
     WHERE fp.fournisseur_id = s.fournisseur_id
       AND p.status = 'ENABLE'
       AND p.type_produit = 'PACKAGE'
     ORDER BY fp.id
     LIMIT 8
) fp ON true
WHERE NOT EXISTS (
    SELECT 1 FROM suggestion_line sl WHERE sl.suggestion_id = s.id
);

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_sug int;
    v_lig int;
    v_vide int;
BEGIN
    SELECT count(*) INTO v_sug FROM suggestion;
    SELECT count(*) INTO v_lig FROM suggestion_line;
    SELECT count(*) INTO v_vide
      FROM suggestion s
     WHERE NOT EXISTS (SELECT 1 FROM suggestion_line sl WHERE sl.suggestion_id = s.id);

    IF v_sug < 3 THEN RAISE EXCEPTION 'Propositions : % (attendu >= 3)', v_sug; END IF;
    IF v_lig < 20 THEN RAISE EXCEPTION 'Lignes de proposition : % (attendu >= 20)', v_lig; END IF;
    IF v_vide > 0 THEN RAISE EXCEPTION '% proposition(s) sans ligne', v_vide; END IF;

    RAISE NOTICE '17_suggestions : % propositions, % lignes.', v_sug, v_lig;
END $$;

-- ---------------------------------------------------------------------------
-- Configuration SEMOIS par produit
--
-- `semois_configuration` porte, pour CHAQUE produit, la vente mensuelle moyenne
-- calculee et le stock objectif qui en decoule. C'est ce que le batch SEMOIS de
-- l'application ecrit ; le jeu de demonstration le pose directement, comme il le
-- fait deja des propositions d'achat ci-dessus, pour rester reproductible.
--
-- Sans ces lignes : le tableau de bord Achats affiche « VMM inconnue », les
-- suggestions SEMOIS sont vides, le compteur de produits urgents reste a zero --
-- et, plus bas, l'exclusion temporaire n'a aucune ligne a modifier, ce qui
-- faisait echouer le chargement sur « Aucune exclusion SEMOIS ».
--
-- La table est per-produit et referencie `produit` : le reset l'a videe en meme
-- temps que le catalogue, malgre sa presence dans la liste des referentiels
-- conserves. La reconstruire ici est donc la seule facon d'en avoir.
--
-- La VMM est calculee sur les ventes REELLES des six derniers mois, et le stock
-- objectif en decoule : couverture du delai de livraison plus une marge selon la
-- criticite. Un produit A+ se couvre plus large qu'un produit C -- c'est tout le
-- propos d'une classification.
-- ---------------------------------------------------------------------------
INSERT INTO semois_configuration (
    produit_id, classe_criticite, delai_livraison_jours, frequence_commande_jours,
    facteur_saisonnier_actuel, facteur_saisonnier_manuel, limite_peremption,
    marge_securite, vmm_calcule, stock_objectif_calcule,
    date_dernier_calcul, created_at, updated_at
)
SELECT
    p.id,
    p.classe_criticite,
    2,                                  -- les grossistes livrent en 48 h
    7,                                  -- une commande par semaine
    1.00, false, false,
    CASE p.classe_criticite
        WHEN 'A_PLUS' THEN 15 WHEN 'A' THEN 10 WHEN 'B' THEN 7 ELSE 5 END,
    v.vmm,
    -- Objectif = consommation du delai + marge de securite, plancher a 1 pour
    -- qu'un produit qui se vend ne soit jamais objectif zero.
    GREATEST(1, (v.vmm * 2 / 30)::int
                + CASE p.classe_criticite
                      WHEN 'A_PLUS' THEN 15 WHEN 'A' THEN 10 WHEN 'B' THEN 7 ELSE 5 END),
    NOW() - INTERVAL '6 hours',         -- calcul du matin meme
    NOW() - INTERVAL '180 days',
    NOW() - INTERVAL '6 hours'
FROM produit p
JOIN (
    SELECT sl.produit_id,
           GREATEST(1, (sum(sl.quantity_sold) / 6.0)::int) AS vmm
      FROM sales_line sl
      JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
     WHERE s.statut = 'CLOSED'
       AND s.sale_date >= CURRENT_DATE - 180
     GROUP BY sl.produit_id
) v ON v.produit_id = p.id
WHERE p.status = 'ENABLE'
ON CONFLICT (produit_id) DO NOTHING;

DO $$
DECLARE v_n INTEGER; v_urgents INTEGER;
BEGIN
    SELECT count(*) INTO v_n FROM semois_configuration;
    IF v_n < 100 THEN
        RAISE EXCEPTION 'Configurations SEMOIS : % (attendu >= 100)', v_n;
    END IF;

    -- Le compteur « produits urgents » du tableau de bord compte les produits
    -- dont la VMM est positive et le stock sous l'objectif : sans cas, la
    -- pastille reste muette et ACH-26 n'a rien a montrer.
    SELECT count(*) INTO v_urgents
      FROM semois_configuration sc
      JOIN stock_produit sp ON sp.produit_id = sc.produit_id
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
     WHERE sc.vmm_calcule > 0 AND sp.qty_stock < sc.stock_objectif_calcule;
    IF v_urgents = 0 THEN
        RAISE EXCEPTION 'Aucun produit sous son stock objectif : le calcul SEMOIS ne montre rien';
    END IF;

    RAISE NOTICE '% configuration(s) SEMOIS, dont % produit(s) sous objectif.', v_n, v_urgents;
END $$;

-- ---------------------------------------------------------------------------
-- Exclusions SEMOIS temporaires
--
-- Un produit en rupture durable chez tous les grossistes, ou qu'on arrete,
-- continue d'etre propose chaque semaine par le calcul. L'exclusion le fait
-- taire — TEMPORAIREMENT : elle porte une duree et un motif, et le produit
-- revient de lui-meme a l'echeance.
--
-- Sans exclusion active, le panneau « Exclusions SEMOIS actives » s'ouvre sur
-- « Aucune exclusion active » : ni la duree, ni le motif, ni la reintegration
-- ne s'y demontrent.
--
-- Les trois colonnes vont ENSEMBLE (cf. V1.3.8) : une date sans duree ne dit
-- pas quand le produit revient, un motif sans date n'exclut rien.
-- ---------------------------------------------------------------------------
UPDATE semois_configuration sc
   SET exclusion_date = CURRENT_DATE - (v.jours_ecoules || ' days')::interval,
       exclusion_duree_jours = v.duree,
       exclusion_motif = v.motif
  FROM (VALUES
        (0, 10, 90, 'Rupture fournisseur prolongee, reapprovisionnement incertain'),
        (1,  3, 30, 'Reference en cours d''arret, ecoulement du stock restant'),
        (2, 25, 60, 'Changement de fournisseur en cours de negociation')
       ) AS v(rang, jours_ecoules, duree, motif)
 WHERE sc.id = (
     SELECT s2.id FROM semois_configuration s2
      WHERE s2.exclusion_date IS NULL
      ORDER BY s2.id
      OFFSET v.rang LIMIT 1
 );

DO $$
DECLARE v_n INTEGER;
BEGIN
    SELECT count(*) INTO v_n FROM semois_configuration WHERE exclusion_date IS NOT NULL;
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucune exclusion SEMOIS : le panneau resterait vide';
    END IF;

    -- Une exclusion doit encore courir, sinon elle n'a plus de jours restants a
    -- afficher et la reintegration anticipee ne se demontre pas.
    SELECT count(*) INTO v_n FROM semois_configuration
     WHERE exclusion_date IS NOT NULL
       AND exclusion_date + (exclusion_duree_jours || ' days')::interval > now();
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Toutes les exclusions SEMOIS sont expirees : aucune n''est active';
    END IF;

    -- Les trois colonnes vont ensemble.
    SELECT count(*) INTO v_n FROM semois_configuration
     WHERE (exclusion_date IS NOT NULL) <> (exclusion_duree_jours IS NOT NULL)
        OR (exclusion_date IS NOT NULL AND exclusion_motif IS NULL);
    IF v_n > 0 THEN
        RAISE EXCEPTION '% exclusion(s) SEMOIS incomplete(s)', v_n;
    END IF;

    RAISE NOTICE '% exclusion(s) SEMOIS active(s).',
                 (SELECT count(*) FROM semois_configuration WHERE exclusion_date IS NOT NULL);
END $$;
