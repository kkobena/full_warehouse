# Plan d'implémentation — Module Reports & Reporting
# Analyse consolidée : reports + activity-summary + mvt-caisse + facturation + differes

**Date** : 2026-05-21  
**Périmètre** : `entities/reports`, `entities/raport-gestion`, `entities/mvt-caisse`,
               `features/facturation`, `features/differes`

---

## Règles d'implémentation — à respecter impérativement

### R1 — Utiliser `kpi-strip` pour tous les affichages de KPIs

Le composant de KPI standard du projet est défini dans
`src/main/webapp/app/shared/scss/_kpi-strip.scss`. **Tout nouveau bandeau de KPIs
doit utiliser ces classes**, jamais de markup ad hoc.

```html
<!-- Pattern obligatoire -->
<div class="kpi-strip">
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-chart-line text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">CA Net</span>
      <span class="kpi-strip-value text-primary">{{ ca | number }}</span>
      <span class="kpi-strip-sub">+3.2% vs mois précédent</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item success-accent">...</div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">...</div>
</div>
```

**Modificateurs d'accent disponibles** :
- `primary-accent` → barre bleue `#008cba`
- `success-accent` → barre verte `#43ac6a`
- `danger-accent` → barre rouge `#f04124`
- `warning-accent` → barre orange `#e99002`

**Composants concernés** : tous les bandeaux KPI des nouveaux rapports (situation-creances,
vieillissement-differes, avoirs-analytics, taux-recouvrement-tp, sales-by-staff, seasonality).
Les cards `kpi-card` de `home-base` gardent leur style existant — ne pas modifier.

### R2 — Ne jamais supprimer un menu ou un onglet implicitement

**Toute suppression de menu, d'onglet `ngbNavItem`, ou de route est interdite tant que les
tests fonctionnels de non-régression n'ont pas été validés.**

Processus de suppression :
1. **Étape A** — Désactiver l'onglet : commenter le `ngbNavItem` ou le conditionner avec
   `@if (false)` et ajouter un commentaire `<!-- À SUPPRIMER après validation TNR -->`.
2. **Étape B** — Valider que la fonctionnalité est accessible depuis son nouvel emplacement.
3. **Étape C** — Supprimer le code mort après confirmation explicite.

Cette règle s'applique à toutes les phases du plan, notamment :
- Phase 0.1 : retrait des 7 onglets de `mvt-caisse`
- Phase 1.1 : suppression de la route `stock-rotation`
- Phase 1.2 : suppression de l'onglet `profitability` dans `sales-reports`
- Phase 1.3 : déplacement de `vieillissement-creances` et `concentration-payers`

### R3 — Enum `SaleType` : migration non-cassante uniquement

L'unification des types de vente (Phase 1.5) présente un **risque élevé de régression** :
les valeurs de string actuelles sont envoyées dans les appels API backend. Toute modification
des valeurs peut casser les filtres côté serveur.

Contraintes :
- Les **valeurs de l'enum doivent correspondre exactement aux valeurs acceptées par le backend**.
- Vérifier les valeurs acceptées avant de créer l'enum : inspecter les paramètres
  `GET /api/sales-summary`, `GET /api/pnl-analytique`, `GET /api/comparative-reports`.
- L'enum sert uniquement à **centraliser les labels d'affichage** et les comparaisons
  frontend — ne pas l'utiliser pour construire des paramètres API sans vérification.
- **Ne pas renommer** les variables existantes dans les composants avant que les tests
  fonctionnels valident le comportement des filtres.

---

## 1. Inventaire — données de reporting dispersées sur 5 modules

```
entities/reports/          → 18 composants analytiques (ventes, stock, partners, finance)
entities/raport-gestion/   → activity-summary (arrêté de caisse journalier)
entities/mvt-caisse/       → 9 onglets dont 7 sont des rapports ou documents comptables
features/facturation/      → KPIs TP, récapitulatif mensuel, rapprochement, avoirs
features/differes/         → KPIs crédits clients, historique règlements différés
```

---

## 2. Diagnostic — problèmes identifiés

| # | Problème | Modules | Impact |
|---|---|---|---|
| P1 | `activity-summary` enfermé dans `mvt-caisse` — introuvable pour le titulaire | mvt-caisse | UX bloquant |
| P2 | `mvt-caisse` mélange opérationnel + reporting + comptabilité (9 onglets) | mvt-caisse | Navigation confuse |
| P3 | `ProfitabilityAnalysis` classé sous Ventes alors qu'il mesure des marges | reports/sales | Mauvais rangement |
| P4 | `ABC Pareto` et `Stock Rotation` : 70 % de contenu dupliqué | reports/stock | Doublon |
| P5 | `Vieillissement Créances` et `Concentration Payeurs` : deux onglets TP non unifiés | reports/finance | Navigation fragmentée |
| P6 | 3 nomenclatures différentes pour les types de vente (CashSale / COMPTANT / VNO) | reports | Bug latent |
| P7 | Memory leaks : `subscribe` sans `takeUntilDestroyed` dans les 18+ composants | tous | Stabilité |
| P8 | Filtres date, exports et charts réimplémentés dans chaque composant | tous | Maintenabilité |
| P9 | Aucun tableau de bord synthétique multi-dimensionnel (CA + Stock + TP + Différés) | — | Manque fonctionnel |
| P10 | `IFacturationKpi` (tauxRecouvrement TP, impayées, retards) visible seulement dans le module opérationnel facturation — absent de `reports/finance` | facturation | Donnée cachée |
| P11 | `IDiffereSummary` (encours clients, paidAmount, rest) visible seulement dans le module differes — absent de `reports` | differes | Donnée cachée |
| P12 | Aucune vue unifiée "Situation des créances" combinant TP + clients différés | facturation + differes | Donnée éclatée |
| P13 | Analytics avoirs (montant par statut, impact CA, trend) absents de `reports` | facturation | Manque fonctionnel |
| P14 | Section "Crédits accordés" de `activity-summary` = crédits TP, mais nommée de façon ambiguë — confusion avec les différés clients | activity-summary | UX confus |
| P15 | Aucune analyse de vieillissement pour les différés clients (âge de chaque vente non payée) | differes | Manque fonctionnel |

