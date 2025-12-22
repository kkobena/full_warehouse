# Module Frontend SEMOIS

## Vue d'ensemble

Ce module implémente l'interface utilisateur complète pour le système SEMOIS (Stock Économique Mensuel d'Objectif Interne de Sécurité) de gestion de stock pharmaceutique.

## Composants Créés

### 1. Modèles TypeScript (`app/shared/model/semois/`)

#### `classe-criticite.model.ts`
- Enum `ClasseCriticite` : A_PLUS, A, B, C, D
- Interface `IClasseCriticiteInfo` : Métadonnées de chaque classe
- Constante `CLASSE_CRITICITE_INFO` : Mapping complet avec labels, descriptions, coefficients, couleurs
- Helper `getClasseCriticiteInfo()` : Récupère les infos d'une classe

#### `semois-configuration.model.ts`
- Interface `ISemoisConfiguration` : Configuration SEMOIS par produit
- Classe `SemoisConfiguration` : Implémentation avec valeurs par défaut
- Interfaces de requête : `IInitConfigurationRequest`, `IImportHistoricalRequest`
- Interfaces de réponse : `IAggregationStatus`, `IMessageResponse`, `IInitAllResponse`

#### `semois-suggestion.model.ts`
- Interface `ISemoisSuggestion` : Suggestion de réapprovisionnement
- Classe `SemoisSuggestion` : Implémentation avec méthodes utilitaires
  - `getTauxCouvertureMois()` : Stock actuel / VMM en mois
  - `estEnRupture()` : Stock < Marge sécurité
  - `estEnSurstock()` : Stock > 150% Stock objectif
  - `getEcartStockObjectif()` : Écart en unités
  - `getNiveauUrgence()` : URGENT / NORMAL / OK
  - `getJoursStockRestant()` : Jours avant rupture

### 2. Service Angular (`semois.service.ts`)

Service principal pour communication avec l'API REST backend:

**Méthodes de consultation:**
- `getSuggestions(search?, classeCriticite?)` : Liste suggestions avec filtres
- `getSuggestionForProduct(produitId)` : Suggestion pour un produit
- `getConfiguration(produitId)` : Configuration SEMOIS d'un produit
- `getAggregationStatus()` : Statut de l'agrégation mensuelle

**Méthodes d'administration (ROLE_ADMIN):**
- `initializeConfiguration(request)` : Initialise config pour un produit
- `updateConfiguration(produitId, config)` : Met à jour configuration
- `initializeAllConfigurations()` : Initialisation en masse
- `triggerRecalculation()` : Recalcul manuel
- `importHistoricalData(request)` : Import historique N mois
- `unfreezeMonth(anneeMois, reason)` : Dégel exceptionnel

### 3. Composants UI

#### `semois-suggestions.component` - Composant Principal

**Fonctionnalités:**
- Affichage tableau suggestions avec colonnes: Urgence, Produit, Classe, VMM, Marge Sécurité, Stock Objectif, Stock Actuel, Qté à Commander, Couverture, Jours restants
- Filtres: Classe de criticité, Niveau d'urgence, Recherche texte
- **KPI Cards**: Produits urgents, À commander, OK, Total configurés
- **Help Drawer** complet avec :
  - Explication SEMOIS et formules
  - Classification par criticité (A+ à D)
  - Niveaux d'urgence
  - Utilisation pratique et workflow
  - Exemples concrets
  - Bonnes pratiques

**Signals & Computed:**
```typescript
suggestions = signal<ISemoisSuggestion[]>([]);
urgentCount = computed(() => suggestions.filter(s => s.estEnRupture()).length);
filteredSuggestions = computed(() => /* filtrage dynamique */);
```

**Méthodes d'affichage:**
- `getUrgenceLabel()`, `getUrgenceSeverity()` : Affichage urgence
- `getClasseLabel()`, `getClasseSeverity()` : Affichage classe
- `getCouvertureMois()`, `getJoursRestants()` : Métriques calculées
- `getRowClass()` : Mise en surbrillance lignes (rupture = rouge, à commander = orange)

#### `semois-config-masse.component` - Configuration en Masse

**Stratégies de configuration proposées:**

1. **Classification Automatique ABC** (Onglet 1)
   - Utilise l'analyse ABC de rotation existante
   - Règles de mapping :
     - ABC = A + rotation ≥ 6x/an → Classe A SEMOIS
     - ABC = B ou rotation 3-6x/an → Classe B SEMOIS
     - ABC = C ou rotation 1-3x/an → Classe C SEMOIS
     - Rotation < 1x/an → Classe D SEMOIS
   - Prévisualisation avant application
   - Compteurs par classe (A+, A, B, C, D)
   - Option inclure/exclure produits sans vente
   - Option écraser configurations existantes

2. **Initialisation Manuelle en Masse** (Onglet 2)
   - Initialise tous produits actifs sans config
   - Paramètres par défaut : Classe B, Coeff 0.7, 6 mois, 7 jours délai

3. **Import Historique** (Onglet 3)
   - Import des N derniers mois de ventes (recommandé 12)
   - Affichage avertissements (durée, gel automatique)
   - Configuration nombre de mois

4. **Statut Système** (Onglet 4)
   - Mois en cours : Nb produits agrégés
   - Mois précédent : Nb produits, statut gelé/modifiable
   - Fenêtre de correction (J+7)
   - Horaires schedulers (2h agrégation, 3h recalcul)

### 4. Intégration Formulaire Produit

