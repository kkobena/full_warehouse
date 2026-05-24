# Script d'Import Historique SEMOIS

## Vue d'ensemble

Ce dossier contient le script SQL d'initialisation du système SEMOIS (Stock Économique Mensuel d'Objectif Interne de Sécurité) pour la gestion des stocks de pharmacie.

## Qu'est-ce que SEMOIS ?

SEMOIS est une méthode de gestion des stocks adaptée aux pharmacies qui calcule automatiquement:

- **VMM (Ventes Mensuelles Moyennes)**: Moyenne pondérée des ventes sur N mois (les mois récents ont plus de poids)
- **Marge de Sécurité**: Stock tampon pour éviter les ruptures pendant le délai de livraison
- **Stock Objectif**: VMM + Marge de Sécurité
- **Quantité à Commander**: MAX(0, Stock Objectif - Stock Actuel)

### Formules SEMOIS

```
VMM = Σ(Ventes_mois_i × Poids_i) / Σ(Poids_i)
      où Poids décroît du mois le plus récent au plus ancien

Marge de Sécurité = VMM × (Délai_livraison_jours × Coefficient_sécurité / 30) × Facteur_saisonnier

Stock Objectif = VMM + Marge de Sécurité
                 (avec limite péremption optionnelle: MIN(Stock Objectif, VMM × 3))

Quantité à Commander = MAX(0, Stock Objectif - Stock Actuel)
```

## Prérequis

1. **Migration Flyway V1.1.22**: Les tables SEMOIS doivent exister
   - `ventes_mensuelles_agregees`
   - `semois_configuration`

2. **Données de ventes**: Au moins 12 mois de données dans les tables `sales` et `sales_line`

3. **PostgreSQL 18+**: Le script utilise la syntaxe moderne (IDENTITY, etc.)

## Installation

### Option 1: Via Script SQL (Recommandé pour déploiement initial)

```bash
# Se connecter à PostgreSQL
psql -U warehouse -d warehouse

# Exécuter le script
\i src/main/resources/scripts/semois/import_semois_historique.sql
```

### Option 2: Via REST API (Recommandé pour usage quotidien)

Une fois l'application démarrée, utilisez les endpoints REST:

```bash
# 1. Initialiser toutes les configurations SEMOIS
curl -X POST http://localhost:8080/api/semois/init-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# 2. Importer 12 mois de données historiques
curl -X POST http://localhost:8080/api/semois/import-historical \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nbMois": 12}'

# 3. Déclencher le premier calcul SEMOIS
curl -X POST http://localhost:8080/api/semois/recalculate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Ce que fait le script

### Étape 1: Vérification des tables
- Vérifie que les tables `ventes_mensuelles_agregees` et `semois_configuration` existent
- Erreur si migration Flyway non exécutée

### Étape 2: Initialisation des configurations
- Crée une configuration SEMOIS pour chaque produit actif
- Classe par défaut: **B (Rotation moyenne)**
- Paramètres par défaut:
  - Coefficient de sécurité: 1.0
  - Nombre de mois historique: 6
  - Délai de livraison: 7 jours
  - Facteur saisonnier: 1.0 (neutre)
  - Limite péremption: Non activée

### Étape 3: Import des 12 derniers mois
- Agrège les ventes mensuelles pour chaque produit
- **Gel automatique**:
  - Mois M-2 à M-12: Gelés immédiatement (données historiques stables)
  - Mois M-1: Non gelé (fenêtre de correction J+7)
  - Mois M (en cours): Non gelé (recalcul quotidien automatique)

### Étape 4: Calcul initial VMM et Stock Objectif
- Calcule le VMM pondéré pour chaque produit
- Calcule le Stock Objectif (VMM + Marge de Sécurité)
- Applique limite péremption si activée
- Met à jour les configurations

### Étape 5: Rafraîchissement vue matérialisée
- Rafraîchit `mv_semois_suggestion` si elle existe
- Ignore si la vue n'existe pas (optionnelle)

## Après l'import

### 1. Vérifier les résultats

```sql
-- Nombre de configurations créées
SELECT COUNT(*) FROM pharma_smart.semois_configuration;

-- Répartition par classe de criticité
SELECT classe_criticite, COUNT(*)
FROM pharma_smart.semois_configuration
GROUP BY classe_criticite
ORDER BY classe_criticite;

-- Nombre d'agrégations par mois
SELECT annee_mois, COUNT(*), SUM(quantite_vendue) AS total_ventes
FROM pharma_smart.ventes_mensuelles_agregees
GROUP BY annee_mois
ORDER BY annee_mois DESC;

-- Mois gelés vs non gelés
SELECT
    COUNT(*) FILTER (WHERE is_frozen = TRUE) AS mois_geles,
    COUNT(*) FILTER (WHERE is_frozen = FALSE) AS mois_non_geles
