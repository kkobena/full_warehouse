# Analyse & Plan d'amélioration — Gestion des Rayons / Emplacements & StorageType

---

## 1. État actuel

### Hiérarchie physique implémentée

```
Magasin
  └─ Storage  (ex: "Stock rayon", "Stock réserve" — types: PRINCIPAL, SAFETY_STOCK)
       └─ Rayon  (ex: "Rayon A", "Zone Froid", "SANS EMPLACEMENT")
            └─ RayonProduit  → Produit
```

### Modèle de données clé

| Entité | Champs | Contraintes |
|--------|--------|-------------|
| `Rayon` | id, code (20), libelle (100), exclude, storage_id | UNIQUE(code, storage_id), UNIQUE(libelle, storage_id) |
| `RayonProduit` | id, produit_id, rayon_id | UNIQUE(produit_id, rayon_id) |
| `Storage` | id, name, storageType (enum), magasin_id | UNIQUE(storageType, magasin_id) |

### Fichiers Angular existants (`entities/rayon/`)

```
entities/rayon/
├── clone-form/
│   ├── clone-form.component.html
│   └── clone-form.component.ts
├── form-rayon/
│   ├── form-rayon.component.html
│   ├── form-rayon.component.ts
│   └── rayon-form.scss
├── rayon.component.html       ← liste principale
├── rayon.component.scss
├── rayon.component.ts
├── rayon.route.ts
└── rayon.service.ts
```

Route actuelle dans `entity.routes.ts` :
```typescript
{ path: 'rayon', data: { abilitySubject: 'rayon' }, loadChildren: () => import('./rayon/rayon.route') }
```

### Flux de stock actuel

```
Réception fournisseur
  → updateTotalStock() → StockProduit PRINCIPAL (toujours)
  → createRayonSuggestionReassort()    (PRINCIPAL bas → suggestion transfert depuis SAFETY_STOCK)
  → createReserveSuggestionReassort()  (SAFETY_STOCK bas → suggestion commande fournisseur)

Vente
  → StockProduit PRINCIPAL décrémenté

SEMOIS (calcul automatique des seuils)
  → if PRINCIPAL    : calcule seuilMini, stockMaxi
  → if SAFETY_STOCK : calcule stockReassort
```

### Ce qui fonctionne

