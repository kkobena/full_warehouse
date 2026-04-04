# Analyse & Plan d'Amélioration — Module Gestion des Péremptions

> **Pharma-Smart** — Angular 20 / Spring Boot 4  
> Date de l'analyse : 2026-04-02  
> Périmètre : `src/main/webapp/app/entities/gestion-peremption/`

---

## Table des matières

1. [Inventaire de l'existant](#1-inventaire-de-lexistant)
2. [Bugs & régressions détectés](#2-bugs--régressions-détectés)
3. [Analyse comparative avec les logiciels de référence](#3-analyse-comparative-avec-les-logiciels-de-référence)
4. [Fonctionnalités manquantes](#4-fonctionnalités-manquantes)
5. [Lacunes UX — diagnostic argumenté](#5-lacunes-ux--diagnostic-argumenté)
6. [Axes d'amélioration prioritaires](#6-axes-damélioration-prioritaires)
7. [Plan de mise en œuvre par phases](#7-plan-de-mise-en-œuvre-par-phases)
8. [Synthèse des gains attendus](#8-synthèse-des-gains-attendus)

---

## 1. Inventaire de l'existant

### 1.1 Architecture du module

```
gestion-peremption/
├── gestion-peremption.component      → shell 2 onglets (navigation latérale)
├── lot-perimes/                       → liste des lots périmés/proches
├── lot-a-detruire/                    → file d'attente de destruction
├── ajout-perimes/                     → saisie de session (route séparée /edit)
├── model/
│   ├── lot-perimes.ts                 → LotPerimes, LotFilterParam, LotPerimeValeurSum
│   ├── product-to-destroy.ts          → ProductToDestroy, filtres, payloads
│   └── peremption-statut.ts           → {libelle, days, mouths, years}
└── product-to-destroy.service.ts      → CRUD + export (PDF/Excel/CSV)
```

### 1.2 Flux utilisateur actuel

```
[Tableau de bord péremptions]
        │
        ├─── Onglet "Lots périmés" (LotPerimesComponent)
        │       ├── Filtres : type / recherche / nbJours / plage dates / magasin / rayon / fournisseur
        │       ├── KPI cards : nb périmés, quantités, valeurs achat/vente, prochaines 30j, retours fourn.
        │       ├── Table lazy : libellé, code, n°lot, date péremption, qté, PA, PV, statut, fournisseur, rayon
        │       └── Actions : retirer stock (unitaire), retirer tout (sélection > 1), export PDF/Excel/CSV
        │
        └─── Onglet "Lots à détruire" (LotADetruireComponent)
                ├── Filtres similaires sans dayCount
                ├── KPI cards : nb détruits, quantités, valeurs
                ├── Table lazy : idem + colonne détruits oui/non + date destruction
                └── Actions : détruire unitaire, détruire en masse (sélection > 1), export
                
[Route /gestion-peremption/edit] → AjoutPerimesComponent
        ├── Saisie séquentielle : produit (autocomplete) → n°lot → date péremption → quantité
        ├── Table de session en cours (édition inline quantité)
        └── Clôture définitive (retire du stock)
```

### 1.3 Ce qui fonctionne bien

| Point positif | Détail |
|---|---|
| Lazy loading paginé | Les deux tableaux principaux utilisent PrimeTable lazy, correct |
| KPI synthétiques | 4 cards avec les métriques clés sur chaque onglet |
| Export multi-format | PDF, Excel, CSV disponibles sur les deux vues |
| Workflow de saisie guidé | Focus automatique champ-à-champ dans ajout-perimes |
| Tag de sévérité dynamique | Coloration rouge/orange/info selon `days` |
| Multi-entrepôt | Prise en charge magasin/stockage si `isMono=false` |

---

## 2. Bugs & régressions détectés

### 🔴 Bug critique — Filtre `toDate` ignoré

**Fichier :** `lot-perimes/lot-perimes.component.ts` ligne 282

```typescript
// ❌ Code actuel (bug)
private buidParams(): LotFilterParam {
  return {
    ...
    fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
    toDate: DATE_FORMAT_ISO_DATE(this.fromDate),  // ← COPIE de fromDate !
    ...
  };
}

// ✅ Correction attendue
toDate: DATE_FORMAT_ISO_DATE(this.toDate),
```

**Impact :** Toute requête avec un intervalle de dates renvoie des résultats filtrés uniquement sur `fromDate`. La recherche par plage de dates est silencieusement cassée.

---

### 🔴 Bug — Spinner masqué avant la requête dans `LotADetruireComponent`

**Fichier :** `lot-a-detruire/lot-a-detruire.component.ts` ligne 358

```typescript
private loadPage(page?: number): void {
  this.spinner().hide();     // ← spinner caché AVANT la requête HTTP
  const pageToLoad = page || this.page || 1;
  this.productToDestroyService.query({...}).subscribe({...});
}
```

**Impact :** L'indicateur de chargement n'est jamais affiché lors de la pagination. L'utilisateur voit un tableau vide sans feedback visuel.

---

### 🟠 Bug fonctionnel — Bouton "Retour fournisseur" appelle la mauvaise action

**Fichier :** `lot-perimes/lot-perimes.component.html` ligne 302–310

```html
<!-- ❌ Les deux boutons appellent confirmRetirerDialog(data) ! -->
<jhi-remove-tex-button (click)="confirmRetirerDialog(data)"></jhi-remove-tex-button>
<jhi-cta
  (click)="confirmRetirerDialog(data)"   ← devrait déclencher un workflow retour fournisseur
  [tooltip]="'warehouseApp.buttons.retourFournisseur'"
  icon="pi pi-reply"
  severity="info"
></jhi-cta>
```

**Impact :** Le workflow retour fournisseur est inexistant. L'icône trompe l'utilisateur.

---

### 🟡 Anomalie — `selectedLotPerimes.length > 1` pour le bouton "Tout retirer"

**Fichier :** `lot-perimes/lot-perimes.component.html` ligne 154

Le bouton d'action collective n'apparaît que si **plus d'un** article est sélectionné, mais il n'est pas accessible si exactement **un** article est coché (comportement incohérent avec le `lot-a-detruire` qui a le même problème).

---

## 3. Analyse comparative avec les logiciels de référence

| Fonctionnalité | Pharmagest iConcept | Winpharma | LGPI (Pharma) | Caducée | **Pharma-Smart** |
|---|:---:|:---:|:---:|:---:|:---:|
| Alertes proactives configurables (J-90/J-60/J-30) | ✅ | ✅ | ✅ | ✅ | ❌ |
| Tableau de bord chronologique / timeline | ✅ | ✅ | ❌ | ✅ | ❌ |
| Workflow retour fournisseur complet (BR + avoir) | ✅ | ✅ | ✅ | ✅ | ❌ stub |
| PV de destruction éditable et imprimable | ✅ | ✅ | ✅ | ❌ | ❌ |
| Liaison bons de réception → lots à risque | ✅ | ✅ | ✅ | ❌ | ❌ |
| Filtre par classe thérapeutique / ATC | ✅ | ❌ | ✅ | ❌ | ❌ |
| Gestion stupéfiants & liste I/II (traçabilité renforcée) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Badges de comptage sur les onglets | ✅ | ✅ | ✅ | ✅ | ❌ |
| Coloration conditionnelle des lignes | ✅ | ✅ | ✅ | ✅ | ❌ |
| Tri des colonnes de la table | ✅ | ✅ | ✅ | ✅ | ❌ |
| Filtre famille produit | ✅ | ✅ | ✅ | ❌ | ❌ (champ présent dans le modèle mais non affiché) |
| Notification email / push automatique | ✅ | ❌ | ✅ | ❌ | ❌ |
| Historique des destructions traçable | ✅ | ✅ | ✅ | ✅ | ✅ (voir note) |
| Vue calendrier des prochaines péremptions | ✅ | ❌ | ❌ | ❌ | ❌ |

> **Note traçabilité destructions :** La traçabilité est **complète au niveau backend** via deux mécanismes :  
> — `ProductsToDestroy` (table `products_to_destroy`) : stocke `numLot`, `datePeremption`, `dateDestuction`, `quantity`, `stockInitial`, `user`, `magasin`, `destroyed`  
> — `InventoryTransaction` avec `MouvementProduit.RETRAIT_PERIME` : créée automatiquement par `InventoryTransactionBuilder` lors de chaque destruction, enregistre `quantityBefore` / `quantityAfter` / `costAmount`  
> Ce qui manque côté frontend : rendre cette traçabilité **visible** (lien vers `/produits/transaction?type=RETRAIT_PERIME`) et éditer un PV PDF (qui n'est pas encore généré).

**Score actuel Pharma-Smart :** 3/15 fonctionnalités de référence couvertes complètement (traçabilité destructions mise à jour).

---

## 4. Fonctionnalités manquantes

### 4.1 Alertes proactives — priorité HAUTE

**Situation :** Aucun mécanisme d'alerte automatique n'existe. L'utilisateur doit aller consulter la liste manuellement pour découvrir les péremptions imminentes.

**Impact métier :** Dans les officines, les péremptions non détectées à temps entraînent :
- Des pertes financières (produits invendables)
- Des risques de dispensation de médicaments périmés
- Des sanctions réglementaires (IGAS, ANSM)

**Attendu :**
- Seuils configurables (J-90, J-60, J-30, J-7) déclenchant une icône/badge dans la navigation
- Email automatique hebdomadaire aux responsables
- Bandeau d'alerte sur le tableau de bord principal si produits périmés non traités

---

### 4.2 Workflow retour fournisseur complet — priorité HAUTE

**Situation :** Le bouton avec l'icône `pi-reply` (retour fournisseur) n'est qu'un alias du retrait de stock. Il n'existe aucun workflow dédié.

**Attendu :**
1. Sélection des lots à retourner (avec vérification des conditions de retour du fournisseur)
2. Génération d'un **Bon de Retour (BR)** imprimable (PDF)
3. Mise à jour automatique du stock et des comptes fournisseurs
4. Suivi de l'avoir attendu (statut : en attente / reçu / partiel)
5. Historique des retours par fournisseur

---

### 4.3 Procès-Verbal de destruction — priorité HAUTE (réglementaire)

**Situation :** La destruction d'un lot se fait en un clic. La traçabilité **existe déjà** au niveau des données :

```
ProductsToDestroy (table products_to_destroy)
  ├── numLot, datePeremption, dateDestuction
  ├── quantity, stockInitial, prixAchat, prixUnit
  ├── user (pharmacien qui a effectué la destruction)
  ├── magasin, fournisseurProduit
  └── destroyed: boolean

InventoryTransaction (type = RETRAIT_PERIME)
  ├── quantityBefore / quantityAfter
  ├── costAmount, regularUnitPrice
  └── createdAt, user, magasin, storage
```

**Ce qui manque :** Aucun **document PDF officiel** n'est généré depuis ces données.

**Attendu (réglementation française — article R. 4235-12 CSP) :**
- PV de destruction numéroté, signé par le pharmacien titulaire
- Généré depuis les données `ProductsToDestroy` + `InventoryTransaction.RETRAIT_PERIME` déjà en base
- Template Thymeleaf → Flying Saucer PDF (comme les autres rapports du projet)
- Archivage électronique pendant 10 ans minimum

> **Effort réduit :** Les données sont toutes présentes. Il s'agit uniquement de créer un template PDF et un endpoint d'export — pas de nouveau domaine métier.

---

### 4.4 Filtre "Famille de produit" dans l'interface — priorité MOYENNE

**Situation :** `LotFilterParam` possède le champ `familleProduitId` et le composant `lot-perimes` déclare `selectedFamilleProduit: IFamilleProduit = null`, mais aucun select de famille n'est rendu dans le template.

**Attendu :**
- Ajout d'un `p-select` `familles de produits` dans la toolbar (les données sont probablement déjà disponibles via un service)

---

### 4.5 Vue chronologique / Timeline — priorité MOYENNE

**Situation :** Les KPI montrent uniquement "prochaines péremptions dans 30j" (un nombre). L'utilisateur ne peut pas visualiser la distribution temporelle.

**Attendu :**
- Vue calendrier (ou graphique bar/line Chart.js déjà intégré au projet) montrant la distribution des péremptions sur 3–6 mois
- Sélection d'une date sur le graphique → filtre le tableau

---

### 4.6 Liaisons bons de réception — priorité MOYENNE

**Situation :** Aucun lien entre les lots commandés/réceptionnés et leur date de péremption prévisionnelle. Les lots à risque ne sont pas identifiés lors de la réception.

**Attendu :**
- À la réception, alerte si la date de péremption d'un lot est < seuil configuré
- Lien depuis un lot périmé → bon de commande d'origine

---

### 4.7 Notifications email — priorité BASSE

**Situation :** Zéro notification automatique (alors que la stack supporte Gmail SMTP via `spring.mail`).

**Attendu :**
- Job quotidien (Spring `@Scheduled`) envoyant un récapitulatif des lots proches péremption
- Paramétrage des destinataires dans la configuration

---

## 5. Lacunes UX — diagnostic argumenté

### 5.1 Toolbar surchargée — cognitif overload

**Problème :** La toolbar contient jusqu'à **8 filtres simultanés** (type, recherche texte, dayCount, dateDebut, dateFin, magasin, stockage, rayon, fournisseur) sur une seule ligne horizontale.

**Pourquoi c'est problématique :**
- Sur les écrans < 1400px, les filtres se chevauchent ou se tronquent
- Selon la loi de Hick (1952) : le temps de décision augmente avec le nombre de choix présentés
- Les filtres peu utilisés (magasin, stockage) polluent visuellement les filtres primaires (recherche, type)

**Solution :** Séparer en filtres primaires (toujours visibles) et filtres avancés (collapsed par défaut derrière un bouton "Filtres avancés").

---

### 5.2 Aucun badge de comptage sur les onglets

**Problème :** Les onglets "Lots périmés" et "Lots à détruire" n'affichent pas le nombre d'éléments. L'utilisateur doit naviguer dans chaque onglet pour savoir s'il y a des éléments urgents.

**Pourquoi c'est problématique :**
- Dans les logiciels de gestion de stock (Pharmagest, Winpharma), les compteurs dans les menus sont des **indicateurs visuels immédiats** permettant de prioriser l'action
- Un pharmacien surcharge de travail ne consulte pas un onglet vide — sans badge, il pourrait ignorer 50 lots périmés

**Solution :**
```html
<a ngbNavLink>
  Lots périmés
  @if (lotPerimeValeurSum?.count > 0) {
    <span class="badge bg-danger ms-1">{{ lotPerimeValeurSum.count }}</span>
  }
</a>
```

---

### 5.3 Lignes de tableau sans coloration conditionnelle

**Problème :** Tous les lots (périmés depuis 6 mois, proches dans 5 jours, ou encore à 30 jours) s'affichent avec le même fond blanc. Seul le tag de statut change.

**Pourquoi c'est problématique :**
- Le regard humain traite les couleurs **avant** le texte (vision préattentive)
- Dans les applications de soins (cliniques, pharmacies), le code couleur rouge/orange/vert est une convention universelle de risque
- L'utilisateur doit lire chaque libellé de statut pour identifier les urgences — perte de temps × nb de lignes

**Solution :** `[ngClass]` sur `<tr>` :
```html
<tr [class.table-danger]="data.peremptionStatut.days < 0"
    [class.table-warning]="data.peremptionStatut.days >= 0 && data.peremptionStatut.days <= 30">
```

---

### 5.4 Saisie des périmés dans une route séparée — rupture de contexte

**Problème :** L'ajout d'un lot périmé nécessite une navigation vers `/gestion-peremption/edit`, déchargeant complètement le tableau de bord. Au retour, les filtres précédents sont perdus.

**Pourquoi c'est problématique :**
- L'utilisateur perd le contexte (il avait peut-être filtré sur un fournisseur précis)
- Sur une pharmacie avec 30+ lots à saisir, naviguer aller-retour est fastidieux
- Les logiciels modernes (Pharmagest) utilisent des panneaux latéraux ou des drawers qui ne quittent pas la page principale

**Solution :** Transformer `AjoutPerimesComponent` en side panel (`p-drawer` PrimeNG) ou en section repliable dans la vue principale.

---

### 5.5 Pas de tri des colonnes

**Problème :** La table PrimeNG ne déclare aucun `pSortableColumn` sur ses en-têtes. L'utilisateur ne peut pas trier par date de péremption, quantité ou valeur.

**Pourquoi c'est problématique :**
- Trier par **date de péremption croissante** est le premier besoin naturel : "montrez-moi ce qui périme le plus tôt"
- Trier par **valeur décroissante** : "montrez-moi les pertes les plus importantes"
- Sans tri, l'utilisateur doit scanner visuellement la liste entière

---

### 5.6 KPI cards non interactives

**Problème :** Les 4 cartes de statistiques (produits périmés, valeurs, prochaines péremptions, retours fournisseur) s'affichent mais n'ont aucun comportement au clic.

**Pourquoi c'est problématique :**
- L'affordance d'une card Bootstrap avec ombre et chiffres en gras suggère une interactivité
- Le clic sur "Prochaines péremptions (30j)" devrait automatiquement filtrer sur `type=EN_COURS` et `dayCount=30`
- Rupture entre l'information affichée et la capacité d'action immédiate

---

### 5.7 Dates non formatées (raw string depuis backend)

**Problème :** `{{ data.datePeremption }}` affiche une chaîne ISO brute (ex: `2025-11-30`).

**Attendu :** Utiliser le pipe `date:'dd/MM/yyyy'` (format français) ou créer un pipe `pharmaDate`.

---

### 5.8 Message de confirmation non informatif pour la clôture

**Fichier :** `ajout-perimes.component.ts` ligne 148

```typescript
'Êtes-vous sûr de vouloir clôtuer ? Les quantités saisies seront définitivement retirées du stock.'
```

**Problème :** Ce message ne donne pas le résumé des lots (nombre de lignes, quantité totale, valeur totale) avant la validation définitive. Pour une action irréversible, l'utilisateur doit pouvoir **mesurer l'impact** avant de confirmer.

---

## 6. Axes d'amélioration prioritaires

### Axe 1 — Correction des bugs (ROI immédiat, 0 risque)

| Priorité | Bug | Effort |
|:---:|---|:---:|
| 🔴 P0 | `toDate` copié sur `fromDate` dans `lot-perimes` | 1h |
| 🔴 P0 | Spinner caché avant la requête dans `lot-a-detruire` | 30min |
| 🟠 P1 | Bouton retour fournisseur = alias retrait stock | 2h (stub → modal dédiée) |
| 🟡 P2 | Bouton action collective visible seulement si > 1 | 30min |

---

### Axe 2 — Quick wins UX (fort impact, faible effort)

| Priorité | Amélioration | Effort |
|:---:|---|:---:|
| 🟠 P1 | Coloration conditionnelle des lignes | 1h |
| 🟠 P1 | Badges de comptage sur les onglets | 2h |
| 🟠 P1 | Tri des colonnes (pSortableColumn) | 2h |
| 🟠 P1 | Formatage des dates avec pipe `date` | 30min |
| 🟡 P2 | Filtre famille produit (champ déjà dans le modèle) | 3h |
| 🟡 P2 | KPI cards interactives (clic → filtre auto) | 3h |
| 🟡 P2 | Message de clôture avec résumé des lots | 2h |

---

### Axe 3 — Fonctionnalités métier manquantes (valeur ajoutée)

| Priorité | Fonctionnalité | Effort estimé | Remarque |
|:---:|---|:---:|---|
| 🟠 P1 | Alertes proactives (badge navigation + bandeau dashboard) | 5j | — |
| 🟠 P1 | Filtres avancés (accordion) pour désengorger la toolbar | 2j | — |
| 🟠 P1 | Lien "Voir transactions RETRAIT_PERIME" dans lot-a-detruire | 1j | Données existantes, UI manquante |
| 🟡 P2 | Vue chronologique Chart.js (distribution péremptions) | 3j | — |
| 🟡 P2 | Drawer latéral pour saisie sans navigation | 3j | — |
| 🟡 P2 | PV de destruction PDF (template Thymeleaf) | 3j | Données déjà en base, PDF à créer |
| 🟡 P2 | Passerelle lot-périmé → RetourBon (workflow existant) | 5j | RetourBon déjà implémenté côté backend |
| 🔵 P3 | Notification email automatique (Spring Scheduler) | 3j | SMTP déjà configuré |
| 🔵 P3 | Liaison bons de réception → lots à risque | 10j | — |

---

## 7. Plan de mise en œuvre par phases

### Phase 1 — Stabilisation (Sprint 1, ~1 semaine)

**Objectif :** Corriger les bugs bloquants et quick wins UX.

**Backend :**
- [ ] Vérifier/corriger l'endpoint `/api/lot` pour le filtre `toDate` (si le bug est aussi côté backend)

**Frontend :**
- [ ] **BUG** : Corriger `buidParams()` dans `lot-perimes` → `toDate: DATE_FORMAT_ISO_DATE(this.toDate)`
- [ ] **BUG** : Déplacer `this.spinner().hide()` dans `onSuccess()` et `onError()` de `lot-a-detruire`
- [ ] **BUG** : Corriger le bouton retour fournisseur (affichage modal dédiée ou désactivation + TODO)
- [ ] Ajouter `[class.table-danger/warning]` sur les lignes des deux tables
- [ ] Ajouter `pSortableColumn` sur les colonnes date péremption, quantité, valeur achat
- [ ] Formater les dates avec `| date:'dd/MM/yyyy'`
- [ ] Corriger la condition `> 1` → `>= 1` pour le bouton d'action collective
- [ ] Ajouter le filtre `famille produit` dans la toolbar de `lot-perimes`

---

### Phase 2 — Enrichissement UX (Sprint 2–3, ~3 semaines)

**Objectif :** Améliorer l'ergonomie et réduire les frictions.

- [ ] **Badges de comptage** sur les onglets (alimentés par `LotPerimeValeurSum.count`)
- [ ] **Filtres avancés** : regrouper magasin/stockage/rayon/fournisseur dans un accordion `p-panel` replié par défaut. La toolbar ne montre que : type, recherche, dayCount, plage dates.
- [ ] **KPI cards interactives** : `(click)` sur les cards → application automatique des filtres correspondants
- [ ] **Message de confirmation** de clôture enrichi : afficher nb lignes + quantité totale + valeur totale avant confirmation
- [ ] **Drawer PrimeNG** (`p-drawer`) pour la saisie des périmés, sans navigation vers `/edit` — conserver la route pour compatibilité

---

### Phase 3 — Alertes proactives (Sprint 4, ~2 semaines)

**Objectif :** Passer d'un module réactif à un module proactif.

**Backend :**
- [ ] Endpoint `/api/lot/upcoming-alerts?days=7,30,60,90` → retourne des compteurs
- [ ] Config params : `APP_PERIMES_ALERT_D30`, `APP_PERIMES_ALERT_D60`, `APP_PERIMES_ALERT_D90`

**Frontend :**
- [ ] Service `PeremptionAlertService` appelé au démarrage de l'app
- [ ] Bandeau d'alerte sticky sur le layout principal si produits périmés non traités
- [ ] Badge rouge sur l'entrée "Gestion péremptions" dans le menu latéral
- [ ] Vue **Chart.js** de distribution des péremptions sur 6 mois (bar chart mensuel)

---

### Phase 4 — PV destruction & liaison RetourBon (Sprint 5–7, ~4 semaines)

**Objectif :** Exposer la traçabilité existante et connecter le workflow RetourBon depuis le module péremptions.

**PV de destruction (données déjà en base, PDF à créer) :**
- [ ] Template Thymeleaf `pv-destruction.html` alimenté par `ProductsToDestroy` + `InventoryTransaction.RETRAIT_PERIME`
- [ ] Endpoint backend : `GET /api/product-to-destroy/{id}/pv-pdf` → génération Flying Saucer
- [ ] Bouton "Télécharger PV" dans la colonne "Détruit le" de `lot-a-detruire`
- [ ] Numérotation séquentielle du PV + signature pharmacien (champ `AppUser.fullName`)

**Passerelle Péremptions → RetourBon (backend existe, passerelle UI manquante) :**
- [ ] Dans `lot-perimes`, le bouton "Retour fournisseur" ouvre un dialog de sélection de la `Commande` source
- [ ] Pré-remplissage du formulaire `RetourBon` avec les données du lot périmé
- [ ] Alternative : créer un `RetourBon` sans `Commande` obligatoire (modification domaine mineure)
- [ ] Tableau de suivi des retours dans un 3e onglet (API `RetourBonService.findAll` existe)

**Visibilité de la traçabilité dans le frontend :**
- [ ] Dans `lot-a-detruire`, ajouter un lien vers `/produits/transaction?type=RETRAIT_PERIME&produitId=X`
- [ ] Dans la fiche produit (`/produits/:id`), onglet "Lots & Péremptions" filtré sur `RETRAIT_PERIME`

---

### Phase 5 — Intégration avancée (Sprint 8–10, ~6 semaines)

**Objectif :** Intégration dans le flux global de gestion de stock.

- [ ] Alerte à la réception : si date péremption < seuil, popup de confirmation
- [ ] Lien depuis lot périmé → bon de commande d'origine
- [ ] Notification email automatique (Spring `@Scheduled`, configurable)
- [ ] Traçabilité renforcée pour médicaments stupéfiants / liste I / liste II

---

## 8. Synthèse des gains attendus

### Après Phase 1 (Bugfixes + Quick Wins UX)

| Indicateur | Avant | Après estimé |
|---|---|---|
| Temps moyen pour identifier les lots les plus urgents | ~3–5 min (scan manuel) | < 30 sec (couleurs + tri) |
| Confiance dans le filtre dates | ❌ résultats incorrects | ✅ corrigé |
| Feedback de chargement | ❌ absent | ✅ spinner fonctionnel |
| Clics pour filtrer par famille | N/A (impossible) | 1 clic |

### Après Phase 2–3 (Alertes + UX avancée)

| Indicateur | Avant | Après estimé |
|---|---|---|
| Produits périmés détectés sans visite du module | 0 | ≥ 80% (alertes proactives) |
| Charge cognitive à l'ouverture du module | Élevée (8 filtres, table monochrome) | Faible (filtres avancés, codes couleur) |
| Temps de saisie d'un lot périmé | Navigation + retour | Inline (drawer) |

### Après Phase 4–5 (Conformité réglementaire)

| Indicateur | Avant | Après estimé |
|---|---|---|
| Conformité réglementaire (PV destruction) | ❌ Non conforme | ✅ Conforme CSP art. R4235 |
| Workflow retour fournisseur | ❌ Inexistant | ✅ Complet (BR + suivi avoir) |
| Traçabilité des destructions | Partielle | Complète (archivage 10 ans) |

---

## 9. Architecture de navigation — Position du module dans le menu

### 9.1 Positionnement actuel

```
navigation.service.ts — buildNavItems()
│
├── Gestion Courante        (Ventes, Mvt Caisse)
├── Gestion Stock           ← gestion-peremption EST ICI
│   ├── Catalogue produits
│   ├── Commandes
│   ├── Transactions inventaire
│   ├── Ajustements de stock
│   ├── ⚠️  Gestion Péremptions   ← sous-menu parmi 6 items
│   ├── Inventaire
│   └── Dépôts
├── Facturation
├── Référentiel
├── Rapports & Statistiques
└── Administration
```

### 9.2 Est-il pertinent d'avoir un menu à part entière ?

**Réponse courte : Non — mais sa visibilité doit être renforcée.**

#### Argument pour rester sous "Gestion Stock" ✅

| Raison | Explication |
|---|---|
| **Cohérence sémantique** | Les péremptions sont un sous-ensemble de la gestion de stock ; les isoler dans un menu racine briserait la logique métier |
| **Audiences identiques** | Les utilisateurs de la gestion stock et des péremptions sont les mêmes personnes (responsable stock, pharmacien adjoint) |
| **Standard de l'industrie** | Pharmagest, Winpharma et LGPI intègrent tous les péremptions dans le menu stock — aucun n'en fait un menu de premier niveau |
| **Réduction cognitive** | Plus un menu de premier niveau est ajouté, plus la charge mentale de navigation augmente (loi de Hick) |

#### Argument contre un menu séparé ❌

| Raison | Contre-argument |
|---|---|
| "C'est une fonction critique" | La criticité s'exprime via un **badge d'alerte**, pas via un menu racine supplémentaire |
| "Les utilisateurs ne le trouvent pas" | Le problème est la découvrabilité → solution : badge + widget dashboard, pas restructuration |

#### Ce que font les logiciels de référence

| Logiciel | Positionnement péremptions | Mécanisme de visibilité |
|---|---|---|
| **Pharmagest iConcept** | Sous-menu "Stock & Approvisionnements" | Badge rouge dans la sidebar + alerte à l'ouverture |
| **Winpharma** | Sous-menu "Gestion du stock" | Indicateur visuel dans la barre d'état basse |
| **LGPI** | Sous-menu "Stock → Gestion lots" | Notification popup au démarrage + rapport journalier |
| **Caducée** | Menu autonome "Péremptions" (seule exception) | Compteur en temps réel |
| **Dispenso (Belgique)** | Sous-menu "Gestion du stock" | Tableau de bord dédié en page d'accueil |

**Conclusion :** Caducée est le seul à isoler les péremptions. C'est une **exception**, non une règle, et leur modèle répond à une base d'utilisateurs avec des exigences réglementaires renforcées (AFMPS belge). Pour Pharma-Smart ciblant les officines françaises, rester sous "Gestion Stock" est le bon choix — **à condition d'ajouter les mécanismes de visibilité manquants**.

---

### 9.3 Interactions manquantes avec les autres menus

#### Carte des interactions existantes vs attendues

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ECOSYSTÈME PÉREMPTIONS                           │
│                                                                     │
│  /commande ──────────────[RECEPTION]──────→ ⚠️ ALERTE PEREMPTION   │
│  (Commandes)              Lot reçu avec       (manquant)            │
│                           date péremption                           │
│                                ↓                                   │
│  /produits ──────────────[FICHE PRODUIT]──→ Onglet "Lots & Péremptions"│
│  (Catalogue)              Tous les lots        (manquant)           │
│                           par produit                               │
│                                ↓                                   │
│  /gestion-peremption ────[RETRAIT STOCK]──→ /features-ajustement   │
│  (Péremptions) ←→         Lot périmé          Ajustement auto.      │
│  existant mais           → destruction        (manquant)            │
│  non connecté                 ↓                                     │
│                          [RETOUR FOURN.]──→ /fournisseur            │
│                           Bon de retour       Avoir fournisseur      │
│                                               (manquant)            │
│                                ↓                                   │
│  /reports ───────────────[RAPPORT PERTE]──→ Valorisation pertes     │
│  (Rapports)               Mensuel/annuel      péremptions            │
│                                               (manquant)            │
│                                ↓                                   │
│  /inventaire ────────────[INVENTAIRE]─────→ Identification lots      │
│  (Inventaire)             Rapprochement       périmés pendant        │
│                           physique            inventaire             │
│                                               (manquant)            │
└─────────────────────────────────────────────────────────────────────┘
```

#### Détail des 5 interactions manquantes

**① Commandes → Péremptions (Alerte à la réception)**

```
Déclencheur : Réception d'un bon de commande
Condition   : datePeremption du lot reçu < (today + seuil_alerte_jours)
Action      : Popup d'avertissement dans /commande avec lien vers /gestion-peremption
Données     : numLot, produit, fournisseur, datePeremption, quantité
Intérêt     : Stopper le problème à la source plutôt que le traiter après
```

**② Catalogue produits → Péremptions (Onglet lots sur la fiche produit)**

```
Déclencheur : Ouverture de la fiche d'un produit dans /produits
Action      : Onglet "Lots" listant tous les lots actifs avec statut péremption
Données     : numLot, datePeremption, quantité, statut (tag coloré), lien deep-link → /gestion-peremption?produitId=X
Intérêt     : Contextualisation — l'utilisateur voit le problème là où il gère le produit
```

**③ Péremptions → Transactions inventaire (Traçabilité — ✅ DÉJÀ IMPLÉMENTÉE)**

```
Déclencheur : Destruction confirmée d'un lot dans lot-a-detruire
Action ACTUELLE : InventoryTransactionBuilder crée automatiquement une
                  InventoryTransaction avec MouvementProduit.RETRAIT_PERIME
                  (quantity, quantityBefore, quantityAfter, costAmount, user, magasin)
Entité backend : ProductsToDestroy → InventoryTransaction (type=RETRAIT_PERIME)
Statut : ✅ Fonctionnel côté backend
Lacune frontend : Aucun lien depuis lot-a-detruire vers /produits/transaction
                  pour visualiser l'historique des RETRAIT_PERIME par produit
Recommandation : Ajouter un bouton "Voir les transactions" qui navigue vers
                 /produits/transaction?type=RETRAIT_PERIME&produitId=X
```

**④ Péremptions → Retour fournisseur (Workflow RetourBon — partiellement existant)**

```
Backend existant : Entité RetourBon (table retour_bon) liée à Commande
                   RetourBonItem → InventoryTransaction(RETOUR_FOURNISSEUR)
                   RetourStatut : PROCESSING | VALIDATED | CLOSED
                   RetourBonService avec create/findAll/findAllByCommande/close
Lacune : RetourBon est lié à une Commande (bon de commande) et non
         directement aux lots périmés de ProductsToDestroy
Lacune frontend : Le bouton "Retour fournisseur" dans lot-perimes appelle
                  confirmRetirerDialog() (identique à "Retirer du stock") — BUG
Recommandation : Créer une passerelle entre ProductsToDestroy et RetourBon
                 depuis l'interface péremptions :
                 1. Identifier la Commande source du lot (via numLot)
                 2. Ouvrir le workflow RetourBon existant pré-rempli
                 3. Ou créer un RetourBon direct sans Commande (évolution domaine)
```

**⑤ Péremptions → Rapports (Valorisation des pertes)**

```
Déclencheur : Consultation des rapports dans /reports
Action      : Nouveau rapport "Pertes par péremption" (mensuel, par fournisseur, par famille)
Données     : Somme des valeurs achat/vente des lots détruits, évolution temporelle
Statut actuel : Aucun rapport péremption dans /reports/stock
Intérêt     : Pilotage de la politique d'achat (acheter moins de petites quantités si taux de perte élevé)
```

---

## 10. Conception optimale — analyse comparative UX

### 10.1 Modèles UX de référence dans l'industrie

#### Modèle A — "Gestion réactive" (actuel Pharma-Smart)

```
Utilisateur → Menu → Consulte liste → Agit sur les périmés
```
- ✅ Simple à implémenter
- ❌ Passif : l'utilisateur doit penser à aller vérifier
- ❌ Découverte tardive des problèmes
- ❌ Adapté aux pharmacies < 500 références

---

#### Modèle B — "Gestion proactive avec alertes" (Pharmagest, LGPI)

```
Système → Détecte seuil → Badge/notification → Utilisateur agit
```
- ✅ Standard de l'industrie pour les officines moyennes
- ✅ Réduction des oublis et des pertes
- ✅ Applicable avec le stack Spring Boot (Scheduler + mail déjà configuré)
- Effort : 2 sprints

---

#### Modèle C — "Gestion préventive intégrée" (SAP Pharma, Oracle Retail)

```
Réception → Prédiction péremption → Plan d'action → Exécution → Traçabilité → Rapport
```
- ✅ Optimal pour les grossistes et pharmacies > 2000 références
- ✅ Zéro perte grâce à la prévision des rotations
- ❌ Lourd à implémenter
- Effort : plusieurs mois, pertinent Phase 5+

---

#### Verdict pour Pharma-Smart

> **Cible recommandée : Modèle B** avec les éléments clés du Modèle C intégrés progressivement.  
> Pharma-Smart cible des officines françaises standard (200–2000 références). Le Modèle B est le standard industriel démontré, atteignable en 2 sprints.

---

### 10.2 Proposition UX complète — Architecture cible

#### Vision globale

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    DASHBOARD PRINCIPAL (/)                               │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────────────┐   │
│  │ Widget Ventes │  │Widget Stock   │  │ 🔴 Widget Péremptions       │   │
│  │              │  │              │  │ ● 12 lots périmés           │   │
│  └──────────────┘  └──────────────┘  │ ● 34 expirent < 30j        │   │
│                                       │ [→ Traiter maintenant]      │   │
│                                       └─────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘

SIDEBAR
├── Gestion Stock  🔴 (12)   ← badge sur le groupe parent
│   ├── Catalogue produits
│   ├── Commandes
│   ├── Gestion Péremptions 🔴 (12)  ← badge sur l'entrée
│   └── ...
```

#### Structure interne du module cible

```
/gestion-peremption
├── [Onglet 1] 📋 Lots Périmés          (12) badge rouge
│   ├── Filtres PRIMAIRES (toujours visibles) :
│   │   type | recherche | nbre de jours
│   ├── Filtres AVANCÉS (accordion, repliés) :
│   │   plage dates | magasin | stockage | rayon | fournisseur | famille
│   ├── KPI cards INTERACTIVES (clic = filtre auto)
│   ├── Graphique Chart.js — distribution mensuelle (6 mois)
│   └── Tableau ENRICHI :
│       ✅ Lignes colorées (rouge/orange/neutre)
│       ✅ Tri sur toutes colonnes
│       ✅ Dates formatées dd/MM/yyyy
│       ✅ Actions : Retirer stock | Retour fournisseur (workflow réel)
│
├── [Onglet 2] 🗑️  À Détruire           (5) badge orange
│   ├── (filtres similaires)
│   ├── Tableau avec actions : Détruire + génération PV
│   └── Téléchargement PV destruction
│
├── [Onglet 3] 🔄 Retours Fournisseur   (3) badge info  ← NOUVEAU
│   ├── Liste des demandes en cours
│   ├── Statut : En attente | Envoyé | Avoir reçu
│   └── Actions : Générer BR PDF | Marquer avoir reçu
│
└── [Panel latéral — Drawer]            ← saisie SANS navigation
    📦 Saisir un lot périmé
    └── Produit → N°Lot → Date → Quantité → Ajouter
        (remplace la route /edit qui reste pour compatibilité)
```

---

### 10.3 Comparaison design courant vs cible

| Dimension UX | État actuel | Cible recommandée | Gain |
|---|---|---|---|
| **Découvrabilité** | Menu passif, aucun indicateur | Badge rouge sur sidebar + widget dashboard | Critique |
| **Densité de la toolbar** | 8–9 filtres visibles simultanément | 3 primaires + accordion avancé | Charge cognitive –60% |
| **Scanning du tableau** | Monochrome, tri impossible | Couleurs conditionnelles + tri multi-colonnes | Temps –70% |
| **Saisie d'un lot** | Navigation + perte de contexte | Drawer latéral sans quitter la page | Friction –80% |
| **Actions contextuelles** | Retour fournisseur = retrait stock (bug) | Workflow distinct par action | Fiabilité +100% |
| **Interactivité des KPI** | Cards inertes | Clic → filtre automatique | Ergonomie |
| **Alertes** | Aucune | J-90/J-60/J-30/J-7 configurable | Proactivité |
| **Traçabilité** | Destruction sans document | PV PDF + ajustement auto | Conformité légale |
| **Intégration cross-module** | Module isolé | 5 points d'entrée contextuels | Cohérence métier |

---

### 10.4 Wireframe textuel — Vue "Lots Périmés" cible

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  📋 Lots Périmés    🗑️ À Détruire (5)    🔄 Retours (3)                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  [Tout ▼]  [🔍 Rechercher...]  [Nbre jours: ___]  [⚙️ Filtres avancés ▼]   │
│                                                                             │
│  ┌──────┬─────────────────────┬───────┐  ┌──────────────────────────────┐  │
│  │ 🔴 12│ Produits périmés    │       │  │  📊 Distribution 6 mois      │  │
│  │      │ 487 unités · 2.3M F │ → [1] │  │  ████ Jan  ████ Fev  ██ Mar  │  │
│  ├──────┼─────────────────────┤       │  │  (clic barre = filtre mois)  │  │
│  │ 🟠 34│ Expirent < 30j      │       │  └──────────────────────────────┘  │
│  │      │ 1.2K unités · 890K F│ → [2] │                                    │
│  ├──────┼─────────────────────┤       │                                    │
│  │ 🔵 8 │ Retours fournisseur │ → [3] │                                    │
│  └──────┴─────────────────────┴───────┘                                    │
│  [1] Clic → filtre type=PERIME   [2] Clic → dayCount=30  [3] → onglet 3    │
│                                                                             │
│  ┌──────┬──────────┬────────┬────────────┬───────┬──────┬────────┬────┐    │
│  │ Lib. ↕│ Code     │ N°Lot  │ Date pér. ↕│ Qté ↕│ PA ↕ │ Statut │ ☐ │    │
│  ├──────┼──────────┼────────┼────────────┼───────┼──────┼────────┼────┤    │
│  │🔴 Amoxil 500    │ L2401  │ 15/11/2025 │   120 │ 450  │ PÉRIMÉ │ ☑ │    │
│  │🟠 Doliprane 1g  │ L2450  │ 12/02/2026 │    48 │ 320  │ 41 j.  │ ☐ │    │
│  │⬜ Ibuprofène 400│ L2501  │ 30/06/2026 │   200 │ 280  │ 89 j.  │ ☐ │    │
│  └──────┴──────────┴────────┴────────────┴───────┴──────┴────────┴────┘    │
│                                          [🗑️ Retirer] [↩ Retour fourn.] [↗] │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
  (Drawer latéral)  │  📦 Saisir un lot périmé            │
                    ├─────────────────────────────────────┤
                    │  Produit : [Amoxicilline 500mg ▼ 🔍]│
                    │  N° Lot  : [L2401_______________]   │
                    │  Date    : [15/11/2025__________]   │
                    │  Quantité: [120] [➕ Ajouter]        │
                    │                                     │
                    │  ● L2401 · 120u · 15/11/2025  [🗑] │
                    │  ● L2402 · 50u  · 20/12/2025  [🗑] │
                    │                                     │
                    │  [🔒 Clôturer (2 lots · 170u)]      │
                    └─────────────────────────────────────┘
```

---

### 10.5 Recommandation de navigation — Intégration dans le menu

#### Modification recommandée de `navigation.service.ts`

```typescript
// navigation.service.ts — Gestion Stock avec badge d'alerte
{
  label: this.translateLabel('menuGestionStock'),
  faIcon: faTruckFast,
  badge: this.peremptionAlertService.getUrgentCount(), // ← NOUVEAU signal
  badgeSeverity: 'danger',
  children: [
    // ...
    {
      label: this.translateFullLabel('gestionPerimes.title'),
      routerLink: '/gestion-peremption',
      faIcon: faCalendarTimes,
      badge: this.peremptionAlertService.getUrgentCount(), // ← badge entrée
      badgeSeverity: 'danger',
    },
    // ...
  ],
}
```

#### Widget dashboard (page d'accueil)

```typescript
// home.component.ts — Widget péremptions proactif
// Affiché uniquement si l'utilisateur a l'autorité GESTION_PERIMES ou ADMIN
// Données : endpoint /api/lot/upcoming-alerts
// Apparence : card rouge si lots périmés non traités, orange si < 30j, verte sinon
```

#### Points d'entrée contextuels (deep links)

| Depuis | Vers | Paramètre |
|---|---|---|
| Fiche produit (`/produits/:id`) | `/gestion-peremption?produitId=X` | `produitId` |
| Bon de commande (`/commande/:id`) | `/gestion-peremption?fournisseurId=X` | `fournisseurId` |
| Rapport pertes (`/reports/stock`) | `/gestion-peremption?type=PERIME` | `type` |
| Dashboard widget | `/gestion-peremption` | (aucun, vue par défaut) |

Ces deep links permettent à l'utilisateur de passer d'un module à l'autre **avec le contexte pré-rempli**, sans avoir à recopier les filtres manuellement.

---

## 11. Implémentation technique — Guide pas à pas pour le badge d'alerte

> Cette section traduit les recommandations de navigation (§9 et §10) en modifications de code concrètes, basées sur l'analyse de `navbar-item.model.ts`, `sidebar.component.ts` et `navigation.service.ts`.

### 11.1 Étape 1 — Étendre le modèle `NavItem`

**Fichier :** `src/main/webapp/app/layouts/navbar/navbar-item.model.ts`

```typescript
// ❌ Actuel — aucun support de badge
export interface NavItem {
  label: string;
  routerLink?: string;
  authorities?: string[];
  faIcon?: IconProp;
  children?: NavItem[];
  click?: () => void;
}

// ✅ Cible — ajout des champs badge
export interface NavItem {
  label: string;
  routerLink?: string;
  authorities?: string[];
  faIcon?: IconProp;
  children?: NavItem[];
  click?: () => void;
  /** Valeur numérique affichée dans le badge (0 ou undefined = badge masqué) */
  badge?: number;
  /** Couleur sémantique du badge Bootstrap : 'danger' | 'warning' | 'info' | 'success' */
  badgeSeverity?: 'danger' | 'warning' | 'info' | 'success';
}
```

---

### 11.2 Étape 2 — Créer le service `PeremptionAlertService`

**Fichier :** `src/main/webapp/app/shared/services/peremption-alert.service.ts` _(nouveau)_

```typescript
import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';

export interface PeremptionAlerts {
  expired: number;   // Lots déjà périmés (ProductsToDestroy non détruits + LotPerimes.days < 0)
  days7: number;     // Lots expirant dans 7j
  days30: number;    // Lots expirant dans 30j
}

@Injectable({ providedIn: 'root' })
export class PeremptionAlertService {
  private readonly http = inject(HttpClient);
  private readonly _urgentCount = signal(0);

  /** Signal réactif lu par la sidebar et le dashboard */
  readonly urgentCount = this._urgentCount.asReadonly();

  /** Appeler au démarrage et toutes les 15 minutes */
  fetchAlerts(): void {
    this.http.get<PeremptionAlerts>(SERVER_API_URL + 'api/lot/alerts').subscribe({
      next: data => this._urgentCount.set(data.expired + data.days7),
      error: () => this._urgentCount.set(0),
    });
  }
}
```

> **Backend requis :** Endpoint `GET /api/lot/alerts` → `{ expired: n, days30: n, days7: n }`  
> Implémentation suggérée : requêtes `COUNT` sur `Lot` (périmés non retirés) + `products_to_destroy` (non détruits).  
> **Note :** La traçabilité des destructions existe déjà via `InventoryTransaction` (type `RETRAIT_PERIME`).  
> Cet endpoint ne crée rien de nouveau — il agrège des compteurs depuis les tables existantes.

---

### 11.3 Étape 3 — Brancher le badge dans `navigation.service.ts`

**Fichier :** `src/main/webapp/app/core/config/navigation.service.ts`

```typescript
// Injection du nouveau service
private readonly peremptionAlertService = inject(PeremptionAlertService);

// Dans buildNavItems(), section "Gestion Stock" :
allItems.push({
  label: this.translateLabel('menuGestionStock'),
  faIcon: faTruckFast,
  badge: this.peremptionAlertService.urgentCount(),      // ← signal réactif
  badgeSeverity: this.peremptionAlertService.urgentCount() > 0 ? 'danger' : undefined,
  authorities: [...],
  children: [
    // ...
    {
      label: this.translateFullLabel('gestionPerimes.title'),
      routerLink: '/gestion-peremption',
      faIcon: faCalendarTimes,
      badge: this.peremptionAlertService.urgentCount(),  // ← même signal
      badgeSeverity: 'danger',
    },
    // ...
  ],
});
```

> **Note :** `sidebar.component.ts` reconstruit déjà les `navItems` via un `effect()` réactif (ligne 52–54). Le signal sera donc recalculé automatiquement à chaque changement du compteur.

---

### 11.4 Étape 4 — Afficher le badge dans les deux layouts de navigation

> Le badge doit être cohérent dans **les deux layouts** : sidebar verticale et navbar horizontale, qui partagent les mêmes `navItems`.

#### 11.4a — Sidebar (`sidebar.component.html`)

**Fichier :** `src/main/webapp/app/layouts/sidebar/sidebar.component.html`

```html
<!-- Item simple avec badge -->
<span class="nav-icon">
  <fa-icon [icon]="item.faIcon"></fa-icon>
  @if (item.badge > 0) {
    <span class="nav-badge badge bg-{{ item.badgeSeverity ?? 'danger' }}">
      {{ item.badge > 99 ? '99+' : item.badge }}
    </span>
  }
</span>

<!-- Item parent avec badge -->
<span class="nav-label">{{ item.label }}</span>
@if (item.badge > 0) {
  <span class="ms-auto badge bg-{{ item.badgeSeverity ?? 'danger' }} nav-badge-label">
    {{ item.badge > 99 ? '99+' : item.badge }}
  </span>
}

<!-- Sous-menu avec badge -->
<span class="submenu-label">{{ child.label }}</span>
@if (child.badge > 0) {
  <span class="ms-auto badge bg-{{ child.badgeSeverity ?? 'danger' }}">
    {{ child.badge > 99 ? '99+' : child.badge }}
  </span>
}
```

CSS à ajouter dans `sidebar.component.scss` :
```scss
.nav-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 0.6rem;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  line-height: 16px;
  border-radius: 8px;
}
.nav-badge-label { font-size: 0.65rem; }
.nav-icon { position: relative; }
```

---

#### 11.4b — Navbar horizontale (`navbar.component.html`) ✅ IMPLÉMENTÉ

**Fichier :** `src/main/webapp/app/layouts/navbar/navbar.component.html`

Trois points de rendu ajoutés (même structure que la sidebar, alignement inline) :

```html
<!-- Parent dropdown (ex : "Gestion Stock 🔴 12") -->
<span class="d-flex align-items-center gap-1">
  <fa-icon [icon]="item.faIcon"></fa-icon>
  <span>{{ item.label }}</span>
  @if (item.badge > 0) {
    <span class="badge rounded-pill bg-{{ item.badgeSeverity ?? 'danger' }} navbar-badge">
      {{ item.badge > 99 ? '99+' : item.badge }}
    </span>
  }
</span>

<!-- Dropdown item enfant (ex : "Gestion Péremptions 🔴 12") -->
<a class="dropdown-item d-flex align-items-center justify-content-between" ...>
  <span><fa-icon .../><span>{{ child.label }}</span></span>
  @if (child.badge > 0) {
    <span class="badge rounded-pill bg-{{ child.badgeSeverity ?? 'danger' }} ms-2">
      {{ child.badge > 99 ? '99+' : child.badge }}
    </span>
  }
</a>

<!-- Item simple sans enfants -->
<span class="d-flex align-items-center gap-1">
  ...
  @if (item.badge > 0) {
    <span class="badge rounded-pill bg-{{ item.badgeSeverity ?? 'danger' }} navbar-badge">
      {{ item.badge > 99 ? '99+' : item.badge }}
    </span>
  }
</span>
```

CSS ajouté dans `navbar.component.scss` :
```scss
.navbar-badge {
  font-size: 0.6rem;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  line-height: 16px;
  vertical-align: middle;
}
.dropdown-item .badge {
  font-size: 0.65rem;
  flex-shrink: 0;
}
```

---

### 11.5 Étape 5 — Déclencher le fetch au démarrage

**Fichier :** `src/main/webapp/app/layouts/main/main.component.ts`

```typescript
// Appeler fetchAlerts() une fois authentifié, puis toutes les 15 min
private readonly peremptionAlertService = inject(PeremptionAlertService);

ngOnInit(): void {
  // ...code existant...
  
  // Fetch initial + polling toutes les 15 minutes
  this.peremptionAlertService.fetchAlerts();
  setInterval(() => this.peremptionAlertService.fetchAlerts(), 15 * 60 * 1000);
}
```

---

### 11.6 Résumé des fichiers à modifier

| Fichier | Modification | Statut | Effort |
|---|---|:---:|:---:|
| `navbar-item.model.ts` | Ajout champs `badge` / `badgeSeverity` | ⬜ À faire | 5 min |
| `peremption-alert.service.ts` | Nouveau fichier — signal + HTTP | ⬜ À faire | 30 min |
| `navigation.service.ts` | Injection service + badge sur 2 items | ⬜ À faire | 15 min |
| `navbar.component.html` | Rendu badge sur 3 blocs (parent, enfant, simple) | ✅ Fait | — |
| `navbar.component.scss` | Styles `.navbar-badge` et `.dropdown-item .badge` | ✅ Fait | — |
| `sidebar.component.html` | Rendu badge sur 3 blocs | ⬜ À faire | 30 min |
| `sidebar.component.scss` | Styles `.nav-badge`, `.nav-badge-label`, `.nav-icon` | ⬜ À faire | 15 min |
| `main.component.ts` | Fetch au démarrage + polling 15 min | ⬜ À faire | 10 min |
| **Backend** | `GET /api/lot/alerts` endpoint | ⬜ À faire | 1h |
| **Total restant** | | | **~2h** |

---

## Annexe — Références réglementaires

- **Code de la Santé Publique** — Art. R. 4235-12 : obligation de destruction des médicaments périmés par le pharmacien titulaire
- **ANSM** — Bonnes Pratiques de Pharmacie (BPP) : traçabilité des lots et gestion des retours
- **Règlement européen 2016/161** (sérialisation) : traçabilité des médicaments à data matrix
- **Ordonnance n°2017-49** : dématérialisation des ordonnances et traçabilité

---

*Document rédigé le 2026-04-02 — à intégrer dans le backlog Pharma-Smart*

