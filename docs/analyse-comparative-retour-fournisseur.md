# Analyse comparative — Module Retour Fournisseur

**Pharma-Smart vs. logiciels de gestion d'officine du marché français**

> Date d'analyse : mars 2026
> Module analysé : `features/commande/feature/retour-fournisseur`

---

## 1. Contexte & périmètre fonctionnel analysé

Le module **Retour Fournisseur** de Pharma-Smart gère l'ensemble du processus de retour de
marchandises vers un grossiste ou un laboratoire, depuis la création du bon de retour jusqu'au
suivi de la réponse du fournisseur. Il est composé de :

| Composant | Rôle |
|---|---|
| `AppRetourFournisseurComponent` | Liste paginée des bons de retour avec filtres et expansion de lignes |
| `SupplierReturnsComponent` | Formulaire de création d'un bon de retour (sélection commande + produit + motif + lots) |
| `LotSelectionDialogComponent` | Sélection des lots à retourner en mode modale |
| `InlineLotSelectionComponent` | Sélection des lots à retourner en mode inline (intégré dans le formulaire) |

**Logiciels de référence pour la comparaison :**

- **Winpharma** (Alliadis) — leader historique en officine française
- **Lgpi / Pharmagest** (iSPharma) — deuxième acteur du marché
- **Allwin (SmartRx)** — solution cloud nouvelle génération
- **Caduciel / PharmaSuccess** — solutions régionales PME
- **Isipharm (CGPS)** — solution dédiée aux groupements indépendants

---

## 2. Fonctionnalités couvertes : tableau comparatif

| Fonctionnalité | Pharma-Smart | Winpharma | Lgpi/Pharmagest | Allwin/Cloud | Caduciel |
|---|:---:|:---:|:---:|:---:|:---:|
| Création d'un bon de retour manuel | ✅ | ✅ | ✅ | ✅ | ✅ |
| Retour rattaché à une réception de commande | ✅ | ✅ | ✅ | ✅ | ⚠️ partiel |
| Motifs de retour paramétrables | ✅ | ✅ | ✅ | ✅ | ⚠️ liste fixe |
| Gestion des lots lors du retour | ✅ double mode (dialog/inline) | ✅ | ✅ | ✅ | ⚠️ partiel |
| Auto-répartition FEFO sur les lots | ✅ (calcul côté client) | ⚠️ manuelle | ✅ | ✅ | ❌ |
| Cumul automatique si ligne dupliquée | ✅ | ⚠️ erreur | ⚠️ | ⚠️ | ❌ |
| Saisie de la réponse fournisseur (accepté/refusé) | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| Workflow statuts (En attente → Clôturé) | ✅ `VALIDATED → CLOSED` | ✅ | ✅ | ✅ | ⚠️ 2 statuts |
| Filtre par statut dans la liste | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| Filtres par date dans la liste | ✅ (UI) | ✅ | ✅ | ✅ | ⚠️ |
| Génération d'un bon de retour PDF | ❌ | ✅ | ✅ | ✅ | ✅ |
| Envoi EDI PharmaMl/Pharmagest | ⚠️ backend uniquement | ✅ intégré | ✅ natif | ✅ | ❌ |
| Modification d'un retour créé | ❌ | ✅ | ✅ | ✅ | ⚠️ |
| Statistiques / taux d'acceptation | ❌ | ✅ | ✅ | ✅ | ❌ |
| Champ commentaire libre | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| Affichage quantité reçue lors de la saisie | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| Validation côté client (qté ≤ qté reçue) | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| Deux modes de saisie lots (dialog / inline) | ✅ unique | ❌ | ❌ | ❌ | ❌ |

**Légende :** ✅ implémenté · ⚠️ partiel ou configurable · ❌ absent

---

## 3. Points forts de l'implémentation Pharma-Smart

### 3.1 Double mode de sélection des lots — originalité fonctionnelle

Pharma-Smart est la seule solution analysée à proposer **deux modes interchangeables** pour la
sélection des lots lors d'un retour, basculables en cours de session via le bouton
`Mode: Dialog / Mode: Inline` :

- **Mode Dialog** (`LotSelectionDialogComponent`) : modale PrimeNG avec tableau listant les lots,
  pré-remplie automatiquement par ordre FEFO. Adapté à une consultation détaillée.
- **Mode Inline** (`InlineLotSelectionComponent`) : interface expandable intégrée dans la page,
  avec cartes par lot, incréments +/−, aperçu dynamique du total. Adapté à la saisie rapide sur
  écran tactile ou grand écran.