---

## 3. Distinction fondamentale : deux types de créances

L'application gère **deux flux de créances totalement distincts** qui doivent rester séparés
dans les rapports mais être agrégés dans un tableau de bord unique :

| Type | Module source | Débiteur | Suivi via | Montants typiques |
|---|---|---|---|---|
| **Créances tiers-payants** | `features/facturation` | Mutuelles, CNAM, assurances | IFacture (statut NOT_PAID/PARTIALLY_PAID) | Gros montants, règlements en lot |
| **Différés clients** | `features/differes` | Patients individuels | IDiffere (rest > 0) | Petits montants, règlement au guichet |

Ces deux sources alimentent :
- `reports/finance/vieillissement-creances` (TP seulement aujourd'hui)
- `activity-summary` section "Crédits accordés" (TP seulement — nom trompeur)
- `activity-summary` section "Règlements tiers-payants" (règlements TP reçus)

Les différés clients ne sont **jamais** agrégés dans un rapport de synthèse global.

---

## 4. Découverte clé : le tableau de bord exécutif existe déjà

`home/home-base/home-base.component.html` **est** le tableau de bord exécutif décrit dans
les analyses précédentes. Il est plus complet que ce qui était planifié.

### Ce que `home-base` couvre déjà

| Dimension | Contenu |
|---|---|
| **Alertes stock** | 5 badges cliquables : Péremptions, Ruptures, À commander, Ajustements, Modif. Prix |
| **Sélecteur de période** | Presets pills (Aujourd'hui, Semaine, Mois…) |
| **CA Net** | Valeur + % évolution N-1 + nb ventes + remise |
| **Marge brute** | Montant + taux % + nb produits faible marge + coût achat |
| **Ventes comptant** | CA + badge TP + nb annulées |
| **Créances TP** | Total encours + créances >90j + nb assureurs |
| **Achats fournisseurs** | TTC + HT + nb commandes + TVA |
| **Stock valorisé** | Valeur PA + valeur PV + marge potentielle % + nb réf. |
| **Modes de règlement** | Liste ou pie chart |
| **Top tiers-payants** | Classement or/argent/bronze + montant, ou doughnut |
| **Top fournisseurs** | Classement + score performance + délai livraison |
| **Top produits valeur** | Tableau rangé + total, ou bar chart |
| **Top produits quantité** | Tableau rangé + total, ou bar chart |
| **Pareto 20/80** | Onglets Quantité / Montant — tableau ou bar chart |
| **Toggle vue** | Segment control Tabulaire ↔ Graphique |
| **Liens vers reports** | "CA avancé" → /reports/sales, "Marges & Résultat" → /reports/finance |
| **Skeleton loading** | Pendant le chargement des données |
| **Horodatage** | Dernière mise à jour affichée |

### Ce qu'il manque dans `home-base` (à ajouter — Phase 2)

| Manque | Source disponible |
|---|---|
| Encours différés **clients** (IDiffereSummary.rest) | `GET /api/differes/summary` |
| Taux de recouvrement TP + nb factures en retard | `GET /api/edition-factures/kpi` |
| Nb avoirs en attente (statut DRAFT/EMIS) | `GET /api/avoirs` |

### Rôles distincts : home-base ≠ activity-summary

| Composant | Nature | Question répondue |
|---|---|---|
| `home-base` | Dashboard de pilotage live | Comment se porte l'officine en ce moment ? |
| `activity-summary` | Arrêté comptable de période | Qu'est-ce qui s'est passé sur cette période ? |

Ils sont complémentaires. `activity-summary` va dans `comptabilite`, **pas dans reports**.

---

## 5. Structure cible

```
home/home-base/                        ← TABLEAU DE BORD PRINCIPAL (existe, enrichir)

entities/
├── mvt-caisse/                        ← ALLÉGÉ : 2 onglets opérationnels uniquement
│   ├── visualisation-mvt-caisse
│   └── gestion-caisse
│
├── comptabilite/                      ← NOUVEAU MODULE
│   ├── balance-mvt-caisse             (arrêté de caisse — document comptable)
│   ├── taxe-report                    (rapport TVA)
│   ├── tableau-pharmacien             (tableau de clôture journalier)
│   ├── recap-caisse                   (récapitulatif de caisse)
│   ├── activity-summary               (arrêté comptable de période ← depuis mvt-caisse)
│   ├── declaration-tva                (déclaration fiscale)
│   └── export-comptable               (export vers logiciel compta)
│
├── features/facturation/              ← INCHANGÉ (module opérationnel)
├── features/differes/                 ← INCHANGÉ (module opérationnel)
│
└── reports/                           ← ENRICHI (pas de nouveau pilier dashboard)
    ├── sales/                         ← RATIONALISÉ
    │   ├── dashboard-ca
    │   ├── sales-summary
    │   ├── top-products
    │   ├── comparative-analysis
    │   ├── sales-forecast
    │   ├── market-basket
    │   ├── sales-by-staff             ← NOUVEAU
    │   └── seasonality                ← NOUVEAU
    ├── stock/                         ← RATIONALISÉ
    │   ├── stock-alerts
    │   ├── stock-valuation
    │   ├── recap-produit-vendu
    │   └── stock-abc                  ← FUSION abc-pareto + stock-rotation
    ├── partners/                      ← ENRICHI
    │   ├── customer-segmentation
    │   ├── supplier-performance
    │   └── client-retention           ← NOUVEAU
    └── finance/                       ← ENRICHI
        ├── pnl/
        │   ├── pnl-analytique         (+ onglet profitabilité ← depuis sales)
        │   └── remises-analysis       ← NOUVEAU
        ├── creances/                  ← NOUVEAU BLOC UNIFIÉ
        │   ├── situation-creances     ← NOUVEAU (TP + différés fusionnés)
        │   ├── vieillissement-creances
        │   ├── vieillissement-differes ← NOUVEAU
        │   ├── avoirs-analytics       ← NOUVEAU
        │   └── concentration-payers
        └── tresorerie/
            ├── cash-flow-bfr
            └── taux-recouvrement-tp   ← NOUVEAU
```

---

## 5. Cartographie des données disponibles par module

### 5.1 Données de `features/facturation` exploitables en reporting

| Donnée | Endpoint | Utilisation recommandée |
|---|---|---|
| `IFacturationKpi` (totalFacture, totalRegle, totalRestant, tauxRecouvrement, countImpayees, countEnRetard) | `GET /api/edition-factures/kpi` | Dashboard exécutif + rapport taux recouvrement TP |
| `IRecapitulatifMensuelDto` (soldePrecedent, totalFacture, totalRegle, soldeActuel, soldeCumule) | `GET /api/edition-factures/recapitulatif` | Situation créances TP par organisme |
| `IEtatRapprochement` (ecartTotal, lignes) | `GET /api/rapprochement` | Rapport avoirs & rejets TP |
| `IAvoir` (montantAvoir, statut DRAFT/EMIS/IMPUTE/ANNULE) | `GET /api/avoirs` | Analytics avoirs |
| `IPlanification` (derniereExecution, dernierStatut, historique) | `GET /api/planifications-facturation/{id}/historique` | Monitoring automatisation |

### 5.2 Données de `features/differes` exploitables en reporting

| Donnée | Endpoint | Utilisation recommandée |
|---|---|---|
| `IDiffereSummary` (saleAmount, paidAmount, rest) | `GET /api/differes/summary` | Dashboard exécutif + situation créances |
| `IDiffereItem` (amount, paidAmount, restAmount, mvtDate) | `GET /api/differes` | Vieillissement différés (âge = today - mvtDate) |
| `IReglementDiffereSummary` (paidAmount, solde) | `GET /api/differes/reglements/summary` | Flux de recouvrement sur période |
| `IReglementDiffereItem` (libelleMode, montantVerse, mvtDate, user) | `GET /api/differes/reglements` | Modes paiement + ventes par collaborateur |

---

## Phase 0 — Restructuration architecturale
**Objectif** : Déplacer les composants existants sans toucher au code métier.  
**Effort** : 1 jour | **Risque** : faible (routes + imports uniquement)

### 0.1 — Alléger `mvt-caisse` (7 onglets à désactiver → 2)

**Fichier** : `entities/mvt-caisse/mvt-caisse.component.html`

**Règle R2 — Désactiver avant de supprimer.** Ne pas retirer le code immédiatement.
Envelopper chaque onglet dans un commentaire de désactivation le temps de valider les TNR :

```html
<!-- À SUPPRIMER après validation TNR — migré vers comptabilite -->
@if (false) {
  <ng-container ngbNavItem="balance">...</ng-container>
}
```

| Onglet à désactiver | Composant | Destination finale |
|---|---|---|
| `balance` | `jhi-balance-mvt-caisse` | → `comptabilite` |
| `taxe-report` | `jhi-taxe-report` | → `comptabilite` |
| `tableau-pharmacien` | `jhi-tableau-pharmacien` | → `comptabilite` |
| `recapitulatif-caisse` | `jhi-recapitualtif-caisse` | → `comptabilite` |
| `declaration-tva` | `app-declaration-tva` | → `comptabilite` |
| `export-comptable` | `app-export-comptable` | → `comptabilite` |
| `raport-activite` | `jhi-activity-summary` | → `comptabilite` |

Après validation TNR, supprimer les blocs `@if (false)`.  
`mvt-caisse` conserve uniquement : **Mouvements de caisse** + **Gestion de caisse**.

### 0.2 — Créer le module `comptabilite`

Nouveau dossier `entities/comptabilite/` avec sidebar verticale identique à `mvt-caisse`.

| Onglet | Composant | Nature |
|---|---|---|
| Balance de caisse | `jhi-balance-mvt-caisse` | Arrêté comptable quotidien |
| Rapport TVA | `jhi-taxe-report` | Synthèse taxes collectées |
| Tableau pharmacien | `jhi-tableau-pharmacien` | Document de clôture journalier |
| Récapitulatif caisse | `jhi-recapitualtif-caisse` | Synthèse comptable |
| Déclaration TVA | `app-declaration-tva` | Déclaration fiscale périodique |
| Export comptable | `app-export-comptable` | Intégration logiciel tiers |

Ajouter la route `/comptabilite` dans `app.routes.ts`.

### 0.3 — Intégrer `activity-summary` dans `comptabilite`

`activity-summary` est un **arrêté comptable de période**, pas un dashboard. Il va dans
`comptabilite` aux côtés de la balance et du tableau pharmacien.

Ajouter un onglet **"Rapport d'activité"** dans `comptabilite.component.html` :
```html
<ng-container ngbNavItem="rapport-activite">
  <a ngbNavLink><i class="pi pi-chart-line"></i> Rapport d'activité</a>
  <ng-template ngbNavContent>
    <jhi-activity-summary></jhi-activity-summary>
  </ng-template>
</ng-container>
```

**Note** : ne pas créer de pilier `reports/dashboard` — `home-base` remplit déjà ce rôle.

---

## Phase 1 — Consolidation des rapports existants
**Objectif** : Éliminer les doublons fonctionnels sans régression.  
**Effort** : 3 jours | **Risque** : faible

### 1.1 — Fusionner `ABC Pareto` + `Stock Rotation` → `StockABCComponent`

**Base** : `entities/reports/stock/abc-pareto/` (renommer en `stock-abc/`)  
**Intégration** : contenu de `stock-rotation/` comme 2e onglet "Par rotation"

Étapes :
1. Créer `stock-abc/stock-abc.component.*` (nouveau fichier — **ne pas modifier abc-pareto ni stock-rotation**)
2. Ajouter tabs ng-bootstrap : **Par CA (Pareto)** / **Par Rotation**
3. Copier le contenu HTML de `abc-pareto.component.html` dans le 1er onglet
4. Copier le contenu HTML de `stock-rotation.component.html` dans le 2e onglet
5. Injecter `ABCParetoReportService` et `StockRotationReportService`
6. Créer `shared/model/enums/stock-abc.enum.ts` : `ClasseParetoCA` (A+/A/B/C/D) et `ClasseRotationStock` (A/B/C)
7. Dans `stock-reports.component.html` : ajouter l'onglet `app-stock-abc` et désactiver les 2 anciens avec `@if (false)` + commentaire `<!-- À SUPPRIMER après TNR -->`
8. **Après validation TNR** : supprimer les dossiers `abc-pareto/`, `stock-rotation/` et les blocs désactivés

**KPI banner** du composant fusionné : utiliser `kpi-strip` :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-chart-pie text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">CA Total</span>
      <span class="kpi-strip-value text-primary">{{ summary?.caTotal | number }}</span>
      <span class="kpi-strip-sub">{{ summary?.nbProduits }} produits</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">
    <i class="pi pi-star text-danger"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Classe A+</span>
      <span class="kpi-strip-value text-danger">{{ summary?.nbClasseAPlus }}</span>
      <span class="kpi-strip-sub">{{ summary?.concentrationAPlus | number:'1.0-0' }}% du CA</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item warning-accent">
    <i class="pi pi-sync text-warning"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Rotation lente</span>
      <span class="kpi-strip-value text-warning">{{ summary?.nbRotationLente }}</span>
      <span class="kpi-strip-sub">stock immobilisé</span>
    </div>
  </div>
</div>
```

### 1.2 — Déplacer `ProfitabilityAnalysis` → Finance / P&L (3e onglet)

**Destination** : `entities/reports/finance/pnl-analytique/pnl-analytique.component.html`

Étapes :
1. Dans `pnl-analytique.component.html`, ajouter un 3e onglet **"Profitabilité détaillée"**
2. Importer `ProfitabilityAnalysisComponent` en standalone
3. Dans `sales-reports.component.html` : désactiver l'onglet `profitability` avec `@if (false)` + commentaire `<!-- À SUPPRIMER après TNR — migré vers Finance/P&L -->`
4. **Après validation TNR** : supprimer l'onglet désactivé et mettre à jour la permission : `rapport-finance.profitability`

### 1.3 — Créer le bloc `Créances` dans Finance

Nouveau sous-dossier `entities/reports/finance/creances/` avec composant conteneur
`creances.component.html` hébergeant les onglets :
- Situation des créances ← NOUVEAU (Phase 4)
- Vieillissement créances TP ← existant (déplacé depuis finance-reports)
- Vieillissement différés ← NOUVEAU (Phase 4)
- Concentration payeurs ← existant (déplacé depuis finance-reports)
- Avoirs ← NOUVEAU (Phase 4)

Dans `finance-reports.component.html` : désactiver les onglets `vieillissement-creances` et
`concentration-payers` avec `@if (false)` + commentaire `<!-- À SUPPRIMER après TNR -->`.  
**Après validation TNR** : supprimer les onglets désactivés.

### 1.4 — Renommer la section "Crédits accordés" de `activity-summary`

**Fichier** : `entities/raport-gestion/activity-summary/activity-summary.component.html`

Changer le titre de la card `<h5>` de "Crédits accordés" → **"Crédits tiers-payants"** et
ajouter un lien `routerLink="/differes"` dans le footer de la card pour accéder à l'encours
patients.

### 1.5 — Unifier l'enum `SaleType` — RISQUE RÉGRESSION ⚠️

> **Règle R3 — Migration non-cassante.** Les valeurs de l'enum doivent correspondre **exactement**
> aux chaînes acceptées par le backend. Vérifier avant toute modification les paramètres attendus
> par : `GET /api/sales-summary` (param `typeVente`), `GET /api/pnl-analytique/snapshot/segment`
> (param `segment`), `GET /api/comparative-reports/by-sales-type`.

**Démarche sécurisée** :

1. **Audit préalable** : inspecter les appels HTTP existants dans `sales-summary.component.ts`,
   `pnl-analytique.component.ts`, `comparative-analysis.component.ts` et noter les valeurs
   de string exactes envoyées au backend.

2. **Créer l'enum en miroir** des valeurs constatées (ne pas inventer de nouvelles valeurs) :

```typescript
// shared/model/enums/sale-type.enum.ts
// ⚠️ Les valeurs doivent correspondre exactement aux valeurs backend constatées à l'audit
export enum SaleType {
  CASH       = 'CASH',        // à confirmer depuis audit
  INSURANCE  = 'INSURANCE',   // à confirmer
  DEPOT      = 'DEPOT',       // à confirmer
  CREDIT     = 'CREDIT',      // à confirmer
}

// Labels affichage uniquement — ne pas utiliser pour construire des params API
export const SALE_TYPE_LABEL: Record<SaleType, string> = {
  [SaleType.CASH]:      'Comptant',
  [SaleType.INSURANCE]: 'Tiers-payant',
  [SaleType.DEPOT]:     'Dépôt',
  [SaleType.CREDIT]:    'Crédit / Différé',
};
```

3. **Remplacer progressivement**, un composant à la fois, en validant les filtres après chaque remplacement.

4. **Ne pas toucher** aux valeurs envoyées dans les paramètres `HttpParams` — seulement aux
   comparaisons dans les templates (`type === 'CashSale'` → `type === SaleType.CASH`) et aux
   labels d'affichage.

---

## Phase 2 — Enrichissement de `home-base` (tableau de bord existant)
**Objectif** : Ajouter les 3 dimensions manquantes dans le dashboard déjà très complet.  
**Effort** : 2 jours | **Risque** : faible (endpoints déjà implémentés côté backend)

**Fichier** : `home/home-base/home-base.component.html` + `.ts`

### 2.1 — Ajouter la card "Différés clients"

Insérer une **7e card KPI** dans la grille (après "Stock valorisé") :

```typescript
// Dans home-base.component.ts — injecter DiffereApiService
private readonly differeApi = inject(DiffereApiService);
protected differeSummary = signal<IDiffereSummary | null>(null);

// Dans loadDashboard() :
this.differeApi.getDiffereSummary({ paymentStatuses: ['IMPAYE'] })
  .pipe(takeUntilDestroyed(this.destroyRef))
  .subscribe(res => this.differeSummary.set(res.body));
```

```html
<!-- Card 7 : Différés clients -->
<div class="col-xl-2 col-md-6 mb-2">
  <div class="card h-100 kpi-card danger-accent">
    <div class="card-body">
      <div class="kpi-header">
        <div class="kpi-content">
          <p class="kpi-label">Différés clients</p>
          <h4 class="kpi-value">{{ differeSummary()?.rest | number }}</h4>
          <div class="kpi-badges mt-1">
            <span class="badge bg-warning-subtle text-warning">
              {{ differeSummary()?.saleAmount | number }} engagé
            </span>
          </div>
        </div>
        <div class="kpi-icon text-danger"><i class="pi pi-users"></i></div>
      </div>
    </div>
    <div class="card-footer">
      <a routerLink="/differes" class="footer-label text-decoration-none">Voir le détail →</a>
    </div>
  </div>
</div>
```

### 2.2 — Enrichir la card "Créances TP" avec les KPIs de facturation

La card existante affiche `totalCreances` et `creancesPlusDe90j`. La compléter avec
les données de `IFacturationKpi` :

```typescript
// Injecter FactureApiService
private readonly factureApi = inject(FactureApiService);
protected facturationKpi = signal<IFacturationKpi | null>(null);

// Dans loadDashboard() :
this.factureApi.getKpi({ fromDate: this.fromDate, toDate: this.toDate })
  .subscribe(res => this.facturationKpi.set(res.body));
```

Ajouter dans le footer de la card Créances TP :
```html
<div class="card-footer d-flex justify-content-between">
  <span class="badge bg-success-subtle text-success">
    {{ facturationKpi()?.tauxRecouvrement | number:'1.0-0' }}% recouvré
  </span>
  <span class="badge bg-danger-subtle text-danger"
        title="Factures en retard de paiement">
    {{ facturationKpi()?.countEnRetard }} retard
  </span>
</div>
```

### 2.3 — Ajouter un lien vers Facturation et Différés dans la zone "liens rapides"

Dans la zone droite header (à côté des liens "CA avancé" / "Marges & Résultat") :
```html
<a routerLink="/facturation" class="periode-pill" title="Facturation tiers-payants">
  <i class="pi pi-file-invoice"></i>
  <span class="pill-label">Facturation</span>
</a>
<a routerLink="/differes" class="periode-pill" title="Ventes à crédit clients">
  <i class="pi pi-users"></i>
  <span class="pill-label">Différés</span>
</a>
```

---

## Phase 3 — Infrastructure technique partagée
**Objectif** : Éliminer le code dupliqué dans les 20+ composants.  
**Effort** : 3 jours | **Risque** : faible

### 3.1 — `DateRangeFilterComponent` partagé

**Fichier** : `shared/components/date-range-filter/date-range-filter.component.ts`

```typescript
@Component({ selector: 'app-date-range-filter' })
// Input  : presets: string[], defaultPreset: string
// Output : periodChange: EventEmitter<{ from: string; to: string }>
```

Remplacer les filtres dates dans les 18 composants de `reports` + `activity-summary` + composants
de `facturation` et `differes` qui réimplémentent le même sélecteur.

### 3.2 — `ReportExportService` partagé

**Fichier** : `shared/services/report-export.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class ReportExportService {
  export(type: 'pdf'|'excel'|'csv', url: string, params: object, filename: string): Observable<void>
}
// Centralise : handleBlobForTauri() vs URL.createObjectURL()
```

Consolide la logique dupliquée dans 12 composants de `reports`, `facturation` et `differes`.

### 3.3 — `ChartBuilderService` partagé

**Fichier** : `shared/services/chart-builder.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class ChartBuilderService {
  createLineChart(canvas, data, options?): Chart
  createBarChart(canvas, data, options?): Chart
  createHorizontalBarChart(canvas, data): Chart
  createPieChart(canvas, data): Chart
  destroyChart(chart: Chart | null): void
}
```

Remplace le cycle destroy→create réimplémenté dans 8 composants.

### 3.4 — Fix memory leaks `takeUntilDestroyed`

```typescript
// Pattern à appliquer partout :
this.service.getData()
  .pipe(takeUntilDestroyed(this.destroyRef))
  .subscribe(data => { ... });
```

Priorité : DashboardCA, RecapProduitVendu, ComparativeAnalysis, FacturationHomeComponent,
DiffereDetailPanelComponent.

---

## Phase 4 — Nouveaux rapports : Créances & Finance
**Objectif** : Combler les manques identifiés dans facturation et differes.  
**Effort** : 6 jours | **Risque** : moyen (nouveaux endpoints backend)

### 4.1 — Situation unifiée des créances ⭐⭐⭐

**Composant** : `reports/finance/creances/situation-creances/`  
**Placement** : 1er onglet du bloc Créances (Phase 1.3)

**KPI strip en haut de page** (utiliser `kpi-strip`) :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item danger-accent">
    <i class="pi pi-credit-card text-danger"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Encours TP</span>
      <span class="kpi-strip-value text-danger">{{ kpi.totalRestantTP | number }}</span>
      <span class="kpi-strip-sub">{{ kpi.tauxRecouvrement | number:'1.0-0' }}% recouvré</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item warning-accent">
    <i class="pi pi-users text-warning"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Différés clients</span>
      <span class="kpi-strip-value text-warning">{{ kpi.restDifferes | number }}</span>
      <span class="kpi-strip-sub">{{ kpi.nbClientsDifferes }} clients</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-chart-line text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Total encours</span>
      <span class="kpi-strip-value text-primary">{{ kpi.totalEncours | number }}</span>
      <span class="kpi-strip-sub">TP + différés</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">
    <i class="pi pi-clock text-danger"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Différés > 60j</span>
      <span class="kpi-strip-value text-danger">{{ kpi.nbDifferesAnciens }}</span>
      <span class="kpi-strip-sub">clients à risque</span>
    </div>
  </div>
</div>
```

**Deux tableaux côte à côte :**
- **Créances TP** (depuis `GET /api/edition-factures/recapitulatif`) : Organisme, Solde précédent, Facturé, Réglé, Solde actuel
- **Différés clients** (depuis `GET /api/differes`) : Client, Total différé, Payé, Reste, Âge max (jours)

**Endpoints** : réutiliser ceux déjà implémentés dans facturation et differes.

### 4.2 — Vieillissement des différés clients ⭐⭐⭐

**Composant** : `reports/finance/creances/vieillissement-differes/`  
**Placement** : onglet dans le bloc Créances (Phase 1.3)

**KPI strip** :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item success-accent">
    <i class="pi pi-check-circle text-success"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">0 – 30 jours</span>
      <span class="kpi-strip-value text-success">{{ tranches.t30 | number }}</span>
      <span class="kpi-strip-sub">{{ tranches.pct30 | number:'1.0-0' }}%</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item warning-accent">...</div>  <!-- 31-60j -->
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">...</div>   <!-- 61-90j -->
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">...</div>   <!-- > 90j -->
</div>
```

Tranches calculées depuis `IDiffereItem.mvtDate` (today - mvtDate).  
**Backend** : `GET /api/differes/vieillissement` (nouveau endpoint à créer)  
**Chart** : stacked bar chart par tranche — même visuel que `vieillissement-creances`.

### 4.3 — Analytics avoirs ⭐⭐

**Composant** : `reports/finance/creances/avoirs-analytics/`  
**Données** : `GET /api/avoirs` (déjà implémenté dans facturation)

**KPI strip** :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item success-accent">
    <i class="pi pi-file-check text-success"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Avoirs émis</span>
      <span class="kpi-strip-value text-success">{{ avoirs.montantEmis | number }}</span>
      <span class="kpi-strip-sub">{{ avoirs.nbEmis }} avoirs</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item warning-accent">
    <i class="pi pi-clock text-warning"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">En attente</span>
      <span class="kpi-strip-value text-warning">{{ avoirs.montantDraft | number }}</span>
      <span class="kpi-strip-sub">{{ avoirs.nbDraft }} brouillons</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-percentage text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Taux avoir / CA</span>
      <span class="kpi-strip-value text-primary">{{ avoirs.tauxAvoir | number:'1.1-1' }}%</span>
      <span class="kpi-strip-sub">impact marge</span>
    </div>
  </div>
</div>
```

**Tableau** : Organisme, Nb avoirs, Montant, Statut dominant, Évolution vs mois précédent  
**Chart** : évolution mensuelle des avoirs émis sur 12 mois (impact sur CA)

### 4.4 — Taux de recouvrement TP par organisme ⭐⭐

**Composant** : `reports/finance/tresorerie/taux-recouvrement-tp/`  
**Données** : `GET /api/edition-factures/kpi?organismeId=X` pour chaque TP

**KPI strip** :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item success-accent">
    <i class="pi pi-check text-success"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Taux moyen</span>
      <span class="kpi-strip-value text-success">{{ global.tauxMoyen | number:'1.0-0' }}%</span>
      <span class="kpi-strip-sub">tous organismes</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">
    <i class="pi pi-exclamation-triangle text-danger"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Factures en retard</span>
      <span class="kpi-strip-value text-danger">{{ global.countEnRetard }}</span>
      <span class="kpi-strip-sub">{{ global.montantEnRetard | number }} FCFA</span>
    </div>
  </div>
</div>
```

**Tableau** : Organisme, CA Facturé, CA Réglé, Taux (%), Nb Impayées, Nb En retard, DSO moyen  
**Chart** : évolution du taux de recouvrement par organisme sur 12 mois (line chart multi-séries)

---

## Phase 5 — Nouveaux rapports : Ventes & Stock
**Objectif** : Combler les manques identifiés dans l'analyse officine.  
**Effort** : 8 jours | **Risque** : moyen (développement full-stack)

### 5.1 — Ventes par collaborateur ⭐⭐⭐

**Composant** : `reports/sales/sales-by-staff/`  
**Données backend** : `GET /api/reports/sales-by-staff?from=&to=`  
Peut s'enrichir depuis `IReglementDiffereItem.user` pour les encaissements différés.

**KPI strip** :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-users text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">CA moyen / collaborateur</span>
      <span class="kpi-strip-value text-primary">{{ stats.caMoyen | number }}</span>
      <span class="kpi-strip-sub">{{ stats.nbCollaborateurs }} actifs</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item success-accent">
    <i class="pi pi-trophy text-success"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Meilleur CA</span>
      <span class="kpi-strip-value text-success">{{ stats.topCA | number }}</span>
      <span class="kpi-strip-sub">{{ stats.topNom }}</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item warning-accent">
    <i class="pi pi-percentage text-warning"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Taux remise moy.</span>
      <span class="kpi-strip-value text-warning">{{ stats.tauxRemiseMoyen | number:'1.1-1' }}%</span>
      <span class="kpi-strip-sub">tous collaborateurs</span>
    </div>
  </div>
</div>
```

**Tableau** : Collaborateur, Nb transactions, CA (FCFA), Panier moyen, Nb produits distincts, Taux remise accordé  
**Chart** : bar chart CA par collaborateur

### 5.2 — Saisonnalité des ventes ⭐⭐⭐

**Composant** : `reports/sales/seasonality/`  
**Données backend** : `GET /api/reports/seasonality?familleId=&years=3`

**KPI strip** :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-calendar text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Mois fort N</span>
      <span class="kpi-strip-value text-primary">{{ stats.moisFort }}</span>
      <span class="kpi-strip-sub">{{ stats.caMoisFort | number }} FCFA</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item success-accent">
    <i class="pi pi-arrow-up text-success"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Croissance N vs N-1</span>
      <span class="kpi-strip-value text-success">{{ stats.croissance | number:'1.1-1' }}%</span>
      <span class="kpi-strip-sub">sur la même période</span>
    </div>
  </div>
</div>
```

**Chart** : 3 courbes superposées (N, N-1, N-2) par mois  
**Filtres** : Famille de produits (grippe, allergie, solaire, etc.), Nb années

### 5.3 — Taux de substitution génériques ⭐⭐

**Composant** : `reports/sales/generics-substitution/`  
**Données backend** : `GET /api/reports/generics-substitution?from=&to=`

**KPI strip** :
```html
<div class="kpi-strip">
  <div class="kpi-strip-item primary-accent">
    <i class="pi pi-percentage text-primary"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Taux substitution</span>
      <span class="kpi-strip-value text-primary">{{ stats.tauxGlobal | number:'1.1-1' }}%</span>
      <span class="kpi-strip-sub">objectif {{ stats.objectifCnam }}%</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item success-accent">
    <i class="pi pi-check-circle text-success"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Substituées</span>
      <span class="kpi-strip-value text-success">{{ stats.nbSubstituees }}</span>
      <span class="kpi-strip-sub">ordonnances ce mois</span>
    </div>
  </div>
  <div class="kpi-strip-divider"></div>
  <div class="kpi-strip-item danger-accent">
    <i class="pi pi-times-circle text-danger"></i>
    <div class="kpi-strip-body">
      <span class="kpi-strip-label">Non substituées</span>
      <span class="kpi-strip-value text-danger">{{ stats.nbNonSubstituees }}</span>
      <span class="kpi-strip-sub">potentiel non exploité</span>
    </div>
  </div>
</div>
```

**Tableau** : DCI, Nb ordonnances substituables, Nb substituées, Taux, Δ vs mois précédent

---

## Phase 6 — Nouveaux rapports secondaires
**Effort** : 6 jours | **Risque** : faible à moyen

### 6.1 — Analyse des remises (`reports/finance/pnl/remises-analysis/`)

**KPI strip** : Montant total remises, Impact marge (%), Remise max accordée, Collaborateur le plus remiseur  
- Impact des remises sur la marge globale (montant total remises accordées)
- Remises par collaborateur — qui accorde le plus et pour quels montants
- Remises par type client (comptant vs tiers-payant vs différé)

### 6.2 — Démarque & pertes (`reports/stock/demarque/`)

**KPI strip** : Valeur démarque totale, Nb produits, % du stock, Démarque péremption vs casse  
- Produits sortis sans vente : casse, péremption, vol
- Valeur de la démarque par période
- Classement par famille / fournisseur

### 6.3 — Rétention clients (`reports/partners/client-retention/`)

**KPI strip** : Taux rétention M+1, Taux rétention M+3, Nouveaux clients %, Clients perdus  
- Taux de retour clients à 30j / 60j / 90j
- Cohortes mensuelles : % de clients du mois M actifs en M+1, M+3, M+6
- Mix nouveaux clients vs récurrents

---

## Récapitulatif & priorisation

| Phase | Intitulé | Effort | Valeur | Priorité |
|---|---|---|---|---|
| **0** | Restructuration architecturale (alléger mvt-caisse, créer comptabilite, y intégrer activity-summary) | 1 j | Très haute | 🔴 Immédiat |
| **1** | Consolidation (fusion ABC+Rotation, Profitability→Finance, bloc Créances, renommage crédits TP, enum SaleType) | 3 j | Haute | 🔴 Immédiat |
| **2** | Enrichissement `home-base` (card différés clients, KPIs facturation sur card TP, liens rapides) | 2 j | Très haute | 🔴 Immédiat |
| **3** | Infrastructure partagée (DateRange, Export, Chart, memory leaks) | 3 j | Haute | 🟠 Court terme |
| **4.1** | Situation unifiée des créances (TP + différés) | 2 j | Très haute | 🟠 Court terme |
| **4.2** | Vieillissement différés clients | 1 j | Haute | 🟠 Court terme |
| **4.3** | Analytics avoirs | 2 j | Haute | 🟠 Court terme |
| **4.4** | Taux de recouvrement TP par organisme | 1 j | Haute | 🟠 Court terme |
| **5.1** | Ventes par collaborateur | 2 j | Haute | 🟡 Moyen terme |
| **5.2** | Saisonnalité des ventes | 2 j | Haute | 🟡 Moyen terme |
| **5.3** | Substitution génériques | 2 j | Haute (réglementaire) | 🟡 Moyen terme |
| **6.1** | Analyse remises | 2 j | Moyenne | 🟡 Moyen terme |
| **6.2** | Démarque & pertes | 2 j | Moyenne | 🟢 Long terme |
| **6.3** | Rétention clients | 2 j | Moyenne | 🟢 Long terme |

**Total estimé** : ~27 jours développeur (-2j vs version précédente — pas de nouveau dashboard à créer)

---

## Fichiers clés à modifier / créer

### À modifier
```
entities/mvt-caisse/mvt-caisse.component.html                 ← désactiver 7 onglets (R2)
entities/reports/stock/stock-reports.component.html            ← désactiver abc+rotation (R2)
entities/reports/finance/finance-reports.component.html        ← désactiver vieillissement+concentration (R2)
entities/reports/sales/sales-reports.component.html            ← désactiver profitability (R2)
entities/raport-gestion/activity-summary/activity-summary.*   ← Phase 1.4 : renommer "Crédits TP"
home/home-base/home-base.component.*                           ← Phase 2 : enrichir (différés, KPIs TP)
```

### À créer
```
entities/comptabilite/comptabilite.component.*
  (balance-mvt-caisse, taxe-report, tableau-pharmacien,
   recap-caisse, declaration-tva, export-comptable, activity-summary)

                                                               ← PAS de reports/dashboard : home-base est le dashboard

entities/reports/stock/stock-abc/*                             ← fusion abc+rotation

entities/reports/finance/creances/
  ├── creances.component.*                                     ← conteneur onglets
  ├── situation-creances/*                                     ← NOUVEAU
  ├── vieillissement-differes/*                                ← NOUVEAU
  ├── avoirs-analytics/*                                       ← NOUVEAU
  └── (vieillissement-creances + concentration-payers migrés)

entities/reports/finance/tresorerie/
  └── taux-recouvrement-tp/*                                   ← NOUVEAU

entities/reports/sales/sales-by-staff/*                        ← NOUVEAU
entities/reports/sales/seasonality/*                           ← NOUVEAU
entities/reports/sales/generics-substitution/*                 ← NOUVEAU
entities/reports/finance/pnl/remises-analysis/*               ← NOUVEAU

shared/components/date-range-filter/*
shared/services/report-export.service.ts
shared/services/chart-builder.service.ts
shared/model/enums/sale-type.enum.ts
shared/model/enums/stock-abc.enum.ts
```