FROM pharma_smart.ventes_mensuelles_agregees;
```

### 2. Ajuster les classes de criticité

Par défaut, tous les produits sont en classe **B**. Ajustez selon l'analyse ABC:

```sql
-- Classe A+ (Produits vitaux, coefficient 1.5)
UPDATE pharma_smart.semois_configuration
SET classe_criticite = 'A_PLUS',
    coefficient_securite = 1.5
WHERE produit_id IN (
    SELECT id FROM pharma_smart.produit
    WHERE libelle LIKE '%INSULINE%'
       OR libelle LIKE '%ADRENALINE%'
       OR libelle LIKE '%VENTOLINE%'
);

-- Classe A (Forte rotation, coefficient 1.0)
-- Top 20% des ventes
UPDATE pharma_smart.semois_configuration sc
SET classe_criticite = 'A',
    coefficient_securite = 1.0
WHERE sc.produit_id IN (
    SELECT vma.produit_id
    FROM pharma_smart.ventes_mensuelles_agregees vma
    WHERE vma.annee_mois >= TO_CHAR(CURRENT_DATE - INTERVAL '6 months', 'YYYY-MM')
    GROUP BY vma.produit_id
    ORDER BY SUM(vma.quantite_vendue) DESC
    LIMIT (SELECT COUNT(*) * 0.2 FROM pharma_smart.semois_configuration)::INTEGER
);

-- Classe C (Faible rotation, coefficient 0.4)
UPDATE pharma_smart.semois_configuration sc
SET classe_criticite = 'C',
    coefficient_securite = 0.4
WHERE vmm_calcule > 0 AND vmm_calcule < 5; -- Moins de 5 ventes/mois

-- Classe D (Très faible rotation, coefficient 0.2)
UPDATE pharma_smart.semois_configuration sc
SET classe_criticite = 'D',
    coefficient_securite = 0.2
WHERE vmm_calcule = 0 OR vmm_calcule < 2; -- Moins de 2 ventes/mois
```

### 3. Activer limite péremption pour produits périssables

```sql
-- Produits avec DLU courte (ex: moins de 6 mois)
UPDATE pharma_smart.semois_configuration sc
SET limite_peremption = TRUE
WHERE sc.produit_id IN (
    SELECT p.id FROM pharma_smart.produit p
    WHERE p.type_produit IN ('SERUM', 'VACCIN', 'INSULINE')
);
```

### 4. Ajuster délais de livraison par fournisseur

```sql
-- Délais longs pour fournisseurs étrangers (exemple)
UPDATE pharma_smart.semois_configuration sc
SET delai_livraison_jours = 14
WHERE sc.produit_id IN (
    SELECT p.id FROM pharma_smart.produit p
    JOIN pharma_smart.fournisseur_produit fp ON fp.produit_id = p.id
    JOIN pharma_smart.fournisseur f ON f.id = fp.fournisseur_id
    WHERE f.libelle LIKE '%IMPORT%' OR f.libelle LIKE '%INTERNATIONAL%'
);
```

## Automatisation quotidienne

Une fois l'import initial effectué, le système fonctionne automatiquement:

| Heure | Tâche | Scheduler |
|-------|-------|-----------|
| 2h00 | Agrégation des ventes mensuelles | `VentesAgregeesService.aggregateMonthlySalesDaily()` |
| 3h00 | Recalcul SEMOIS (VMM, Stock Objectif) | `SemoisCalculationService.recalculateAllConfigurations()` |

### Configuration des schedulers

Dans `application.yml`:

```yaml
application:
  semois:
    freeze-delay-days: 7 # Fenêtre de correction après fin de mois
    aggregation-cron: '0 0 2 * * *' # 2h du matin
    recalculation-cron: '0 0 3 * * *' # 3h du matin
```

## Endpoints REST API

### Consultation

```bash
# Obtenir toutes les suggestions de réapprovisionnement
GET /api/semois/suggestions
GET /api/semois/suggestions?search=DOLIPRANE
GET /api/semois/suggestions?classeCriticite=A_PLUS

# Obtenir suggestion pour un produit spécifique
GET /api/semois/suggestions/{produitId}

# Obtenir configuration SEMOIS d'un produit
GET /api/semois/configuration/{produitId}

# Statut de l'agrégation mensuelle
GET /api/semois/aggregation/status
```

### Administration (ROLE_ADMIN requis)

```bash
# Initialiser configuration pour un produit
POST /api/semois/configuration
Body: {"produitId": 123, "classeCriticite": "A"}

# Mettre à jour configuration
PUT /api/semois/configuration/{produitId}

# Initialiser toutes les configurations manquantes
POST /api/semois/init-all

