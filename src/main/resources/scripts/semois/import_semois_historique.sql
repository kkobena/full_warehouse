-- =============================================================================
-- Script d'initialisation SEMOIS
-- Import des 12 derniers mois de données historiques de ventes
--
-- À EXÉCUTER UNE SEULE FOIS lors du déploiement initial du module SEMOIS
-- =============================================================================

-- Étape 1: Vérifier que les tables SEMOIS existent
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'pharma_smart'
        AND table_name = 'ventes_mensuelles_agregees'
    ) THEN
        RAISE EXCEPTION 'Table ventes_mensuelles_agregees non trouvée. Exécutez d''abord la migration Flyway V1.1.22';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'pharma_smart'
        AND table_name = 'semois_configuration'
    ) THEN
        RAISE EXCEPTION 'Table semois_configuration non trouvée. Exécutez d''abord la migration Flyway V1.1.22';
    END IF;
END $$;

-- =============================================================================
-- Étape 2: Initialiser les configurations SEMOIS pour tous les produits actifs
-- Classe par défaut: B (rotation moyenne)
-- =============================================================================

INSERT INTO semois_configuration (
    produit_id,
    classe_criticite,
    coefficient_securite,
    nb_mois_historique,
    delai_livraison_jours,
    facteur_saisonnier_actuel,
    limite_peremption,
    created_at,
    updated_at
)
SELECT
    p.id,
    'B', -- Classe par défaut, à ajuster manuellement après initialisation
    1.0, -- Coefficient de sécurité par défaut
    6,   -- 6 mois d'historique pour VMM
    7,   -- Délai de livraison par défaut: 7 jours
    1.0, -- Facteur saisonnier neutre par défaut
    FALSE, -- Pas de limite péremption par défaut
    NOW(),
    NOW()
FROM produit p
WHERE p.status = 'ENABLE'
  AND p.type_produit != 'DETAIL'
  AND NOT EXISTS (
      SELECT 1 FROM semois_configuration sc WHERE sc.produit_id = p.id
  )
ON CONFLICT (produit_id) DO NOTHING;

-- Afficher le nombre de configurations créées
DO $$
DECLARE
    config_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO config_count FROM semois_configuration;
    RAISE NOTICE '✅ Étape 2 terminée: % configurations SEMOIS initialisées', config_count;
END $$;

-- =============================================================================
-- Étape 3: Importer les 12 derniers mois de ventes agrégées
-- Les mois M-2 à M-12 seront gelés immédiatement
-- Le mois M-1 reste modifiable (fenêtre de correction J+7)
-- Le mois en cours (M) sera recalculé quotidiennement
-- =============================================================================

DO $$
DECLARE
    current_month TEXT;
    mois_a_importer TEXT;
    i INTEGER;
    rows_affected INTEGER;
    total_rows INTEGER := 0;
    should_freeze BOOLEAN;
BEGIN
    current_month := TO_CHAR(CURRENT_DATE, 'YYYY-MM');

    -- Boucle pour importer 12 mois (M-1 à M-12)
    FOR i IN 1..12 LOOP
        -- Calculer le mois à importer
        mois_a_importer := TO_CHAR(CURRENT_DATE - INTERVAL '1 month' * i, 'YYYY-MM');

        -- Geler tous les mois sauf M-1 (qui reste dans la fenêtre de correction)
        should_freeze := i > 1;

        -- Insérer ou mettre à jour les agrégations pour ce mois
        INSERT INTO ventes_mensuelles_agregees (
            produit_id,
            annee_mois,
            quantite_vendue,
            montant_ca,
            nombre_ventes,
            is_frozen,
            freeze_date,
            created_at,
            updated_at
        )
        SELECT
            sli.produit_id,
            mois_a_importer,
            COALESCE(SUM(sli.quantity_sold), 0) AS quantite_vendue,
            COALESCE(SUM(sli.sales_amount), 0) AS montant_ca,
            COUNT(DISTINCT s.id) AS nombre_ventes,
            should_freeze,
            CASE WHEN should_freeze THEN NOW() ELSE NULL END,
            NOW(),
            NOW()
        FROM sales_line sli
        JOIN sales s ON s.id = sli.sales_id
        WHERE s.updated_at >= (CURRENT_DATE - INTERVAL '1 month' * i)::date
          AND s.updated_at < (CURRENT_DATE - INTERVAL '1 month' * (i - 1))::date
          AND s.statut = 'CLOSED'
        GROUP BY sli.produit_id

        ON CONFLICT (produit_id, annee_mois) DO UPDATE SET
            quantite_vendue = CASE
                WHEN ventes_mensuelles_agregees.is_frozen = TRUE
                THEN ventes_mensuelles_agregees.quantite_vendue
                ELSE EXCLUDED.quantite_vendue
            END,
            montant_ca = CASE
                WHEN ventes_mensuelles_agregees.is_frozen = TRUE
                THEN ventes_mensuelles_agregees.montant_ca
                ELSE EXCLUDED.montant_ca
            END,
            nombre_ventes = CASE
                WHEN ventes_mensuelles_agregees.is_frozen = TRUE
                THEN ventes_mensuelles_agregees.nombre_ventes
                ELSE EXCLUDED.nombre_ventes
            END,
            is_frozen = EXCLUDED.is_frozen,
            freeze_date = EXCLUDED.freeze_date,
            updated_at = NOW();

        GET DIAGNOSTICS rows_affected = ROW_COUNT;
        total_rows := total_rows + rows_affected;

        RAISE NOTICE '  Mois % : % produits agrégés (frozen=%)', mois_a_importer, rows_affected, should_freeze;
    END LOOP;

    RAISE NOTICE '✅ Étape 3 terminée: % lignes d''agrégation créées pour 12 mois', total_rows;
