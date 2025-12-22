# Plan de Classification Dynamique de la Criticité des Produits

## 📋 Vue d'ensemble

Ce document décrit la stratégie de mise à jour automatique du champ `Produit.classeCriticite` basée sur l'analyse de la rotation de stock et du chiffre d'affaires. Cette classification dynamique alimente directement le système SEMOIS pour optimiser les paramètres de réapprovisionnement.

---

## 🎯 Objectifs

1. **Automatiser** la classification des produits (A+, A, B, C, D) selon leur performance réelle
2. **Cohérence** avec le système SEMOIS existant (VMM, agrégations mensuelles)
3. **Stabilité** : éviter les reclassifications trop fréquentes qui déstabilisent les stocks
4. **Traçabilité** : historique des changements de classe pour audit

---

## 📊 Méthodologie de Classification

### 1. Critères de Classification (Méthode Multi-Critères)

La classification combine **3 indicateurs** avec pondération :

| Critère | Poids | Description |
|---------|-------|-------------|
| **Chiffre d'affaires** | 50% | Contribution au CA total (méthode ABC classique) |
| **Rotation de stock** | 30% | Nombre de rotations annuelles (Ventes annuelles / Stock moyen) |
| **Fréquence de vente** | 20% | Nombre de mois avec ventes sur les 12 derniers mois |

### 2. Calcul du Score de Criticité

```
Score = (0.5 × Score_CA) + (0.3 × Score_Rotation) + (0.2 × Score_Fréquence)
```

Chaque sous-score est normalisé entre 0 et 100.

### 3. Grille de Classification

| Classe | Score | Rotation annuelle | Contribution CA | Description |
|--------|-------|-------------------|-----------------|-------------|
| **A+** | ≥ 90 | ≥ 12 rotations/an | Top 5% du CA | Produits vitaux, forte rotation |
| **A**  | 80-89 | 8-11 rotations/an | Top 20% du CA | Produits importants |
| **B**  | 60-79 | 4-7 rotations/an | 20-50% du CA cumulé | Produits moyens |
| **C**  | 40-59 | 2-3 rotations/an | 50-80% du CA cumulé | Rotation faible |
| **D**  | < 40 | < 2 rotations/an | Dernier 20% du CA | Très faible rotation |

### 4. Cas Particuliers

- **Produits nouveaux** (< 6 mois) : Classe **B** par défaut, réévaluation après 6 mois(La fréquence doit être configurable ex: 3 ou 6 mois)
- **Produits saisonniers** : Analyse sur 12 mois complets avec facteur saisonnier
- **Produits en rupture prolongée** : Exclus temporairement de la reclassification
- **Produits périmés/discontinués** : Classe **D** automatique

---

## 🏗️ Architecture Technique

### 1. Nouvelles Tables

#### Table `classification_criticite_log`
```sql
CREATE TABLE classification_criticite_log (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    produit_id INTEGER NOT NULL,

    -- Ancienne et nouvelle classe
    ancienne_classe VARCHAR(3),
    nouvelle_classe VARCHAR(3) NOT NULL,

    -- Métriques ayant justifié le changement
    vmm_12_mois INTEGER,
    ca_12_mois BIGINT, -- en centimes
    rotation_annuelle DECIMAL(6,2),
    frequence_vente_mois INTEGER, -- nb de mois avec ventes sur 12
    score_total DECIMAL(5,2),

    -- Contexte
    raison_changement VARCHAR(255),
    classification_type VARCHAR(20) DEFAULT 'AUTO', -- AUTO, MANUAL, INITIAL

    -- Audit
    user_id INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_classif_log_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
);

CREATE INDEX idx_classif_log_produit ON classification_criticite_log(produit_id);
CREATE INDEX idx_classif_log_date ON classification_criticite_log(created_at);
CREATE INDEX idx_classif_log_type ON classification_criticite_log(classification_type);
```