- CRUD Rayon avec pagination, recherche (code + libellé), filtre par Storage/Magasin
- Import CSV (libelle ; code ; exclude)
- Clonage de rayons entre Storages (backend complet, UI masquée)
- Rayon par défaut « SANS EMPLACEMENT » auto-créé pour chaque Storage
- Association Rayon ↔ Produit via `RayonProduit` (création programmatique à l'import produit)
- Cache Hibernate READ_WRITE sur Rayon et RayonProduit

---

## 2. Lacunes identifiées

### 2.1 Lacunes fonctionnelles critiques

**A — Association Rayon ↔ Produit non gérée en UI**
La table `rayon_produit` existe et est alimentée, mais aucune interface pour :
- Affecter un produit à un rayon manuellement
- Consulter quels produits sont dans un rayon donné
- Transférer un produit d'un rayon à un autre
- Identifier les produits sans emplacement affecté

**B — Capacité et occupation non modélisées**
Aucun champ `capacite`, `nbProduits`, `tauxOccupation`. Impossible de savoir si un rayon est plein.

**C — Un seul rayon par produit**
La contrainte `UNIQUE(produit_id, rayon_id)` implique qu'un produit n'a qu'un emplacement par Storage.

**D — Rayon = emplacement logique, pas physique**
Aucune notion de localisation spatiale (allée, étagère, niveau). L'opérateur ne peut pas trouver physiquement un produit.

**E — Bouton Cloner masqué**
`[hidden]="true"` dans le template — fonctionnalité complète côté backend mais inaccessible.

**F — Pas d'export**
Import CSV disponible, export absent. Impossible d'auditer les rayons.

**G — Historique absent**
Aucun audit des affectations produit ↔ rayon.

**H — `StorageType` enum rigide**
Seulement `PRINCIPAL` et `SAFETY_STOCK`. Analyse complète → section 3.

**I — Rayon « SANS EMPLACEMENT » hardcodé**
Code `SANS` constant dans `EntityConstant`. Non configurable.

**J — Pas de tri personnalisé**
Tri alphabétique uniquement. Impossible de définir un ordre de picking logique.

**K — Pas de validation de doublons cross-Storage**
Aucune alerte si un produit est dans des rayons incohérents entre storages.

### 2.2 Fonctionnalités absentes vs. logiciels de référence

Ces fonctionnalités existent dans **Winpharma, Lgpi, Pharmagest, Odoo WMS** et sont absentes ici :

**L — Réassort par emplacement (alerte rupture rayon)**
Quand le stock d'un produit dans un rayon descend sous `stock_min_emplacement`, déclencher automatiquement une suggestion de transfert depuis la réserve pour CE rayon précis. Actuellement la suggestion est globale (par Storage), pas par emplacement.

**M — Étiquettes de rayon imprimables**
En officine, chaque facing de rayon a une étiquette physique : libellé produit, prix de vente, code-barres, référence emplacement. Aucune génération d'étiquette emplacement dans le système.

**N — Scan barcode → localisation produit**
Flasher le code-barres d'un produit pour afficher immédiatement son rayon et son adresse physique. Indispensable pour la gestion quotidienne du linéaire.

**O — Itinéraire de picking optimisé**
Pour une préparation de commande ou un réassort, générer un ordre de passage par rayon (selon `position` et `adresse_physique`) qui minimise les déplacements dans la pharmacie.

**P — Inventaire par rayon**
Compter les produits rayon par rayon (et non seulement par Storage). Aujourd'hui l'inventaire de type STORAGE porte sur tout un Storage. Un inventaire partiel ciblé sur un rayon spécifique n'est pas possible.

**Q — Affectation en masse (import CSV rayon_produit)**
Importer un fichier CSV `produit_cip ; rayon_code` pour affecter des centaines de produits à leurs rayons en une seule opération. Aujourd'hui l'affectation est uniquement programmatique (lors de l'import produit).

**R — QR code emplacement**
Imprimer un QR code pour chaque rayon (code + libellé + adresse). L'opérateur scanne le QR du rayon pour déclencher un inventaire partiel ou afficher la liste des produits attendus.

**S — Statistiques d'occupation et rotation**
- Taux d'occupation par rayon (nb produits / capacite)
- Rayons vides ou sous-utilisés
- Produits jamais vendus dans leur rayon (rotation nulle)
- Valeur de stock par zone / rayon

**T — Historique des mouvements par emplacement**
Traçabilité : qui a déplacé quel produit de quel rayon vers quel autre, et quand. Nécessaire pour les contrôles pharmaceutiques et les audits.

---

## 3. Analyse StorageType — Synthèse complète

### 3.1 Sémantique actuelle et ses limites

Le système traite StorageType comme un **booléen implicite** :
- `PRINCIPAL` = vendable, reçoit les livraisons fournisseur, déclenche les suggestions de réassort
- `SAFETY_STOCK` = réserve non-vendable, alimente le rayon

27 fichiers Java contiennent de la logique dépendant de StorageType, dont des comparaisons directes (`storageType == StorageType.PRINCIPAL`) et des chaînes SQL hardcodées (`'SAFETY_STOCK'`, `'PRINCIPAL'`).

### 3.2 Option retenue — Propriétés sémantiques dans l'enum

```java
public enum StorageType {
    PRINCIPAL   ("Stock rayon",  true,  true),
    SAFETY_STOCK("Réserve",      false, true),
    TOXIQUE     ("Toxiques",     true,  false),
    QUARANTAINE ("Quarantaine",  false, false);

    private final String libelle;
    private final boolean vendable;         // produits dispensables depuis ce storage
    private final boolean suggereReassort;  // participe aux suggestions de réassort automatique

    public boolean isVendable()        { return vendable; }
    public boolean isReassortSuggeré() { return suggereReassort; }
}
```

### 3.3 Justification des valeurs

| Type | vendable | suggereReassort | Justification |
|------|----------|-----------------|---------------|
| `PRINCIPAL` | true | true | Reçoit les livraisons fournisseur (`updateTotalStock` ligne 380), point de vente, déclenche les deux suggestions de réassort après réception |
| `SAFETY_STOCK` | false | true | Réserve non vendue directement, alimente PRINCIPAL, déclencheur commande fournisseur quand vide |
| `TOXIQUE` | true | false | Dispensé sur ordonnance (stupéfiants). Vendable = oui. Réassort automatique = non : commande stupéfiants nécessite bon ANSM, pas une suggestion ordinaire |
| `QUARANTAINE` | false | false | Stock bloqué (retrait de lot, non-conformité). Ni vente, ni réappro — attente de décision |

### 3.4 Cas FRIGO — décision d'architecture

**FRIGO n'est PAS un StorageType.** La réception (`updateTotalStock`) hardcode `storageType == PRINCIPAL` : tous les produits froids ont leur stock dans PRINCIPAL. Faire de FRIGO un StorageType autonome exigerait de modifier le routing de réception — changement majeur sur un flux critique.

```
✅ FROID → Rayon.type_zone = 'FROID'  (badge visuel, filtre, alerte température)
❌ FRIGO → StorageType               (rupture du flux de réception, complexité inutile)
```

### 3.5 Zone vs Rayon — clarification conceptuelle

Une **Zone** répond à : *"Quel contexte réglementaire/physique ?"*
Un **Rayon** répond à : *"Où physiquement dans la pharmacie ?"*

```
Zone OTC        → Rayon "Analgésiques", Rayon "Vitamines"
Zone ORDONNANCE → Rayon "Antibiotiques", Rayon "Cardio"
Zone FROID      → Rayon "Vaccins", Rayon "Insulines"
Zone RÉSERVE    → Rayon "Stock général"
```

Choix retenu : `type_zone` sur Rayon (pas d'entité Zone distincte). Suffisant pour officine, zéro refonte de hiérarchie.

### 3.6 Impact par zone de code

**Ajustements (`AjustementService`) — Impact zéro**
Ciblent le stock par Storage ID, pas par StorageType. Un Storage TOXIQUE ou QUARANTAINE fonctionnerait sans modifier une ligne.

**Inventaire — 4 points de changement obligatoires**

| Point | Fichier | Changement |
|-------|---------|------------|
| 1 | `proc_close_inventory_v2` STEP 2 | `= 'SAFETY_STOCK'` → `IN (:reassortTypes)` |
| 2 | `InventoryClosedEventListener` lignes 74, 97, 189 | Paramétrer avec `reassortTypes` dynamique |
| 3 | `SemoisCalculationService` lignes 825, 843 | `== PRINCIPAL` → `.isVendable()` |
| 4 | `Magasin.java` JoinFormula | Conserver en l'état, accès TOXIQUE via StorageService |

```java
// Construire reassortTypes dynamiquement
List<String> reassortTypes = Arrays.stream(StorageType.values())
    .filter(StorageType::isReassortSuggeré)
    .map(Enum::name)
    .toList();
```

---

## 4. Référence sectorielle

### 4.1 WMS standards (Manhattan, SAP EWM, Generix, Odoo)

```
Entrepôt / Site
  └─ Zone  (Ambiant, Froid, Toxiques, OTC)
       └─ Allée
            └─ Rack / Étagère
                 └─ Niveau
                      └─ Case / Alvéole  (capacité + type produit autorisé)
```

### 4.2 Logiciels officinaux (Winpharma, Lgpi, Pharmagest)

- **Emplacement** = adresse physique Allée / Étagère / Position
- **Planogramme** = affectation produit → emplacement avec quantité idéale (facing)
- **Alerte rupture par emplacement** : rayon vide → réappro depuis réserve
- **Itinéraire de picking** : ordre optimisé pour préparation commande
- **Étiquettes rayon** : impression libellé + prix + code-barres + emplacement
- **Scan barcode** : localisation immédiate d'un produit dans le rayon
- **Inventaire partiel par rayon** : comptage ciblé sans clôturer tout le Storage
- **Statistiques linéaire** : taux d'occupation, rotation, valeur par zone

---

## 5. Architecture cible — Migration vers `features/rayon`

### 5.1 Pourquoi migrer

Le module `entities/rayon/` suit l'ancien pattern (service + composants plats). La codebase migre progressivement vers `features/` avec le pattern `data-access / feature / ui`. La migration est nécessaire avant d'ajouter les nouvelles fonctionnalités (store signal, façade, composants UI enrichis).

### 5.2 Structure cible

```
features/rayon/
├── rayon.routes.ts
├── data-access/
│   ├── services/
│   │   └── rayon-api.service.ts          ← renommage de rayon.service.ts
│   ├── facades/
│   │   └── rayon.facade.ts               ← nouveau : orchestration
│   └── store/
│       └── rayon.store.ts                ← nouveau : signal store
├── feature/
│   ├── rayon-home/
│   │   ├── rayon-home.component.ts       ← renommage de rayon.component.ts
│   │   ├── rayon-home.component.html
│   │   └── rayon-home.component.scss
│   └── rayon-produits/                   ← nouveau : vue produits d'un rayon
│       ├── rayon-produits.component.ts
│       └── rayon-produits.component.html
└── ui/
    ├── form-rayon/                        ← déplacé depuis entities/rayon/form-rayon/
    │   ├── form-rayon.component.ts
    │   ├── form-rayon.component.html
    │   └── rayon-form.scss
    └── clone-form/                        ← déplacé depuis entities/rayon/clone-form/
        ├── clone-form.component.ts
        └── clone-form.component.html
```

### 5.3 Responsabilités par couche

| Couche | Fichier | Responsabilité |
|--------|---------|----------------|
| **service** | `rayon-api.service.ts` | HTTP pur : CRUD, import CSV, export, clone, rayon-produits |
| **store** | `rayon.store.ts` | État réactif : liste rayons, rayon sélectionné, produits du rayon, filtres actifs |
| **facade** | `rayon.facade.ts` | Orchestration : délègue au store et au service, expose les actions au composant |
| **feature** | `rayon-home` | Page liste + filtres (consomme facade) |
| **feature** | `rayon-produits` | Page produits d'un rayon + affectation (consomme facade) |
| **ui** | `form-rayon` | Formulaire création/édition rayon (presentational) |
| **ui** | `clone-form` | Formulaire clonage (presentational) |

### 5.4 Mise à jour `entity.routes.ts`

```typescript
// Avant
{ path: 'rayon', loadChildren: () => import('./rayon/rayon.route') }

// Après
{ path: 'rayon', loadChildren: () => import('../features/rayon/rayon.routes') }
```

Le chemin `/rayon` et les guards (`abilitySubject: 'rayon'`) restent identiques.

---

## 6. Plan d'implémentation détaillé

### Sprint 0 — Migration `features/rayon` (1 semaine)

```
[ ] Créer features/rayon/ avec la structure cible
[ ] rayon-api.service.ts
    - Reprendre rayon.service.ts (HTTP CRUD, import, clone)
    - Ajouter : export CSV, rayon-produits CRUD
[ ] rayon.store.ts (signal store)
    - State : rayons[], selectedRayon, rayonProduits[], filters, loading, pagination
    - Computed : rayonsFiltres, selectedRayonProduits, isEmpty
[ ] rayon.facade.ts
    - load(filters), selectRayon(id), createRayon(), updateRayon(), deleteRayon()
    - cloneRayon(), importCSV(), exportCSV()
    - loadRayonProduits(rayonId), affecterProduit(), retirerProduit()
[ ] rayon-home.component.ts — reprendre rayon.component.ts, consommer facade
[ ] form-rayon + clone-form — déplacer vers ui/, adapter imports
[ ] rayon.routes.ts — lazy load feature + ui components
[ ] entity.routes.ts — pointer vers features/rayon/rayon.routes
[ ] Supprimer entities/rayon/ après vérification
```

### Sprint 1 — Gains rapides (1 semaine)

```
[ ] P1.1 — Retirer [hidden]="true" dans form-rayon (bouton Cloner)
[ ] P1.2 — Endpoint GET /api/rayons/export → CSV (libelle;code;storageId;exclude;type_zone)
[ ] P1.2 — Bouton Export dans rayon-home.component
[ ] P1.3 — rayon-produits.component (route /rayon/:id/produits)
    - Colonnes : CIP, Libellé, Stock actuel, Dernier mouvement, Rayon
    - Bouton "Affecter un produit" → recherche produit + confirmation
    - Bouton "Retirer du rayon"
[ ] P1.4 — Filtre "Sans emplacement" dans /produits
[ ] P1.5 — Affectation en masse : import CSV produit_cip;rayon_code (lacune Q)
    - Endpoint POST /api/rayon-produits/import
    - Bouton import dans rayon-produits.component
```

### Sprint 2 — StorageType sémantique (1.5 semaines)

```
[ ] StorageType.java
    - Ajouter vendable + suggereReassort + isVendable() + isReassortSuggeré()
    - Ajouter TOXIQUE(true, false) et QUARANTAINE(false, false)

[ ] Refactor Java (27 fichiers) — remplacer == PRINCIPAL par isVendable()
    Fichiers clés :
    - SemoisCalculationService.java (lignes 825, 843)
    - SuggestionReassortServiceImpl.java
    - RepartitionStockServiceImpl.java
    - ProduitServiceImpl.java (updateTotalStock ligne 380 : conserver PRINCIPAL pour réception)

[ ] InventoryClosedEventListener.java
    - Extraire reassortTypes dynamique
    - Paramétrer les 3 requêtes SQL (lignes 74, 97, 189)

[ ] Migration Flyway V1.X.X__storagetype_semantique.sql
    - INSERT Storage TOXIQUE par défaut (un par magasin)
    - Mise à jour proc_close_inventory_v2 STEP 2 → IN (:reassortTypes)
```

### Sprint 3 — Enrichissement Rayon (1.5 semaines)

```
[ ] Migration Flyway V1.X.X__rayon_enrichissement.sql
    ALTER TABLE rayon ADD COLUMN position INTEGER DEFAULT 0
    ALTER TABLE rayon ADD COLUMN capacite INTEGER
    ALTER TABLE rayon ADD COLUMN type_zone VARCHAR(30)
      -- AMBIANT, FROID, OTC, ORDONNANCE, TOXIQUE, RESERVE, PARA
    ALTER TABLE rayon_produit ADD COLUMN quantite_ideale INTEGER
    ALTER TABLE rayon_produit ADD COLUMN stock_min_emplacement INTEGER
    ALTER TABLE rayon_produit ADD COLUMN date_affectation TIMESTAMP DEFAULT NOW()

[ ] Backend
    - RayonDTO + RayonProduitDTO : nouveaux champs
    - Endpoints rayon-produits CRUD complets
    - Tri par position dans la liste des rayons
    - Règle : alerte si stock produit < stock_min_emplacement dans ce rayon (lacune L)

[ ] Frontend
    - type_zone dans form-rayon : select + badge coloré
      FROID=bleu, TOXIQUE=rouge, OTC=vert, ORDONNANCE=violet, AMBIANT=gris
    - Filtre type_zone dans rayon-home
    - Formulaire affectation produit → rayon : quantite_ideale, stock_min
    - Barre d'occupation (nb produits / capacite) sur fiche rayon
    - Affichage alerte stock_min_emplacement dans rayon-produits
```

### Sprint 4 — Scan, étiquettes et picking (2 semaines)

```
[ ] Scan barcode → localisation (lacune N)
    - Endpoint GET /api/rayon-produits/by-cip/{cip} → RayonProduit[]
    - Composant "Recherche par scan" dans rayon-home : champ texte + scanner
    - Affichage : rayon + adresse + stock actuel

[ ] Étiquettes de rayon imprimables (lacune M)
    - Endpoint GET /api/rayons/{id}/etiquette → PDF (Flying Saucer + Thymeleaf)
    - Contenu : libellé rayon, code, type_zone, adresse physique, QR code
    - Bouton "Imprimer étiquette" dans rayon-home

[ ] QR code emplacement (lacune R)
    - Générer QR code encodant l'URL /rayon/:id/produits
    - Inclure dans l'étiquette rayon (Barcode4j / ZXing existants)

[ ] Itinéraire de picking (lacune O)
    - Endpoint GET /api/rayons/picking-order?storageId= → Rayon[] triés par position + adresse
    - Vue "Ordre de picking" dans rayon-home : liste ordonnée avec drag-and-drop (optionnel)
```

### Sprint 5 — Adresse physique et inventaire par rayon (1.5 semaines)

```
[ ] Migration Flyway V1.X.X__rayon_adresse.sql
    ALTER TABLE rayon ADD COLUMN adresse_physique VARCHAR(50)
    -- format : "A-03-2" (Allée-Rack-Niveau)

[ ] Backend
    - RayonDTO : adresse_physique
    - Tri par adresse_physique dans la liste
    - Inventaire partiel par rayon (lacune P) :
      Endpoint POST /api/inventaires/rayon/{rayonId} → crée un inventaire RAYON ciblé
      Lignes pré-remplies avec les produits de ce rayon + quantite_ideale

[ ] Frontend
    - Champ "Adresse physique" dans form-rayon
    - Affichage adresse dans rayon-produits et sur étiquettes
    - Bouton "Lancer inventaire" dans rayon-produits.component
```

### Sprint 6 — Statistiques et historique (1 semaine)

```
[ ] Statistiques d'occupation (lacune S)
    - Endpoint GET /api/rayons/stats → { tauxOccupation, valeursStock, rayonsVides, rotationNulle }
    - Page "Tableau de bord rayons" dans rayon-home : cards + graphes Chart.js
      - Taux d'occupation par zone (camembert)
      - Top 10 rayons par valeur de stock
      - Rayons avec produits en rupture emplacement

[ ] Historique des mouvements par emplacement (lacune T)
    - Table rayon_produit_history (produit_id, rayon_from, rayon_to, user, date, motif)
    - Endpoint GET /api/rayon-produits/{id}/history
    - Vue historique dans rayon-produits.component (accordion par produit)
```

### Sprint 7 — Planogramme (optionnel, 2 semaines)

```
[ ] Vue "Planogramme" : grille visuelle des rayons par zone
    - Chaque case = un Rayon, couleur selon taux d'occupation et type_zone
    - Clic → détail rayon + produits
    - Indicateurs : vide / normal / plein / rupture
    - Export planogramme PDF
```

---

## 7. Décisions d'architecture — Récapitulatif

| Décision | Choix | Raison |
|----------|-------|--------|
| Migration `entities/rayon` → `features/rayon` | **Oui (Sprint 0)** | Cohérence avec le pattern de la codebase ; prérequis pour signal store et nouvelles fonctionnalités |
| FRIGO comme StorageType | **Non** | Flux réception hardcodé sur PRINCIPAL ; modélisation via `Rayon.type_zone = 'FROID'` suffit |
| TOXIQUE comme StorageType | **Oui** | Flux réglementaire distinct, traçabilité séparée justifie un Storage dédié |
| QUARANTAINE comme StorageType | **Oui** | Stock bloqué hors flux normal de vente et réassort |
| Propriétés enum vs. switch/case | **Propriétés** | Extensible sans modifier les services existants, remplace 27 points de fragilité |
| Entité Zone distincte | **Non (court terme)** | `type_zone` sur Rayon est suffisant pour officine ; Zone complète = évolution ultérieure |
| Réception TOXIQUE vers Storage TOXIQUE | **Non décidé** | Changement majeur sur flux critique ; reste dans PRINCIPAL en attendant décision métier |
| Inventaire partiel par rayon | **Oui (Sprint 5)** | Besoin opérationnel fort, implémentable sur l'infra inventaire existante |

---

## 8. Récapitulatif des lacunes et sprints associés

| Lacune | Description | Sprint |
|--------|-------------|--------|
| A | Association Rayon ↔ Produit sans UI | S0 + S1 |
| B | Capacité et occupation non modélisées | S3 |
| C | Un seul rayon par produit | S3 (retirer contrainte UNIQUE) |
| D | Pas d'adresse physique | S5 |
| E | Bouton Cloner masqué | S1 |
| F | Pas d'export CSV | S0 + S1 |
| G | Historique absent | S6 |
| H | StorageType rigide | S2 |
| I | SANS EMPLACEMENT hardcodé | S3 |
| J | Pas de tri personnalisé | S3 (position) |
| K | Pas de validation cross-Storage | S3 |
| L | Réassort par emplacement | S3 |
| M | Étiquettes rayon imprimables | S4 |
| N | Scan barcode → localisation | S4 |
| O | Itinéraire de picking | S4 |
| P | Inventaire partiel par rayon | S5 |
| Q | Affectation en masse (import CSV) | S1 |
| R | QR code emplacement | S4 |
| S | Statistiques d'occupation et rotation | S6 |
| T | Historique mouvements par emplacement | S6 |

---

## 9. Recommandation finale

**Hiérarchie cible :**

```
Storage (PRINCIPAL / SAFETY_STOCK / TOXIQUE / QUARANTAINE)
  └─ Rayon (code, libellé, type_zone, adresse physique, capacité, position)
       └─ RayonProduit (produit, quantité idéale, stock min emplacement, date affectation)
```

**Ordre recommandé :**

| Sprint | Thème | Durée | Valeur |
|--------|-------|-------|--------|
| S0 | Migration `features/rayon` | 1 sem | Architecture saine, prérequis |
| S1 | Gains rapides + import masse | 1 sem | Fort — usage quotidien immédiat |
| S2 | StorageType sémantique | 1.5 sem | Fort — désactive 27 points de fragilité |
| S3 | Enrichissement Rayon + type_zone | 1.5 sem | Fort — froid, occupation, affectation |
| S4 | Scan + étiquettes + picking | 2 sem | Moyen/Fort — terrain, préparation commandes |
| S5 | Adresse physique + inventaire rayon | 1.5 sem | Moyen — audit physique précis |
| S6 | Statistiques + historique | 1 sem | Moyen — pilotage et conformité |
| S7 | Planogramme | 2 sem | Optionnel — selon besoin opérationnel |
