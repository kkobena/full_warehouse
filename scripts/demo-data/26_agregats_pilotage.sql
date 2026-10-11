-- ============================================================================
-- 26_agregats_pilotage.sql — Agrégats du pilotage, sur tout l'historique du jeu
--
-- Le pilotage ne lit pas les ventes ni les commandes : il lit des agrégats (V2.1.34, V2.1.36).
--   pilotage_vente_ligne_jour, pilotage_vente_jour   ventes, marge, remises, fréquentation
--   pilotage_achat_jour                              commandes reçues
--   pilotage_encaissement_jour                       encaissements
--   pilotage_stock_mensuel                           stock valorisé en fin de mois
--
-- La migration les charge UNE fois, sur une base encore vide : la démonstration arrive après elle. Ensuite,
-- l'application ne les tient à jour que pour les jours modifiés récemment (AgregatsPilotageScheduler : 30 minutes,
-- 7 jours au démarrage) et ne photographie que le stock du mois en cours. Sans ce script, tous les onglets
-- s'ouvrent sur trois années vides avec seulement la dernière semaine renseignée : le tableau de bord n'a pas de N-1,
-- le suivi des objectifs n'a pas de réalisé pour les mois clos, l'onglet « Achats & stock » n'a ni achats ni
-- courbe de stock, et les alertes qui comparent à N-1 (chute d'activité, érosion de marge, famille en recul) ne
-- se déclenchent jamais.
--
-- Il rejoue donc le chargement initial de la migration, avec les fonctions de l'application :
--   * pilotage_recalculer(du, au) sur toute la période des ventes, réceptions et encaissements ;
--   * pilotage_photographier_stock(mois), un mois par ligne depuis le premier mouvement de stock.
--
-- Il passe APRÈS 24_mouvements.sql : le stock d'un mois passé se reconstitue depuis le journal des mouvements
-- (fn_stock_valuation_bulk, V2.1.33). Rejouable seul : chaque fonction efface ce qu'elle recalcule.
-- ============================================================================

\i _header.sql

\echo '>> 26_agregats_pilotage : agrégats du pilotage (ventes, achats, encaissements, stock mensuel)'

SELECT to_regprocedure('pilotage_recalculer(date, date)') IS NOT NULL AS fonctions_pilotage \gset
\if :fonctions_pilotage

SELECT pilotage_recalculer(
         LEAST(COALESCE((SELECT min(sale_date) FROM sales), CURRENT_DATE),
               COALESCE((SELECT min(receipt_date) FROM commande), CURRENT_DATE),
               COALESCE((SELECT min(transaction_date) FROM payment_transaction), CURRENT_DATE)),
         CURRENT_DATE);

SELECT count(*) AS mois_photographies
  FROM (SELECT pilotage_photographier_stock(mois::date)
          FROM generate_series(date_trunc('month', COALESCE((SELECT min(created_at) FROM inventory_transaction), NOW())),
                               date_trunc('month', NOW()),
                               INTERVAL '1 month') AS mois) p;

-- ---------------------------------------------------------------------------
-- Contrôles : l'historique couvre N et N-1, et les agrégats retombent sur leurs pièces.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_mois_ventes int; v_mois_achats int; v_mois_encaissements int; v_mois_stock int;
    v_ca_agrege bigint; v_ca_pieces bigint;
BEGIN
    SELECT count(DISTINCT date_trunc('month', jour)) INTO v_mois_ventes FROM pilotage_vente_jour;
    SELECT count(DISTINCT date_trunc('month', jour)) INTO v_mois_achats FROM pilotage_achat_jour;
    SELECT count(DISTINCT date_trunc('month', jour)) INTO v_mois_encaissements FROM pilotage_encaissement_jour;
    SELECT count(DISTINCT mois) INTO v_mois_stock FROM pilotage_stock_mensuel;

    -- Treize mois au moins : le mois en cours et le même mois de l'an passé, que comparent le tableau de bord et les alertes.
    IF least(v_mois_ventes, v_mois_achats, v_mois_encaissements, v_mois_stock) < 13 THEN
        RAISE EXCEPTION 'Agrégats du pilotage trop courts : ventes % mois, achats % mois, encaissements % mois, stock % mois (attendu >= 13)',
            v_mois_ventes, v_mois_achats, v_mois_encaissements, v_mois_stock;
    END IF;

    -- Le CA agrégé (en-têtes clôturées, non annulées, catégorie CA) retombe sur les ventes elles-mêmes.
    SELECT COALESCE(sum(montant_ttc), 0) INTO v_ca_agrege
      FROM pilotage_vente_jour WHERE NOT annulee AND categorie_ca = 'CA';
    SELECT COALESCE(sum(sales_amount), 0) INTO v_ca_pieces
      FROM sales WHERE statut = 'CLOSED' AND NOT canceled AND ca = 'CA';
    IF v_ca_agrege <> v_ca_pieces THEN
        RAISE EXCEPTION 'CA agrégé % <> CA des ventes % : les agrégats ne recoupent pas les pièces', v_ca_agrege, v_ca_pieces;
    END IF;

    RAISE NOTICE 'Agrégats : ventes % mois, achats % mois, encaissements % mois, stock % mois.',
        v_mois_ventes, v_mois_achats, v_mois_encaissements, v_mois_stock;
END $$;

\else
\echo '   (fonctions pilotage_recalculer absentes : migration V2.1.34 non appliquée, étape ignorée)'
\endif

\echo '<< 26_agregats_pilotage : terminé'