#### Table `classification_config` (Paramètres configurables)
```sql
CREATE TABLE classification_config (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,

    -- Poids des critères (doivent sommer à 1.0)
    poids_ca DECIMAL(3,2) NOT NULL DEFAULT 0.50,
    poids_rotation DECIMAL(3,2) NOT NULL DEFAULT 0.30,
    poids_frequence DECIMAL(3,2) NOT NULL DEFAULT 0.20,

    -- Seuils de score pour chaque classe
    seuil_a_plus INTEGER NOT NULL DEFAULT 90,
    seuil_a INTEGER NOT NULL DEFAULT 80,
    seuil_b INTEGER NOT NULL DEFAULT 60,
    seuil_c INTEGER NOT NULL DEFAULT 40,
    -- D = tout ce qui est < seuil_c

    -- Seuils de rotation annuelle
    rotation_a_plus DECIMAL(5,2) NOT NULL DEFAULT 12.0,
    rotation_a DECIMAL(5,2) NOT NULL DEFAULT 8.0,
    rotation_b DECIMAL(5,2) NOT NULL DEFAULT 4.0,
    rotation_c DECIMAL(5,2) NOT NULL DEFAULT 2.0,

    -- Périodes d'analyse
    nb_mois_analyse INTEGER NOT NULL DEFAULT 12,
    nb_mois_min_nouveau_produit INTEGER NOT NULL DEFAULT 6,

    -- Stabilité (éviter reclassifications trop fréquentes)
    changement_min_score INTEGER NOT NULL DEFAULT 10, -- écart minimum de score pour changer de classe

    -- Audit
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by INTEGER,

    CONSTRAINT ck_classif_poids_sum CHECK (poids_ca + poids_rotation + poids_frequence = 1.0)
);

-- Insérer configuration par défaut
INSERT INTO classification_config (poids_ca, poids_rotation, poids_frequence)
VALUES (0.50, 0.30, 0.20);
```

### 2. Nouvelles Vues Analytiques

#### Vue `v_produit_metriques_classification`
```sql
CREATE OR REPLACE VIEW v_produit_metriques_classification AS
SELECT
    p.id AS produit_id,
    p.libelle,
    p.classe_criticite AS classe_actuelle,

    -- VMM sur 12 mois
    COALESCE(
        (SELECT AVG(quantite_vendue)
         FROM ventes_mensuelles_agregees vma
         WHERE vma.produit_id = p.id
           AND vma.annee_mois >= TO_CHAR(NOW() - INTERVAL '12 months', 'YYYY-MM')
           AND vma.is_frozen = TRUE
        ), 0
    ) AS vmm_12_mois,

    -- CA total sur 12 mois (en centimes)
    COALESCE(
        (SELECT SUM(montant_ca)
         FROM ventes_mensuelles_agregees vma
         WHERE vma.produit_id = p.id
           AND vma.annee_mois >= TO_CHAR(NOW() - INTERVAL '12 months', 'YYYY-MM')
           AND vma.is_frozen = TRUE
        ), 0
    ) AS ca_12_mois,

    -- Quantité vendue totale sur 12 mois
    COALESCE(
        (SELECT SUM(quantite_vendue)
         FROM ventes_mensuelles_agregees vma
         WHERE vma.produit_id = p.id
           AND vma.annee_mois >= TO_CHAR(NOW() - INTERVAL '12 months', 'YYYY-MM')
           AND vma.is_frozen = TRUE
        ), 0
    ) AS qte_vendue_12_mois,

    -- Fréquence de vente (nb de mois avec ventes)
    COALESCE(
        (SELECT COUNT(*)
         FROM ventes_mensuelles_agregees vma
         WHERE vma.produit_id = p.id
           AND vma.annee_mois >= TO_CHAR(NOW() - INTERVAL '12 months', 'YYYY-MM')
           AND vma.is_frozen = TRUE
           AND vma.quantite_vendue > 0
        ), 0
    ) AS frequence_vente_mois,

    -- Stock moyen sur 12 mois (approximatif via stock actuel)
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel,

    -- Rotation annuelle = Ventes annuelles / Stock moyen
    CASE
        WHEN COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) > 0 THEN
            COALESCE(
                (SELECT SUM(quantite_vendue)
                 FROM ventes_mensuelles_agregees vma
                 WHERE vma.produit_id = p.id
                   AND vma.annee_mois >= TO_CHAR(NOW() - INTERVAL '12 months', 'YYYY-MM')
                   AND vma.is_frozen = TRUE
                ), 0
            )::DECIMAL / COALESCE(SUM(sp.qty_stock + sp.qty_ug), 1)
        ELSE 0
    END AS rotation_annuelle,

    -- Ancienneté du produit (en mois)
    EXTRACT(YEAR FROM AGE(NOW(), p.created_at)) * 12 +
    EXTRACT(MONTH FROM AGE(NOW(), p.created_at)) AS anciennete_mois,

    -- Flags
    CASE
        WHEN EXTRACT(YEAR FROM AGE(NOW(), p.created_at)) * 12 +
             EXTRACT(MONTH FROM AGE(NOW(), p.created_at)) < 6
        THEN TRUE
        ELSE FALSE
    END AS est_nouveau_produit,

    p.created_at AS date_creation_produit

FROM produit p
LEFT JOIN stock_produit sp ON sp.produit_id = p.id
WHERE p.status = 'ENABLE'
  AND p.type_produit != 'DETAIL'
GROUP BY p.id, p.libelle, p.classe_criticite, p.created_at;
```

