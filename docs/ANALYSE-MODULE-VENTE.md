# Analyse Comparative — Module de Vente Pharma-Smart

> **Date :** Avril 2026  
> **Scope :** `src/main/webapp/app/features/sales/`  
> **Référentiels comparés :** Winpharma, LGPI Observia, Caducée, PharmaSys, Pharmavitale, Alliadis (Sil-Expert)

---

## 1. Inventaire des Fonctionnalités

### 1.1 Types de vente supportés

| Type | Route | Description |
|------|-------|-------------|
| **Comptant (VNO)** | `/sales` → tab `comptant` | Vente sans ordonnance, paiement immédiat |
| **Assurance** | `/sales` → tab `assurance` | Tiers payant RO + RC, gestion des bons |
| **Carnet** | `/sales` → tab `carnet` | Vente à crédit sur carnet client |
| **Pré-vente** | `/sales/prevente` | Réservation sans encaissement |
| **Proforma (Devis)** | `/sales/devis` | Devis/facture proforma |
| **Vente dépôt** | `/sales/vente-depot` | Vente depuis un dépôt externe |

### 1.2 Composants UI identifiés

```
sales/
├── feature/
│   ├── sales-home/              ← POS Container (tabs Comptant/Assurance/Carnet)
│   ├── sale-creation/           ← Flux vente comptant
│   ├── sale-assurance/          ← Flux vente tiers payant
│   ├── sale-carnet/             ← Flux vente carnet
│   ├── sale-devis/              ← Flux proforma
│   ├── vente-depot/             ← Flux dépôt
│   ├── sales-journal/           ← Historique avec expand row
│   ├── sales-en-cours/          ← Ventes ACTIVE en attente
│   ├── sales-management-home/   ← Hub de gestion (journal/encours/préventes/proformas)
│   ├── presale-home/            ← Wrapper pré-ventes
│   ├── presale-list/            ← Liste des pré-ventes
│   └── devis-list/              ← Liste des proformas
│
└── ui/ (composants partagés)
    ├── product-search-section/  ← Autocomplete + scan code-barres
    ├── product-list/            ← Tableau lignes de vente (inline editing)
    ├── sale-summary/            ← Panel montants (Total/Remise/Assurance/À payer)
    ├── sale-actions/            ← Boutons Valider/EnAttente/Annuler
    ├── payment-mode/            ← Saisie multi-modes de règlement
    ├── insurance-data-bar/      ← Barre assuré + ayant droit + tiers payants
    ├── customer-overlay-panel/  ← Popover recherche/sélection client
    ├── pending-sales-list/      ← Drawer ventes en attente
    └── sale-type-selector/      ← Sélecteur type de vente
```

### 1.3 Fonctionnalités techniques présentes