Les solutions concurrentes n'offrent qu'un seul mode (modale chez Winpharma et Lgpi, inline chez
Allwin), sans possibilité de changer en cours d'utilisation.

### 3.2 Auto-répartition FEFO côté client sur les lots

Les deux composants de sélection de lot implémentent l'algorithme **FEFO** (First Expired, First
Out) côté frontend :
- Tri des lots par `expiryDate` croissante
- Affectation automatique en cascade jusqu'à épuisement de la quantité demandée
- Bouton "Auto-répartir (FEFO)" dans le mode inline pour déclencher manuellement

Le mode dialog (`LotSelectionDialogComponent`) applique ce calcul dès l'ouverture (pré-remplissage
automatique). C'est une ergonomie supérieure à Winpharma qui laisse le choix de lot entièrement
manuel.

### 3.3 Cumul automatique des doublons

Si l'utilisateur ajoute deux fois le même produit/lot/motif dans le bon de retour, la méthode
`createReturnItem` **détecte le doublon** et additionne les quantités au lieu de créer une ligne
dupliquée, en respectant le plafond de la quantité reçue. Chez Winpharma et Lgpi cette situation
génère une erreur ou une ligne en double que le pharmacien doit corriger manuellement.

### 3.4 Filtrage réactif avec debounce sur l'autocomplétion

Le composant `SupplierReturnsComponent` utilise deux `Subject` + `debounceTime(300ms)` pour les
recherches commande et produit, évitant les appels API redondants à chaque frappe. La recherche
de commandes appelle dynamiquement `DeliveryService.queryWithoutDetail` sans charger le détail
des lignes en amont. C'est un pattern performant que les solutions client lourd (Winpharma) ne
reproduisent pas du tout.

### 3.5 Statut visuel granulaire dans la liste

La liste des retours affiche un tag coloré par statut (`VALIDATED → info`, `PROCESSING →
secondary`, `CLOSED → success`) avec l'action conditionnelle `Saisir la réponse fournisseur`
uniquement disponible pour les retours en statut `VALIDATED`. Les retours clôturés affichent une
icône check sans bouton d'action, ce qui évite les modifications involontaires.

---

## 4. Points d'amélioration identifiés

### 4.1 Absence de génération PDF du bon de retour

C'est le manque fonctionnel le plus visible par rapport à la concurrence. **Aucun endpoint ni
service de génération PDF** n'existe pour les bons de retour (contrairement aux bons de commande
et aux réceptions qui ont leurs propres `PdfReportService`). Le pharmacien ne peut ni imprimer
ni archiver le bon de retour.

**Impact :** en pratique, les grossistes exigent un document de retour accompagnant la
marchandise. Cette fonctionnalité est indispensable pour une utilisation en production.

**Ce que font les concurrents :** tous les logiciels analysés génèrent un bon de retour imprimable
(PDF ou étiquette code-barres) depuis la vue de détail du retour.

### 4.2 Filtres du backend non branchés dans la requête

Le contrôleur `RetourBonResource.getAllRetourBons` accepte les paramètres `statut`, `dtStart` et
`dtEnd`, mais l'implémentation appelle `retourBonService.findAll(pageable)` **sans passer ces
filtres**. Côté frontend, `AppRetourFournisseurComponent` envoie bien `dtStart`, `dtEnd` et le
statut, mais ils sont ignorés par le backend.

```java
// RetourBonResource.java:96 — les paramètres statut/dtStart/dtEnd ne sont pas transmis
Page<RetourBonDTO> page = retourBonService.findAll(pageable);
```

**Impact :** la liste affiche toujours tous les retours sans filtrage effectif, rendant la
fonctionnalité de filtre par statut et par date non opérationnelle.

### 4.3 Pas de modification d'un retour créé

Une fois un `RetourBon` créé, il n'existe ni endpoint `PUT /retour-bons/{id}` ni composant
d'édition. Le pharmacien ne peut corriger une erreur de quantité qu'en supprimant et recréant
le retour — ce qui n'est pas non plus possible (pas d'endpoint DELETE).

**Ce que font les concurrents :** Winpharma et Lgpi permettent de modifier un retour tant qu'il
n'a pas été clôturé (statut `VALIDATED` = modifiable, `CLOSED` = verrouillé).

### 4.4 Intégration EDI PharmaMl non finalisée côté retour