### 3. Services Java

#### `ClassificationCriticiteService.java`

**Responsabilités** :
- Calculer le score de criticité pour chaque produit
- Appliquer la grille de classification
- Détecter et appliquer les changements de classe
- Logger les changements

**Méthodes principales** :
```java
public class ClassificationCriticiteService {

    /**
     * Reclassifie tous les produits selon leurs métriques de rotation
     * Exécuté mensuellement après le gel du mois précédent
     */
    @Scheduled(cron = "0 0 4 8 * ?") // 8e jour de chaque mois à 4h du matin
    @Transactional
    public ReclassificationResult reclassifierTousProduits();

    /**
     * Calcule le score de criticité pour un produit
     * @return Score entre 0 et 100
     */
    public ClassificationScore calculerScore(Integer produitId);

    /**
     * Détermine la classe de criticité basée sur le score
     */
    public ClasseCriticite determinerClasse(ClassificationScore score);

    /**
     * Applique un changement de classe avec logging
     */
    @Transactional
    public void appliquerChangementClasse(
        Integer produitId,
        ClasseCriticite nouvelleClasse,
        ClassificationScore score,
        String raison
    );
}
```

#### DTO `ClassificationScore`
```java
public record ClassificationScore(
    Integer produitId,

    // Métriques brutes
    Long ca12Mois,
    Integer vmm12Mois,
    Integer qteVendue12Mois,
    Integer frequenceVenteMois,
    BigDecimal rotationAnnuelle,
    Integer stockActuel,

    // Scores normalisés (0-100)
    int scoreCA,
    int scoreRotation,
    int scoreFrequence,

    // Score final pondéré
    int scoreTotal,

    // Classe suggérée
    ClasseCriticite classeSuggeree,
    ClasseCriticite classeActuelle,

    // Flags
    boolean estNouveauProduit,
    boolean changementSignificatif
) {}
```

---

## ⏰ Fréquence et Déclenchement

### 1. Fréquence Recommandée : **MENSUELLE**

**Justification** :
- ✅ **Stabilité** : Évite les fluctuations trop fréquentes des paramètres SEMOIS
- ✅ **Cohérence** : Synchronisé avec le gel mensuel des agrégations (J+7)
- ✅ **Performance** : Traitement batch lourd, pas nécessaire plus souvent
- ✅ **Business** : La criticité d'un produit ne change pas drastiquement en quelques jours

