-- ============================================================================
-- Migration V1.1.22 - Tables SEMOIS
-- Stock Économique Mensuel d'Objectif Interne de Sécurité
-- Date: 2025-12-20
-- ============================================================================

-- Table des ventes mensuelles agrégées
-- Note: Utilise IDENTITY (PostgreSQL 10+) au lieu de SERIAL (déprécié PostgreSQL 18+)
CREATE TABLE ventes_mensuelles_agregees (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    produit_id INTEGER NOT NULL,
    annee_mois VARCHAR(7) NOT NULL, -- Format: 'YYYY-MM' ex: '2025-12'

    -- Métriques agrégées
    quantite_vendue INTEGER NOT NULL DEFAULT 0,
    montant_ca INTEGER NOT NULL DEFAULT 0, -- Montant en centimes
    nombre_ventes INTEGER NOT NULL DEFAULT 0,

    -- Gestion du gel (fenêtre de stabilisation 7 jours)
    is_frozen BOOLEAN NOT NULL DEFAULT FALSE,
    freeze_date TIMESTAMP,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Contraintes
    CONSTRAINT fk_ventes_agregees_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE,
    CONSTRAINT uq_ventes_agregees_produit_mois UNIQUE (produit_id, annee_mois),
    CONSTRAINT ck_ventes_agregees_quantite CHECK (quantite_vendue >= 0),
    CONSTRAINT ck_ventes_agregees_montant CHECK (montant_ca >= 0),
    CONSTRAINT ck_ventes_agregees_nb_ventes CHECK (nombre_ventes >= 0)
);

-- Index pour performance
CREATE INDEX idx_ventes_mensuelles_produit ON ventes_mensuelles_agregees(produit_id);
CREATE INDEX idx_ventes_mensuelles_date ON ventes_mensuelles_agregees(annee_mois);
CREATE INDEX idx_ventes_mensuelles_frozen ON ventes_mensuelles_agregees(is_frozen, annee_mois);
CREATE INDEX idx_ventes_mensuelles_updated ON ventes_mensuelles_agregees(updated_at);

COMMENT ON TABLE ventes_mensuelles_agregees IS 'Agrégation mensuelle des ventes pour calcul SEMOIS (VMM)';
COMMENT ON COLUMN ventes_mensuelles_agregees.annee_mois IS 'Format YYYY-MM, ex: 2025-12';
COMMENT ON COLUMN ventes_mensuelles_agregees.is_frozen IS 'Mois gelé après J+7, immuable';
COMMENT ON COLUMN ventes_mensuelles_agregees.freeze_date IS 'Date du gel définitif du mois';

-- ============================================================================

-- Table de configuration SEMOIS par produit
CREATE TABLE semois_configuration (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    produit_id INTEGER NOT NULL,

    -- Classification criticité
    classe_criticite VARCHAR(3) NOT NULL DEFAULT 'B', -- A+, A, B, C, D
    coefficient_securite DECIMAL(3,2) NOT NULL DEFAULT 1.0,

    -- Paramètres de calcul
    nb_mois_historique INTEGER NOT NULL DEFAULT 6, -- Nombre de mois pour calcul VMM
    delai_livraison_jours INTEGER NOT NULL DEFAULT 7,

    -- Cache des derniers calculs
    stock_objectif_calcule INTEGER,
    vmm_calcule INTEGER,
    date_dernier_calcul TIMESTAMP,

    -- Ajustements
    facteur_saisonnier_actuel DECIMAL(3,2) DEFAULT 1.0,
    limite_peremption BOOLEAN DEFAULT FALSE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Contraintes
    CONSTRAINT fk_semois_config_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE,
    CONSTRAINT uq_semois_config_produit UNIQUE (produit_id),
    CONSTRAINT ck_semois_classe CHECK (classe_criticite IN ('A+', 'A', 'B', 'C', 'D')),
    CONSTRAINT ck_semois_coefficient CHECK (coefficient_securite >= 0.1 AND coefficient_securite <= 2.0),
    CONSTRAINT ck_semois_nb_mois CHECK (nb_mois_historique >= 3 AND nb_mois_historique <= 12),
    CONSTRAINT ck_semois_delai CHECK (delai_livraison_jours >= 1 AND delai_livraison_jours <= 90),
    CONSTRAINT ck_semois_facteur_saison CHECK (facteur_saisonnier_actuel > 0 AND facteur_saisonnier_actuel <= 3.0)
);

-- Index pour performance
CREATE INDEX idx_semois_config_produit ON semois_configuration(produit_id);
CREATE INDEX idx_semois_config_classe ON semois_configuration(classe_criticite);
CREATE INDEX idx_semois_config_updated ON semois_configuration(updated_at);

COMMENT ON TABLE semois_configuration IS 'Configuration SEMOIS par produit (criticité, coefficients)';
COMMENT ON COLUMN semois_configuration.classe_criticite IS 'A+=Vital, A=Forte rotation, B=Moyenne, C=Faible, D=Très faible';
COMMENT ON COLUMN semois_configuration.coefficient_securite IS 'Coefficient de sécurité (0.2 à 2.0)';
COMMENT ON COLUMN semois_configuration.vmm_calcule IS 'Ventes Mensuelles Moyennes (cache)';
COMMENT ON COLUMN semois_configuration.stock_objectif_calcule IS 'Stock objectif SEMOIS (cache)';

-- ============================================================================