END $$;

-- =============================================================================
-- Étape 4: Calculer le VMM et Stock Objectif initial pour tous les produits
-- Ce calcul sera ensuite automatisé via le scheduler quotidien (3h du matin)
-- =============================================================================

DO $$
DECLARE
    rec RECORD;
    vmm_value INTEGER;
    stock_objectif_value INTEGER;
    total_calculated INTEGER := 0;
BEGIN
    -- Boucle sur toutes les configurations SEMOIS
    FOR rec IN
        SELECT sc.id, sc.produit_id, sc.nb_mois_historique
        FROM semois_configuration sc
    LOOP
        -- Calcul VMM pondéré (pondération décroissante: mois récent = poids élevé)
        SELECT COALESCE(
            ROUND(
                SUM(vma.quantite_vendue * (sc.nb_mois_historique - row_number + 1)) /
                SUM(sc.nb_mois_historique - row_number + 1)
            )::INTEGER, 0
        )
        INTO vmm_value
        FROM (
            SELECT vma.quantite_vendue,
                   ROW_NUMBER() OVER (ORDER BY vma.annee_mois DESC) - 1 as row_number
            FROM ventes_mensuelles_agregees vma
            WHERE vma.produit_id = rec.produit_id
            ORDER BY vma.annee_mois DESC
            LIMIT rec.nb_mois_historique
        ) vma
        JOIN semois_configuration sc ON sc.id = rec.id;

        -- Calcul Stock Objectif = VMM + Marge de Sécurité
        -- Marge = VMM × (délai_livraison × coefficient_sécurité / 30)
        SELECT COALESCE(
            ROUND(
                vmm_value + (
                    vmm_value *
                    sc.delai_livraison_jours *
                    sc.coefficient_securite / 30.0 *
                    COALESCE(sc.facteur_saisonnier_actuel, 1.0)
                )
            )::INTEGER, 0
        )
        INTO stock_objectif_value
        FROM semois_configuration sc
        WHERE sc.id = rec.id;

        -- Appliquer limite péremption si activée (max 3 mois de VMM)
        IF (SELECT limite_peremption FROM semois_configuration WHERE id = rec.id) AND vmm_value > 0 THEN
            stock_objectif_value := LEAST(stock_objectif_value, vmm_value * 3);
        END IF;

        -- Mettre à jour la configuration
        UPDATE semois_configuration
        SET vmm_calcule = vmm_value,
            stock_objectif_calcule = stock_objectif_value,
            date_dernier_calcul = NOW(),
            updated_at = NOW()
        WHERE id = rec.id;

        total_calculated := total_calculated + 1;
    END LOOP;

    RAISE NOTICE '✅ Étape 4 terminée: % produits avec VMM et Stock Objectif calculés', total_calculated;
END $$;

-- =============================================================================
-- Étape 5: Rafraîchir la vue matérialisée (si elle existe)
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_matviews
        WHERE schemaname = 'pharma_smart'
        AND matviewname = 'mv_semois_suggestion'
    ) THEN
        REFRESH MATERIALIZED VIEW mv_semois_suggestion;
        RAISE NOTICE '✅ Étape 5 terminée: Vue matérialisée mv_semois_suggestion rafraîchie';
    ELSE
        RAISE NOTICE '⚠️  Étape 5 ignorée: Vue matérialisée mv_semois_suggestion non trouvée';
    END IF;
END $$;

-- =============================================================================
-- Résumé final
-- =============================================================================

DO $$
DECLARE
    nb_configs INTEGER;
    nb_agregations INTEGER;
    nb_mois_distincts INTEGER;
BEGIN
    SELECT COUNT(*) INTO nb_configs FROM semois_configuration;
    SELECT COUNT(*) INTO nb_agregations FROM ventes_mensuelles_agregees;
    SELECT COUNT(DISTINCT annee_mois) INTO nb_mois_distincts FROM ventes_mensuelles_agregees;

    RAISE NOTICE '';
    RAISE NOTICE '╔════════════════════════════════════════════════════════════════╗';
    RAISE NOTICE '║       🎉 IMPORT HISTORIQUE SEMOIS TERMINÉ AVEC SUCCÈS 🎉      ║';
    RAISE NOTICE '╠════════════════════════════════════════════════════════════════╣';
    RAISE NOTICE '║  Configurations SEMOIS créées: %                               ║', LPAD(nb_configs::TEXT, 5, ' ');
    RAISE NOTICE '║  Agrégations mensuelles créées: %                              ║', LPAD(nb_agregations::TEXT, 5, ' ');
    RAISE NOTICE '║  Nombre de mois importés: %                                    ║', LPAD(nb_mois_distincts::TEXT, 5, ' ');
    RAISE NOTICE '╠════════════════════════════════════════════════════════════════╣';
    RAISE NOTICE '║  PROCHAINES ÉTAPES:                                            ║';
    RAISE NOTICE '║  1. Vérifier les configurations par classe de criticité       ║';
    RAISE NOTICE '║  2. Ajuster manuellement les classes A+, A, C, D selon ABC    ║';
    RAISE NOTICE '║  3. Les recalculs quotidiens démarreront automatiquement      ║';
    RAISE NOTICE '║     - Agrégation ventes: 2h du matin                          ║';
    RAISE NOTICE '║     - Recalcul SEMOIS: 3h du matin                            ║';
    RAISE NOTICE '╚════════════════════════════════════════════════════════════════╝';
    RAISE NOTICE '';
END $$;