### 2. Calendrier d'Exécution

```
Jour 1-7 du mois M : Agrégation continue du mois M-1
Jour 8 du mois M    : Gel automatique du mois M-1 (J+7)
Jour 8 à 4h00       : Reclassification automatique de criticité
Jour 9 à 3h00       : Recalcul SEMOIS quotidien (utilise nouvelles classes)
```

### 3. Configuration Spring Scheduler

```java
@Scheduled(cron = "0 0 4 8 * ?") // 8e jour de chaque mois à 4h du matin
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void reclassifierTousProduits() {
    LOG.info("🔄 Démarrage reclassification mensuelle criticité produits");

    // Vérifier que le mois précédent est bien gelé
    YearMonth dernierMois = YearMonth.now().minusMonths(1);
    if (!ventesAgregeesService.isMonthFrozen(dernierMois)) {
        LOG.warn("⚠️ Mois {} non gelé, reclassification annulée", dernierMois);
        return;
    }

    // Traitement par lots
    processReclassificationParBatch();

    LOG.info("✅ Reclassification mensuelle terminée");
}
```

### 4. Déclenchement Manuel (Admin)

Endpoint REST pour forcer une reclassification hors calendrier :
```java
@PostMapping("/api/classification/recalculate")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<ReclassificationResult> forceReclassification(
    @RequestParam(required = false) String reason
) {
    ReclassificationResult result = classificationService.reclassifierTousProduits();
    return ResponseEntity.ok(result);
}
```

---

## 🔧 Implémentation Étape par Étape

### Phase 1 : Fondations (Semaine 1)

**1.1 Migration Base de Données**
- `V1.1.25__classification_criticite_tables.sql`
  - Créer `classification_criticite_log`
  - Créer `classification_config`
  - Créer vue `v_produit_metriques_classification`
  - Insérer configuration par défaut

**1.2 Entités JPA**
- `ClassificationCriticiteLog.java` (entité de log)
- `ClassificationConfig.java` (entité de configuration)
- `ProduitMetriquesClassification.java` (vue en lecture seule)

**1.3 Repositories**
- `ClassificationCriticiteLogRepository.java`
- `ClassificationConfigRepository.java`
- `ProduitMetriquesClassificationRepository.java`

### Phase 2 : Logique Métier (Semaine 2)

**2.1 Service de Calcul**
- `ClassificationCriticiteService.java`
  - Méthode `calculerScore(produitId)`
  - Méthode `determinerClasse(score)`
  - Méthode normalization des scores (percentiles)

**2.2 Algorithme de Scoring**
```java
public ClassificationScore calculerScore(Integer produitId) {
    // 1. Récupérer métriques brutes depuis la vue
    ProduitMetriquesClassification metriques = metriquesRepo.findById(produitId);

    // 2. Calculer scores normalisés (0-100)
    int scoreCA = calculerScoreCA(metriques.getCa12Mois());
    int scoreRotation = calculerScoreRotation(metriques.getRotationAnnuelle());
    int scoreFrequence = calculerScoreFrequence(metriques.getFrequenceVenteMois());

    // 3. Score pondéré
    ClassificationConfig config = getConfig();
    int scoreTotal = (int) (
        config.getPoidsCA() * scoreCA +
        config.getPoidsRotation() * scoreRotation +
        config.getPoidsFrequence() * scoreFrequence
    );

    // 4. Déterminer classe suggérée
    ClasseCriticite classeSuggeree = determinerClasse(scoreTotal);

    return new ClassificationScore(...);
}
```

**2.3 Normalisation par Percentiles**
```java
private int calculerScoreCA(Long ca) {
    // Utiliser la méthode des percentiles sur l'ensemble des produits
    // Top 5% → 100 points
    // Top 20% → 80 points
    // etc.
}
```