-- Vue matérialisée pour calcul SEMOIS temps réel (optionnelle, pour performance)
CREATE MATERIALIZED VIEW mv_semois_suggestion AS
SELECT
    p.id AS produit_id,
    p.libelle,
    fp.code_cip,
    sc.classe_criticite,
    sc.coefficient_securite,
    sc.delai_livraison_jours,

    -- VMM (Ventes Mensuelles Moyennes pondérées)
    COALESCE(
        (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / NULLIF(SUM(7 - row_num), 0)
         FROM (
             SELECT quantite_vendue,
                    ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
             FROM ventes_mensuelles_agregees
             WHERE produit_id = p.id
               AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
         ) vma
         WHERE vma.row_num <= 6),
        0
    )::INTEGER AS vmm,

    -- Marge de sécurité
    (COALESCE(
        (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / NULLIF(SUM(7 - row_num), 0)
         FROM (
             SELECT quantite_vendue,
                    ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
             FROM ventes_mensuelles_agregees
             WHERE produit_id = p.id
               AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
         ) vma
         WHERE vma.row_num <= 6),
        0
    ) * (sc.delai_livraison_jours * sc.coefficient_securite / 30.0))::INTEGER AS marge_securite,

    -- Stock objectif
    (COALESCE(
        (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / NULLIF(SUM(7 - row_num), 0)
         FROM (
             SELECT quantite_vendue,
                    ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
             FROM ventes_mensuelles_agregees
             WHERE produit_id = p.id
               AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
         ) vma
         WHERE vma.row_num <= 6),
        0
    ) * (1 + (sc.delai_livraison_jours * sc.coefficient_securite / 30.0)))::INTEGER AS stock_objectif,

    -- Stock actuel
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel,

    -- Quantité à commander
    GREATEST(
        0,
        (COALESCE(
            (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / NULLIF(SUM(7 - row_num), 0)
             FROM (
                 SELECT quantite_vendue,
                        ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
                 FROM ventes_mensuelles_agregees
                 WHERE produit_id = p.id
                   AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
             ) vma
             WHERE vma.row_num <= 6),
            0
        ) * (1 + (sc.delai_livraison_jours * sc.coefficient_securite / 30.0)))::INTEGER
        - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
    ) AS quantite_a_commander,

    -- Métadonnées
    sc.date_dernier_calcul,
    NOW() AS vue_refresh_date

FROM produit p
LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
LEFT JOIN semois_configuration sc ON sc.produit_id = p.id
LEFT JOIN stock_produit sp ON sp.produit_id = p.id
WHERE p.status = 'ENABLE'
  AND p.type_produit != 'DETAIL'
  AND sc.id IS NOT NULL  -- Seulement produits avec config SEMOIS
GROUP BY p.id, p.libelle, fp.code_cip, sc.classe_criticite,
         sc.coefficient_securite, sc.delai_livraison_jours, sc.date_dernier_calcul;

-- Index pour performance de la vue matérialisée
CREATE INDEX idx_mv_semois_produit ON mv_semois_suggestion(produit_id);
CREATE INDEX idx_mv_semois_classe ON mv_semois_suggestion(classe_criticite);
CREATE INDEX idx_mv_semois_qte_commander ON mv_semois_suggestion(quantite_a_commander) WHERE quantite_a_commander > 0;

COMMENT ON MATERIALIZED VIEW mv_semois_suggestion IS 'Vue matérialisée des suggestions SEMOIS (refresh quotidien)';

-- ============================================================================

-- Fonction pour initialiser configuration SEMOIS pour tous les produits
-- (À exécuter manuellement ou via script après migration)
CREATE OR REPLACE FUNCTION init_semois_configurations()
RETURNS INTEGER AS $$
DECLARE
    nb_created INTEGER := 0;
BEGIN
    -- Créer config par défaut pour tous les produits actifs sans config
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
        'B'::VARCHAR(3), -- Classe par défaut: rotation moyenne
        1.0, -- Coefficient par défaut
        6, -- 6 mois d'historique
        7, -- 7 jours de délai livraison
        1.0, -- Pas d'ajustement saisonnier
        FALSE,
        NOW(),
        NOW()
    FROM produit p
    WHERE p.status = 'ENABLE'
      AND p.type_produit != 'DETAIL'
      AND NOT EXISTS (
          SELECT 1 FROM semois_configuration sc WHERE sc.produit_id = p.id
      );

    GET DIAGNOSTICS nb_created = ROW_COUNT;

    RETURN nb_created;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION init_semois_configurations() IS 'Initialise config SEMOIS pour tous produits actifs (classe B par défaut)';

-- ============================================================================
-- Trigger pour mettre à jour updated_at automatiquement
-- ============================================================================

CREATE OR REPLACE FUNCTION update_semois_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_semois_config_updated_at
    BEFORE UPDATE ON semois_configuration
    FOR EACH ROW
    EXECUTE FUNCTION update_semois_updated_at();

CREATE TRIGGER trigger_ventes_agregees_updated_at
    BEFORE UPDATE ON ventes_mensuelles_agregees
    FOR EACH ROW
    EXECUTE FUNCTION update_semois_updated_at();

-- ============================================================================
-- Données de test optionnelles (commentées par défaut)
-- ============================================================================

-- Exemple: Initialiser les configurations SEMOIS pour tous les produits
-- SELECT init_semois_configurations();

-- Exemple: Refresh de la vue matérialisée
-- REFRESH MATERIALIZED VIEW mv_semois_suggestion;