`PharmaMlResource` expose `/api/pharmaml/retour/{commandeRef}/{orderId}` qui interroge les lignes
de retour depuis le portail grossiste. Mais **aucun bouton ni flux** dans le module
`retour-fournisseur` ne permet d'envoyer un retour via EDI ni de récupérer la réponse
automatiquement depuis PharmaMl. La liaison entre `RetourBon` et `PharmaMlEnvoi` n'est pas
implémentée.

**Ce que font les concurrents :** Lgpi/Pharmagest et Allwin transmettent le bon de retour
directement via EDI au grossiste (OCP, CERP, Alliance Healthcare) avec accusé de réception
automatique et mise à jour du statut.

### 4.5 Pas de tableau de bord / statistiques retours

Aucun KPI n'est disponible :
- taux d'acceptation par fournisseur
- montant des retours sur une période
- motifs de retour les plus fréquents
- délai moyen de traitement

**Ce que font les concurrents :** Winpharma et Lgpi proposent ces métriques dans leurs modules
de reporting fournisseur.

### 4.6 Workflow statut incomplet — état `PROCESSING` non utilisé

L'énumération `RetourBonStatut` comporte trois états : `VALIDATED`, `PROCESSING`, `CLOSED`.
L'UI ne propose que `VALIDATED` et `CLOSED` dans les filtres, et la transition vers `PROCESSING`
n'est pas déclenchée par aucune action. Il s'agit probablement d'un état intermédiaire prévu
(en cours de traitement par le grossiste) mais non encore implémenté.

### 4.7 Recherche textuelle dans la liste non reliée au backend

Dans `AppRetourFournisseurComponent`, le champ de recherche construit bien `query.search` mais
`RetourBonResource.getAllRetourBons` ne déclare pas ce paramètre `search` et l'implémentation
backend ne filtre pas sur la référence ou le nom fournisseur.

---

## 5. Comparaison architecturale

| Critère | Pharma-Smart | Solutions du marché |
|---|---|---|
| **Modèle de données** | `RetourBon` → `RetourBonItem` (1-N) + `ReponseRetourBon` | Identique chez Winpharma/Lgpi |
| **Gestion des lots** | `ILot` avec `expiryDate`, `numLot`, `quantityReceived` | Identique, certains ajoutent N° série |
| **Statuts** | `VALIDATED / PROCESSING / CLOSED` | Winpharma : `CREE / ENVOYE / CLOTURE` |
| **Lien EDI** | `PharmaMlEnvoi` (backend prévu, non branché retour) | Natif chez Lgpi/Allwin |
| **Frontend** | Angular 20 signals, autocomplete debounce | React (Allwin), Angular 12-15 (Lgpi cloud), WPF (Winpharma) |
| **Double mode saisie lots** | ✅ unique sur le marché | ❌ tous |
| **API REST** | Spring Boot 4 RC1 | REST (Allwin), REST/SOAP (Winpharma legacy) |
| **PDF** | ❌ non implémenté retour | ✅ tous |

---

## 6. Synthèse

### Ce que Pharma-Smart fait aussi bien ou mieux que la concurrence

- Double mode sélection des lots (dialog/inline) — **fonctionnalité unique** sur le marché analysé
- FEFO automatique côté client avec pré-remplissage à l'ouverture
- Cumul intelligent des doublons lors de la saisie
- UX de saisie fluide avec focus automatique et debounce sur les autocompétions
- Architecture Angular 20 signals moderne (performances réactives supérieures aux solutions legacy)

### Ce que la concurrence fait que Pharma-Smart ne fait pas encore

| Manque | Priorité estimée | Effort |
|---|---|---|
| Génération PDF bon de retour | **Critique** (usage quotidien) | Moyen (service PDF existant) |
| Filtres backend effectifs (statut, dates, search) | **Élevée** (bug existant) | Faible (fix service) |
| Modification d'un retour créé | **Élevée** | Moyen |
| Envoi EDI PharmaMl du retour | **Moyenne** | Élevé |
| Statistiques / KPI retours | **Faible** | Élevé |
| Transition statut `PROCESSING` | **Faible** | Faible |

### Positionnement global

Le module `retour-fournisseur` de Pharma-Smart offre une **base fonctionnelle solide** avec
une ergonomie de saisie supérieure à la concurrence (double mode lots, FEFO automatique,
cumul doublons). Toutefois, deux manques critiques le rendent **non production-ready** pour une
officine standard : l'absence de bon de retour PDF et le dysfonctionnement des filtres backend.
Une fois ces points corrigés, le module atteindra le niveau mid-market des solutions Lgpi/
Pharmagest. L'intégration EDI PharmaMl le hissera au niveau des solutions premium.