### Phase 3 : Traitement Batch (Semaine 3)

**3.1 Reclassification Batch**
```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public ReclassificationResult reclassifierTousProduits() {
    long totalProduits = produitRepo.countByStatusAndTypeProduitNot(Status.ENABLE, "DETAIL");
    int batchSize = 100;
    int totalPages = (int) Math.ceil((double) totalProduits / batchSize);

    AtomicInteger nbChangements = new AtomicInteger(0);
    AtomicInteger nbAnalyses = new AtomicInteger(0);

    for (int page = 0; page < totalPages; page++) {
        Pageable pageable = PageRequest.of(page, batchSize);
        processBatchClassification(pageable, nbChangements, nbAnalyses);
    }

    return new ReclassificationResult(nbAnalyses.get(), nbChangements.get());
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
protected void processBatchClassification(Pageable pageable,
                                          AtomicInteger nbChangements,
                                          AtomicInteger nbAnalyses) {
    Page<ProduitMetriquesClassification> metriquesPage =
        metriquesRepo.findAll(pageable);

    for (ProduitMetriquesClassification metriques : metriquesPage.getContent()) {
        try {
            ClassificationScore score = calculerScore(metriques.getProduitId());
            nbAnalyses.incrementAndGet();

            if (doitChangerClasse(score)) {
                appliquerChangementClasse(
                    metriques.getProduitId(),
                    score.classeSuggeree(),
                    score,
                    "Reclassification mensuelle automatique"
                );
                nbChangements.incrementAndGet();
            }
        } catch (Exception e) {
            LOG.error("Erreur reclassification produit {}", metriques.getProduitId(), e);
        }
    }
}
```

**3.2 Règles de Changement**
```java
private boolean doitChangerClasse(ClassificationScore score) {
    // Ne pas changer si produit nouveau (< 6 mois)
    if (score.estNouveauProduit()) {
        return false;
    }

    // Ne pas changer si la différence de score est trop faible (hysteresis)
    ClassificationConfig config = getConfig();
    if (Math.abs(score.scoreTotal() - getScorePourClasse(score.classeActuelle()))
        < config.getChangementMinScore()) {
        return false;
    }

    // Changer uniquement si classe différente
    return score.classeSuggeree() != score.classeActuelle();
}
```

### Phase 4 : API et Monitoring (Semaine 4)

**4.1 REST Controller**
```java
@RestController
@RequestMapping("/api/classification")
public class ClassificationResource {

    @GetMapping("/metriques/{produitId}")
    public ResponseEntity<ClassificationScore> getMetriques(@PathVariable Integer produitId);

    @PostMapping("/recalculate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ReclassificationResult> forceReclassification();

    @GetMapping("/logs")
    public ResponseEntity<Page<ClassificationCriticiteLog>> getLogs(Pageable pageable);

    @GetMapping("/config")
    public ResponseEntity<ClassificationConfig> getConfig();

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ClassificationConfig> updateConfig(@RequestBody ClassificationConfig config);
}
```

**4.2 Dashboard Angular**
- Page admin affichant :
  - Distribution des classes (graphique camembert)
  - Historique des changements (tableau)
  - Dernière reclassification (date, nb changements)
  - Configuration des seuils

---

## 📈 Monitoring et Maintenance

### 1. Métriques à Surveiller

```java
@Component
public class ClassificationMetrics {

    @Gauge(name = "classification.repartition.a_plus")
    public long countClasseAPlus() {
        return produitRepo.countByClasseCriticite(ClasseCriticite.A_PLUS);
    }

    @Counter(name = "classification.changements")
    public void incrementChangements() { }

    @Timer(name = "classification.reclassification.duration")
    public void recordReclassificationDuration(Duration duration) { }
}
```

### 2. Alertes

- ⚠️ Alerte si > 20% des produits changent de classe en un mois (anomalie possible)
- ⚠️ Alerte si reclassification échoue (vérifier freeze des mois)
- ⚠️ Alerte si distribution des classes aberrante (ex: 80% en classe A+)