# Importer historique
POST /api/semois/import-historical
Body: {"nbMois": 12}

# Déclencher recalcul manuel
POST /api/semois/recalculate

# Dégeler un mois (exceptionnel)
POST /api/semois/aggregation/unfreeze
Body: {"anneeMois": "2025-11", "reason": "Correction annulation tardive"}
```

## Monitoring

### Requêtes de surveillance

```sql
-- Top 20 produits à réapprovisionner
SELECT
    p.libelle,
    sc.classe_criticite,
    sc.vmm_calcule AS vmm,
    sc.stock_objectif_calcule AS stock_objectif,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel,
    GREATEST(0, sc.stock_objectif_calcule - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)) AS qte_a_commander
FROM pharma_smart.semois_configuration sc
JOIN pharma_smart.produit p ON p.id = sc.produit_id
LEFT JOIN pharma_smart.stock_produit sp ON sp.produit_id = p.id
GROUP BY p.id, p.libelle, sc.classe_criticite, sc.vmm_calcule, sc.stock_objectif_calcule
HAVING sc.stock_objectif_calcule > COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
ORDER BY GREATEST(0, sc.stock_objectif_calcule - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)) DESC
LIMIT 20;

-- Produits en rupture potentielle (stock < marge sécurité)
SELECT
    p.libelle,
    sc.classe_criticite,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel,
    ROUND(sc.vmm_calcule * sc.delai_livraison_jours * sc.coefficient_securite / 30.0) AS marge_securite
FROM pharma_smart.semois_configuration sc
JOIN pharma_smart.produit p ON p.id = sc.produit_id
LEFT JOIN pharma_smart.stock_produit sp ON sp.produit_id = p.id
GROUP BY p.id, p.libelle, sc.classe_criticite, sc.vmm_calcule, sc.delai_livraison_jours, sc.coefficient_securite
HAVING COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) < ROUND(sc.vmm_calcule * sc.delai_livraison_jours * sc.coefficient_securite / 30.0)
ORDER BY COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) ASC;

-- Produits en surstock (> 150% du stock objectif)
SELECT
    p.libelle,
    sc.classe_criticite,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel,
    sc.stock_objectif_calcule AS stock_objectif,
    ROUND(100.0 * COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) / NULLIF(sc.stock_objectif_calcule, 0)) AS pourcentage_stock
FROM pharma_smart.semois_configuration sc
JOIN pharma_smart.produit p ON p.id = sc.produit_id
LEFT JOIN pharma_smart.stock_produit sp ON sp.produit_id = p.id
WHERE sc.stock_objectif_calcule > 0
GROUP BY p.id, p.libelle, sc.classe_criticite, sc.stock_objectif_calcule
HAVING COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) > sc.stock_objectif_calcule * 1.5
ORDER BY COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) DESC;
```

## Dépannage

### Problème: Aucune agrégation créée pour certains produits

**Cause**: Produit sans ventes dans la période
**Solution**: Normal, le produit aura VMM = 0 et suggestions à 0

### Problème: VMM semble incorrect

**Vérification**:
```sql
-- Détail des ventes mensuelles d'un produit
SELECT annee_mois, quantite_vendue, montant_ca, is_frozen
FROM pharma_smart.ventes_mensuelles_agregees
WHERE produit_id = 123
ORDER BY annee_mois DESC;

-- Recalcul manuel VMM
SELECT
    ROUND(
        SUM(quantite_vendue * (7 - row_number)) /
        SUM(7 - row_number)
    ) AS vmm_recalcule
FROM (
    SELECT quantite_vendue,
           ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_number
    FROM pharma_smart.ventes_mensuelles_agregees
    WHERE produit_id = 123
    ORDER BY annee_mois DESC
    LIMIT 6
) sub;
```

### Problème: Mois gelé par erreur

**Solution**: Utiliser l'endpoint de dégel (admin uniquement)
```bash
curl -X POST http://localhost:8080/api/semois/aggregation/unfreeze \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"anneeMois": "2025-11", "reason": "Correction erreur gel prématuré"}'
```

## Support

Pour toute question ou problème:
1. Consulter les logs de l'application (`logs/pharma-smart.log`)
2. Vérifier les jobs schedulés dans les logs (niveau INFO)
3. Consulter la documentation SEMOIS dans `PLAN_IMPLEMENTATION_SEMOIS.md`

## Références

- [PLAN_IMPLEMENTATION_SEMOIS.md](../../../../../PLAN_IMPLEMENTATION_SEMOIS.md) - Plan complet SEMOIS
- [SEMOIS_AGREGATION_STRATEGY.md](../../../../../SEMOIS_AGREGATION_STRATEGY.md) - Stratégie d'agrégation
- Migration Flyway: `V1.1.22__semois_tables.sql`