**Ajout section SEMOIS dans `produit-update.component`:**

- Section conditionnelle (@if produit existant)
- Champs de configuration :
  - Classe de criticité (select)
  - Coefficient de sécurité (number 0-3)
  - Délai livraison (number 1-60 jours)
  - Nb mois historique (number 3-12)
  - Facteur saisonnier (number 0.5-3)
  - Limite péremption (checkbox)
- **Métriques en lecture seule** :
  - VMM calculé
  - Stock Objectif calculé
  - Date dernier calcul
- Bouton "Enregistrer config SEMOIS" indépendant
- Lien vers configuration en masse si pas de config

**Méthodes ajoutées:**
```typescript
loadSemoisConfig(produitId) // Charge config au chargement produit
onSemoisClasseChange() // MAJ coefficient quand classe change
saveSemoisConfig() // Sauvegarde config SEMOIS
```

### 5. Routes (`semois.route.ts`)

```typescript
/semois/suggestions → SemoisSuggestionsComponent (User + Admin)
/semois/config-masse → SemoisConfigMasseComponent (Admin only)
```

## Styles

Utilise les classes communes de l'application:
- `.pharma-smart-content`, `.pharma-toolbar` : Layout standard
- `.form-grid-compact`, `.form-field` : Formulaires
- `.pharma-table`, `.pharma-table-head` : Tables
- `.kpi-card`, `.kpi-value` : KPI cards
- Classes Bootstrap : `.card`, `.alert`, `.badge`, etc.

Styles spécifiques SEMOIS:
- `.table-danger`, `.table-warning` : Mise en surbrillance lignes
- Classes de couleur par classe : `.bg-danger-subtle`, `.bg-success-subtle`, etc.

## Workflow Utilisateur Recommandé

### Déploiement Initial

1. **Import Historique** (Admin)
   - Aller dans `/semois/config-masse` → Onglet "Import Historique"
   - Importer 12 mois de données
   - Attendre fin du traitement (peut prendre quelques minutes)

2. **Classification ABC Automatique** (Admin)
   - Onglet "Classification Auto (ABC)"
   - Cliquer "Prévisualiser la classification"
   - Vérifier la répartition A+, A, B, C, D
   - Cliquer "Appliquer la classification ABC"

3. **Ajustements Manuels** (Admin)
   - Aller dans produits individuels
   - Identifier produits vitaux manuellement
   - Les passer en Classe A+ (insuline, adrénaline, etc.)
   - Activer "Limite péremption" pour sérums, vaccins

### Utilisation Quotidienne

1. **Consultation Suggestions** (User/Admin)
   - Aller dans `/semois/suggestions`
   - Filtrer par urgence "URGENT" → Commander en priorité
   - Filtrer par classe A/A+ → Surveiller quotidiennement
   - Utiliser recherche texte pour produit spécifique

2. **Préparation Commandes**
   - Grouper par fournisseur
   - Exporter liste (fonctionnalité future)
   - Passer commandes selon quantités suggérées

3. **Ajustements Saisonniers**
   - Produits grippe/allergies : Augmenter facteur saisonnier à 1.5
   - Après saison : Ramener à 1.0

## API Endpoints Utilisés

| Endpoint | Méthode | Rôle | Description |
|----------|---------|------|-------------|
| `/api/semois/suggestions` | GET | User | Liste suggestions avec filtres |
| `/api/semois/suggestions/{id}` | GET | User | Suggestion pour un produit |
| `/api/semois/configuration/{id}` | GET | User | Config SEMOIS d'un produit |
| `/api/semois/configuration` | POST | Admin | Initialiser config |
| `/api/semois/configuration/{id}` | PUT | Admin | Mettre à jour config |
| `/api/semois/init-all` | POST | Admin | Initialiser en masse |
| `/api/semois/import-historical` | POST | Admin | Import historique |
| `/api/semois/recalculate` | POST | Admin | Recalcul manuel |
| `/api/semois/aggregation/status` | GET | User | Statut agrégation |
| `/api/semois/aggregation/unfreeze` | POST | Admin | Dégel exceptionnel |

## Dépendances

- **PrimeNG 20+** : `p-button`, `p-select`, `p-table`, `p-toolbar`, `p-tag`, `p-drawer`, `p-tabview`, `p-card`
- **Angular 20** : Signals, Computed, Standalone Components
- **Bootstrap** : Grille, cartes, badges, alertes

## Tests Recommandés

### Tests Unitaires
- Méthodes `SemoisSuggestion` : `getTauxCouvertureMois()`, `estEnRupture()`, etc.
- Computed signals : `urgentCount()`, `filteredSuggestions()`
- Classification automatique : Règles de mapping ABC → SEMOIS

### Tests d'Intégration
- Flux complet : Import historique → Classification ABC → Consultation suggestions
- Sauvegarde config SEMOIS depuis formulaire produit
- Filtres et recherche dans suggestions

### Tests E2E
- Workflow admin : Configuration en masse
- Workflow user : Consultation quotidienne suggestions
- Help Drawer : Navigation et affichage

## Améliorations Futures

1. **Export Excel** : Exporter suggestions vers Excel pour commandes
2. **Historique modifications** : Tracer changements de configuration
3. **Notifications** : Alertes si produits urgents > X
4. **Dashboard KPI** : Graphiques évolution VMM, taux rupture, etc.
5. **Import fichier** : Importer configs depuis Excel
6. **Comparaison ABC vs SEMOIS** : Afficher différences de classification