### 3. Rapports Mensuels

Générer rapport PDF automatique après chaque reclassification :
- Nombre de changements par classe
- Top 20 promotions (C→B, B→A, etc.)
- Top 20 rétrogradations
- Distribution avant/après

---

## 🔐 Sécurité et Audit

### 1. Logs Détaillés

Tous les changements de classe sont loggés dans `classification_criticite_log` avec :
- Métriques ayant justifié le changement
- Score calculé
- Type de classification (AUTO, MANUAL)
- User si changement manuel

### 2. Override Manuel

Permettre aux admins de forcer une classe spécifique :
```java
@PostMapping("/api/classification/{produitId}/override")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<Produit> overrideClasse(
    @PathVariable Integer produitId,
    @RequestBody OverrideClasseRequest request
) {
    // Appliquer changement manuel
    // Logger avec classification_type = 'MANUAL'
    // Ajouter flag is_overridden sur Produit pour éviter reclassification auto
}
```

### 3. Restauration

Endpoint pour annuler la dernière reclassification (rollback) :
```java
@PostMapping("/api/classification/rollback")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<RollbackResult> rollbackLastClassification();
```

---

## 📦 Configuration Recommandée

### application.yml
```yaml
pharma-smart:
  classification:
    # Activation/désactivation du scheduler automatique
    auto-classification-enabled: true

    # Taille des batchs pour traitement
    batch-size: 100

    # Seuils par défaut (peuvent être overridés en BDD)
    default-config:
      poids-ca: 0.50
      poids-rotation: 0.30
      poids-frequence: 0.20

      seuil-a-plus: 90
      seuil-a: 80
      seuil-b: 60
      seuil-c: 40

    # Stabilité
    changement-min-score: 10

    # Période d'analyse
    nb-mois-analyse: 12
    nb-mois-min-nouveau-produit: 6
```

---

## 🚀 Déploiement et Migration Initiale

### 1. Migration Initiale

Lors du premier déploiement, classifier tous les produits existants :

```sql
-- Script one-time: Classification initiale basée sur historique existant
-- À exécuter manuellement après migration V1.1.25

-- Option 1: Garder les classes existantes (conservateur)
UPDATE produit p
SET classe_criticite = COALESCE(p.classe_criticite, 'B')
WHERE p.status = 'ENABLE' AND p.type_produit != 'DETAIL';

-- Option 2: Reclassifier tout depuis zéro (recommandé)
-- Exécuter via endpoint admin après déploiement:
POST /api/classification/recalculate?reason=Migration initiale V1.1.25
```

### 2. Vérification Post-Migration

```sql
-- Vérifier distribution des classes
SELECT classe_criticite, COUNT(*) as nb_produits
FROM produit
WHERE status = 'ENABLE' AND type_produit != 'DETAIL'
GROUP BY classe_criticite
ORDER BY classe_criticite;

-- Distribution attendue (approximative) :
-- A+: 5-10% des produits
-- A:  10-15%
-- B:  30-40%
-- C:  25-35%
-- D:  10-20%
```

---

## 📊 Exemple Concret de Calcul

### Produit : Doliprane 1000mg (Boîte de 8)

**Métriques (12 derniers mois)** :
- CA total : 15 000 € (1 500 000 centimes)
- Quantité vendue : 2 400 boîtes
- Fréquence vente : 12 mois (tous les mois)
- Stock moyen : 200 boîtes
- Rotation : 2400 / 200 = 12 rotations/an

**Calcul du Score** :

1. **Score CA** :
   - CA dans le top 3% → Percentile 97 → Score CA = 97

2. **Score Rotation** :
   - 12 rotations/an ≥ seuil A+ (12) → Score Rotation = 100

3. **Score Fréquence** :
   - 12/12 mois avec ventes → Score Fréquence = 100