- ✅ Scan code-barres en temps réel (file d'attente multi-scan)
- ✅ Recherche produit autocomplete (min 2 caractères)
- ✅ Gestion tiers payants multi (RO + RC1 + RC2…)
- ✅ Ayant droit
- ✅ Remise globale sur vente (via popover)
- ✅ Remise par ligne (discount authorized)
- ✅ Modification prix unitaire (rôle requis)
- ✅ Vente différée (encaissement différé)
- ✅ Avoir / livraison partielle (quantité servie ≠ demandée)
- ✅ Multi-modes de règlement (espèces + chèque + virement…)
- ✅ Calcul de monnaie en temps réel
- ✅ Afficheur client externe (CustomerDisplayService)
- ✅ Impression ticket thermique
- ✅ Impression facture
- ✅ Ventes en attente (drawer latéral, badge compteur)
- ✅ Plafond de vente client (indicateur)
- ✅ Contrôle d'autorisation (suppr. ligne, remise, prix → workflow manager)
- ✅ Raccourcis clavier (Alt+1/2/3, F11, F-keys contextuelles)
- ✅ Responsive mobile (icon-only buttons sur petits écrans)
- ✅ Caisse (vérification ouverture obligatoire avant clôture)
- ✅ Sélection vendeur / caissier
- ✅ Édition vente ASSURANCE/CARNET clôturée
- ✅ Annulation vente (avec confirmation)
- ✅ Journal ventes (filtrage date/type/vendeur/heure)
- ✅ Vue expand row (détail financier + lignes + paiements)

---

## 2. Analyse Comparative avec les Logiciels de Référence

### 2.1 Grille comparative

| Fonctionnalité | Pharma-Smart | Winpharma | LGPI Observia | Caducée | PharmaSys |
|---|:---:|:---:|:---:|:---:|:---:|
| **Point de vente multi-type** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Scan code-barres** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Tiers payant multi** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Pré-vente / réservation** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Proforma** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Ordonnance numérique** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Alertes interactions médicamenteuses** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Alertes doublons thérapeutiques** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Fiche produit contextuelle (infobulle)** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Vente par substitution (générique)** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Gestion stupéfiants (ordonnance sécurisée)** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Historique client lors de la vente** | ❌ | ✅ | ✅ | ✅ | ❌ |
| **Programme fidélité / points** | ❌ | ✅ | ✅ | ❌ | ❌ |
| **Retour / échange depuis la caisse** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Vente par lot / conditionnement** | ❌ | ✅ | ✅ | ❌ | ✅ |
| **Affichage numéro de lot en vente** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Rappel de rappels produits (alerte lot)** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Vente sans stock (force stock)** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Multi-modes règlement** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Vente différée** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Afficheur client externe** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Impression ticket thermique** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Raccourcis clavier** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Numéro de lot tracé en vente** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Conseil associé / fiche conseil** | ❌ | ✅ | ✅ | ❌ | ❌ |
| **Envoi ordonnance mobile (app patient)** | ❌ | ✅ | ✅ | ❌ | ❌ |
| **Plan de prise** | ❌ | ✅ | ✅ | ✅ | ❌ |
| **Décompte stock en temps réel affiché** | ⚠️ masqué | ✅ | ✅ | ✅ | ✅ |

> ✅ Présent &nbsp; ❌ Absent &nbsp; ⚠️ Partiel/caché

---

## 3. Axes d'Amélioration Prioritaires (Métier)

### 🔴 Critiques — Conformité réglementaire

#### 3.1 Traçabilité des lots en vente
**Manque :** Aucun numéro de lot/péremption affiché ou saisi lors de la vente.  
**Impact :** Non-conformité aux obligations de traçabilité (ANSM, sérialisation 2D-DATA-MATRIX pour médicaments remboursables).  
**Recommandation :** Afficher le lot et la date de péremption dans la ligne de vente ; alerter si le produit fait l'objet d'un rappel.

#### 3.2 Stupéfiants et ordonnances sécurisées
**Manque :** Pas de workflow spécifique pour les produits classifiés (tableau A/B, stupéfiants, médicaments à prescription restreinte).  
**Impact :** Risque légal. Chaque délivrance de stupéfiants nécessite un enregistrement registre.  
**Recommandation :** Détecter à l'ajout d'une ligne si le produit est classifié ; ouvrir un dialogue de saisie obligatoire (n° ordonnance, prescripteur, date).

#### 3.3 Alertes interactions et doublons thérapeutiques
**Manque :** Aucune alerte lors de l'ajout d'un médicament.  
**Impact :** Risque patient. Les logiciels de référence (Winpharma, LGPI) intègrent des bases de données d'interactions (Thériaque, Vidal).  
**Recommandation :** Intégrer une API d'interactions (ex: base Thériaque ou équivalent selon contexte réglementaire local).

---

### 🟠 Importants — Productivité pharmacien

#### 3.4 Affichage stock dans la liste des produits
**Manque :** Le code de stock est commenté dans `product-search.component.html` (lignes 32–44).  
**Impact :** Le préparateur ne voit pas le stock disponible dans la liste de la vente en cours.  
**Recommandation :** Réactiver l'affichage stock dans le dropdown, ajouter une colonne "Stock" dans `product-list`.

#### 3.5 Historique client lors de la vente
**Manque :** Pas d'accès rapide à l'historique d'achat du client depuis le POS.  
**Impact :** Impossible de savoir si un client a déjà acheté ce médicament, ce qui est utile pour les renouvellements.  
**Recommandation :** Ajouter un bouton "Historique" dans le `customer-overlay-panel` qui ouvre un drawer avec les 10 dernières ventes.

#### 3.6 Substitution par générique
**Manque :** Pas de suggestion de générique lors de la saisie d'un princeps.  
**Impact :** Manque à gagner et non-respect de la politique de prescription en DCI.  
**Recommandation :** À l'ajout d'un princeps, proposer une notification/popover "Générique disponible".

#### 3.7 Retour / échange depuis la caisse
**Manque :** Pas de flux de retour rapide depuis le POS.  
**Impact :** Oblige à naviguer vers le journal, ce qui ralentit le flux caisse.  
**Recommandation :** Ajouter un bouton "Retour/Avoir" dans les actions de la caisse ou dans le drawer ventes en attente.

---

### 🟡 Améliorations — Expérience utilisateur

#### 3.8 Affichage des raccourcis clavier
**Manque :** Les raccourcis (Alt+1, F11…) sont invisibles pour l'utilisateur.  
**Recommandation :** Ajouter un bouton "?" ou un panel d'aide raccourcis (comme dans Winpharma, Caducée).

#### 3.9 Label "VNO" non explicite
**Constat :** Dans le journal, le type de vente comptant est affiché "VNO" (Vente Non Ordonnancée), terme non compris par tous.  
**Recommandation :** Remplacer par "Comptant" ou "Sans ordonnance" selon le contexte, avec une infobulle explicative.

#### 3.10 Fiche produit contextuelle
**Manque :** Aucun accès rapide à la fiche produit depuis la ligne de vente.  
**Recommandation :** Ajouter un bouton info sur chaque ligne de produit (popover avec DCI, forme, conditionnement, prix public, stock).

---

## 4. Analyse UX

### 4.1 Points forts UX

| Aspect | Évaluation | Commentaire |
|--------|-----------|-------------|
| **Architecture Container/Presentation** | ⭐⭐⭐⭐⭐ | Séparation claire, maintenable, testable |
| **Raccourcis clavier** | ⭐⭐⭐⭐ | Bonne couverture (Alt+1/2/3, F-keys, Enter pour valider) |
| **Scan code-barres** | ⭐⭐⭐⭐⭐ | File d'attente multi-scan, feedback audio |
| **Inline editing tableau** | ⭐⭐⭐⭐ | Édition quantité/prix directement dans la cellule |
| **Responsive mobile** | ⭐⭐⭐ | Adaptation icon-only sur petits écrans |
| **Feedback visuel (spinner)** | ⭐⭐⭐⭐ | Spinner global lors des sauvegardes |
| **Ventes en attente (drawer)** | ⭐⭐⭐⭐ | Drawer avec badge compteur — ergonomique |
| **Calcul monnaie en temps réel** | ⭐⭐⭐⭐ | Mise à jour instantanée du rendu monnaie |
| **Confirmation actions destructives** | ⭐⭐⭐⭐ | Dialogue de confirmation systématique |

---

### 4.2 Problèmes UX identifiés

#### P1 — Manque de feedback visuel sur le stock disponible
**Références UX :** Nielsen heuristic #1 — *Visibility of system status*  
**Constat :** Le stock est masqué dans l'autocomplete produit (code commenté). Le préparateur ne sait pas immédiatement si le produit est disponible.  
**Impact :** Erreurs, retours caisse, perte de temps.  
**Correction proposée :**
- Afficher le stock sous forme de badge coloré dans le dropdown :
  - 🟢 `> seuil` — disponible
  - 🟡 `< seuil` — stock bas
  - 🔴 `= 0` — rupture (avec option force stock)

#### P2 — Navigation entre types de vente peu intuitive
**Références UX :** Nielsen heuristic #6 — *Recognition rather than recall*  
**Constat :** La sidebar gauche contient les tabs "Comptant / Assurance / Carnet" mais n'est pas associée à une terminologie universelle en officine.  
**Impact :** Nouvel utilisateur perdu, risque de sélection du mauvais type.  
**Correction proposée :**
- Ajouter des sous-titres contextuels sous chaque tab
- Afficher une icône plus distincte (ex: 🏦 pour Assurance)
- Highlight visuel du tab actif plus prononcé

#### P3 — Section paiement toujours visible (même panier vide)
**Références UX :** Nielsen heuristic #8 — *Aesthetic and minimalist design*  
**Constat :** Les modes de règlement apparaissent dans le layout même avant qu'une vente soit créée.  
**Impact :** Encombrement visuel, confusion pour l'utilisateur.  
**Correction proposée :** Cacher `app-payment-mode` jusqu'à ce que `currentSale()` soit non-null et contienne au moins un article.

#### P4 — Résumé financier insuffisamment hiérarchisé
**Références UX :** Fitts's Law + Visual Hierarchy  
**Constat :** Dans `sale-summary.component.html`, le montant "à payer" n'est pas visuellement dominant par rapport aux autres lignes (total, TVA, remise).  
**Impact :** Le caissier doit "chercher" le montant à encaisser.  
**Correction proposée :**
- Taille de police ×2 pour le montant "à payer"
- Fond coloré contrasté (ex: vert pour un solde positif, rouge pour un avoir)
- Séparation visuelle claire avant "à payer"

#### P5 — Absence d'état vide informatif pour le panier
**Références UX :** Empty state UX pattern  
**Constat :** L'état vide du panier (`Panier vide / Ajoutez des produits...`) est correctement géré, mais manque d'une action directe (CTA : focus sur la recherche produit).  
**Correction proposée :** Ajouter un bouton "Commencer la vente" qui met le focus sur `app-product-search-section`.

#### P6 — Libellé "VNO" cryptique dans le journal
**Références UX :** *Plain language* guideline  
**Constat :** "VNO" (Vente Non Ordonnancée) apparaît comme type de vente dans `sales-journal`.  
**Correction proposée :** Remplacer par "Comptant" ou "Sans ordonnance" avec tooltip explicatif.

#### P7 — Drawer ventes en attente désactivé si vente en cours
**Références UX :** Nielsen heuristic #5 — *Error prevention*  
**Constat :** `[disabled]="salesFacade.currentSale() !== null"` — Le bouton "En attente" est grisé si une vente est en cours, ce qui empêche de voir les autres ventes en attente.  
**Impact :** Le pharmacien ne peut pas consulter les ventes en attente sans perdre sa vente courante.  
**Correction proposée :** Permettre l'ouverture en lecture seule du drawer tout en gardant la vente courante active.

#### P8 — Absence d'indicateur de progression dans le flux de vente
**Références UX :** Progress indication pattern  
**Constat :** Aucun indicateur visuel du step courant (1. Produits → 2. Client → 3. Paiement → 4. Validation).  
**Correction proposée :** Ajouter un mini stepper horizontal dans le header de la vente, ou des micro-indicateurs (badges numériques sur les sections).

#### P9 — Inline editing de cellule non guidé
**Références UX :** *Affordance* principle  
**Constat :** Les colonnes QTÉ.D, QTÉ.S, PU sont éditables au clic via `pEditableColumn`, mais aucune affordance visuelle (icône crayon, fond de couleur différent) n'indique à l'utilisateur que ces cellules sont modifiables.  
**Correction proposée :** Ajouter un curseur `pointer` + hover highlight sur les cellules éditables.

#### P10 — Gestion des erreurs non contextualisée
**Références UX :** Nielsen heuristic #9 — *Help users recognize, diagnose, and recover from errors*  
**Constat :** Les erreurs sont gérées via `NotificationService` (toast en haut) mais sans lien direct vers l'action corrective.  
**Correction proposée :** Pour les erreurs critiques (ex: "Caisse non ouverte"), proposer un bouton d'action directe dans le toast ("Ouvrir la caisse").

---

## 5. Tableau de Priorisation UX

| # | Problème | Impact | Effort | Priorité | Statut |
|---|----------|--------|--------|----------|--------|
| P4 | Montant à payer non dominant visuellement | 🔴 Élevé | 🟢 Faible | **P0** | ✅ Implémenté |
| P1 | Indicateur stock masqué | 🔴 Élevé | 🟡 Moyen | **P0** | ✅ Implémenté |
| P6 | Libellé "VNO" cryptique | 🟡 Moyen | 🟢 Faible | **P1** | ✅ Implémenté |
| P9 | Cellules éditables sans affordance | 🟡 Moyen | 🟢 Faible | **P1** | ✅ Implémenté |
| P7 | Drawer bloqué si vente en cours | 🟡 Moyen | 🟡 Moyen | **P2** | ✅ Implémenté |
| P2 | Navigation tabs peu intuitive | 🟠 Moyen | 🟡 Moyen | **P2** | ✅ Implémenté |
| ~~P5~~ | ~~CTA absent dans état vide panier~~ | ~~🟡 Moyen~~ | ~~🟢 Faible~~ | ~~**P1**~~ | 🚫 Décommissionné |
| ~~P3~~ | ~~Section paiement visible panier vide~~ | ~~🟡 Moyen~~ | ~~🟢 Faible~~ | ~~**P1**~~ | 🚫 Décommissionné |
| ~~P8~~ | ~~Absence de stepper visuel~~ | ~~🟠 Moyen~~ | ~~🟡 Moyen~~ | ~~**P2**~~ | 🚫 Décommissionné |
| ~~P10~~ | ~~Erreurs sans action corrective~~ | ~~🟠 Moyen~~ | ~~🟠 Élevé~~ | ~~**P3**~~ | 🚫 Décommissionné |

---

## 6. Fonctionnalités Manquantes vs Référentiels — Backlog Suggéré

### Backlog Haute Valeur Métier

```
EPIC: Sécurité Patient
├── FEAT-01: Alertes interactions médicamenteuses (intégration Thériaque/Vidal)
├── FEAT-02: Détection doublons thérapeutiques dans le panier
├── FEAT-03: Workflow ordonnances sécurisées (stupéfiants, tableau A/B)
└── FEAT-04: Alertes rappel de lots en vente

EPIC: Traçabilité
├── FEAT-05: Saisie/affichage numéro de lot + péremption par ligne de vente
└── FEAT-06: Scan Data-Matrix sérialisation (Directive 2011/62/UE)

EPIC: Productivité Caisse
├── FEAT-07: Substitution générique inline (princeps → générique)
├── FEAT-08: Historique achat client en un clic depuis le POS
├── FEAT-09: Retour/échange rapide depuis l'interface de vente
├── FEAT-10: Réactivation stock visible dans autocomplete
└── FEAT-11: Fiche produit popup (DCI, forme, conditionnement, interactions)

EPIC: UX Quick Wins
├── FEAT-12: Montant "à payer" visuellement dominant
├── FEAT-13: Affordance cellules éditables (cursor + hover)
├── FEAT-14: Badge stock coloré dans dropdown produit
├── FEAT-15: Raccourcis clavier visibles (panel aide)
└── FEAT-16: Remplacer "VNO" par libellé métier compréhensible
```

---

## 7. Points Forts de l'Architecture Technique

L'architecture du module de vente est globalement **moderne et bien structurée** :

| Aspect | Qualité | Détail |
|--------|---------|--------|
| **Container/Presentation** | ⭐⭐⭐⭐⭐ | Séparation stricte façade ↔ UI |
| **Signal-based state** | ⭐⭐⭐⭐⭐ | Angular Signals + computed, pas de BehaviorSubject |
| **Mixins réutilisables** | ⭐⭐⭐⭐⭐ | `createProductHandling`, `createPaymentHandling`, etc. |
| **SalesFacade** | ⭐⭐⭐⭐⭐ | Centralise tout le state, un seul point d'accès |
| **Scan queue** | ⭐⭐⭐⭐⭐ | File d'attente pour multi-scan simultané |
| **Raccourcis clavier** | ⭐⭐⭐⭐ | `createKeyboardShortcuts` mixin, centralisé |
| **Autorisation granulaire** | ⭐⭐⭐⭐⭐ | Workflow manager pour remise/suppression/prix |
| **Gestion avoir** | ⭐⭐⭐⭐ | Détection automatique qté servie ≠ qté demandée |
| **CustomerDisplay** | ⭐⭐⭐⭐ | Service dédié pour afficheur externe |
| **RemiseCache** | ⭐⭐⭐⭐ | Cache des remises disponibles |

---

## 8. Conclusion

Le module de vente de Pharma-Smart dispose d'une **base solide** avec les flux métier essentiels couverts (comptant, tiers payant, carnet, prévente, proforma, dépôt). L'architecture Angular est moderne et maintenable.

Les **gaps critiques** concernent principalement la **sécurité patient** (interactions médicamenteuses, stupéfiants, traçabilité des lots) et la **productivité caisse** (stock visible, historique client, retour rapide).

Les **quick wins UX** (stock visible, montant à payer dominant, affordances cellules éditables, libellé "VNO") peuvent être implémentés rapidement avec un impact fort sur l'expérience quotidienne.

---

*Analyse réalisée sur la base du code source Angular du module `features/sales` — Pharma-Smart v1.x*