4. **Score Total** :
   ```
   Score = 0.5 × 97 + 0.3 × 100 + 0.2 × 100
        = 48.5 + 30 + 20
        = 98.5
   ```

5. **Classe Suggérée** :
   - Score 98.5 ≥ 90 → **Classe A+**

**Impact sur SEMOIS** :
- Coefficient de sécurité : 1.5 (au lieu de 1.0 pour classe B)
- Stock objectif augmenté de 50%
- Réappros plus fréquents et quantités plus importantes

---

## ✅ Checklist de Mise en Production

### Avant déploiement :
- [ ] Migration V1.1.25 testée sur environnement de dev
- [ ] Configuration par défaut validée par métier
- [ ] Tests unitaires du service de classification (couverture > 80%)
- [ ] Tests d'intégration du batch processing
- [ ] Documentation API (Swagger) mise à jour
- [ ] Dashboard admin prêt pour monitoring

### Jour J :
- [ ] Déploiement migration BDD
- [ ] Déploiement backend avec nouveau service
- [ ] Exécution classification initiale manuelle
- [ ] Vérification distribution des classes
- [ ] Activation scheduler automatique
- [ ] Test endpoint admin de reclassification manuelle

### Jour J+1 mois :
- [ ] Vérifier première reclassification automatique
- [ ] Analyser rapport mensuel
- [ ] Ajuster seuils si nécessaire
- [ ] Former équipe sur dashboard admin

---

## 🎓 Formation Utilisateurs

### Rôle : Administrateur

**Compétences nécessaires** :
1. Comprendre la grille de classification (A+/A/B/C/D)
2. Interpréter le dashboard de distribution
3. Identifier les anomalies (changements massifs)
4. Ajuster la configuration si besoin (poids, seuils)
5. Forcer une reclassification manuelle
6. Overrider la classe d'un produit spécifique

### Rôle : Pharmacien

**Information** :
- Notification mensuelle des changements de classe
- Explication de l'impact sur les suggestions SEMOIS
- Possibilité de demander un override manuel si désaccord

---

## 📚 Références et Ressources

### Documentation Interne
- [PLAN_IMPLEMENTATION_SEMOIS.md](PLAN_IMPLEMENTATION_SEMOIS.md) - Documentation principale SEMOIS
- [V1.1.22__semois_tables.sql](src/main/resources/db/migration/V1.1.22__semois_tables.sql) - Tables SEMOIS

### Méthodes de Classification
- Méthode ABC classique (Pareto 80/20)
- ABC avec rotation de stock
- Classification multi-critères

### Configuration SEMOIS par Classe
| Classe | Coefficient Sécurité | Délai Livraison | Stock Objectif |
|--------|---------------------|-----------------|----------------|
| A+     | 1.5                | 3 jours         | VMM × 1.65     |
| A      | 1.3                | 5 jours         | VMM × 1.52     |
| B      | 1.0                | 7 jours         | VMM × 1.23     |
| C      | 0.8                | 10 jours        | VMM × 1.07     |
| D      | 0.5                | 14 jours        | VMM × 0.73     |

---

## 🔮 Évolutions Futures

### Version 2.0 (Optionnel)
- **Machine Learning** : Prédiction de la classe future basée sur tendances
- **Saisonnalité avancée** : Ajustement automatique des seuils par saison
- **Catégories spéciales** : Classes spécifiques pour produits frigo, stupéfiants, etc.
- **Simulation** : Tester impact d'un changement de configuration avant application
- **Export Excel** : Rapport détaillé de reclassification pour analyse

---

## 📞 Support et Contact

En cas de question sur la classification automatique :
- **Technique** : Équipe développement
- **Métier** : Responsable pharmacie
- **Configuration** : Administrateur système

---

**Version** : 1.0
**Date** : 2025-12-22
**Auteur** : Équipe Pharma-Smart
**Statut** : Proposition - En attente validation
