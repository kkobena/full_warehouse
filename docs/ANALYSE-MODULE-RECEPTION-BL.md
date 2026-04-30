# Analyse comparative — Module Réception de Bon de Livraison

> **Date :** 28 avril 2026  
> **Périmètre :** `features/commande/feature/commande-received/` + `sequential/`  
> **Objectif :** Identifier les forces, les lacunes UX/données, les axes d'amélioration et les
> bonnes pratiques scanning observées dans les WMS/PMS du marché.

---

## 1. Cartographie du module actuel

| Composant | Rôle | Mode |
|---|---|---|
| `commande-received.component` | Conteneur principal, orchestrateur | Les deux |
| `reception-sequential.component` | Saisie ligne par ligne (2 étapes : qté → lot) | Séquentiel |
| `reception-finalize-modal.component` | Récapitulatif avant clôture | Séquentiel |
| `commande-received-actions.component` | Renderer AG Grid — actions par ligne | Grille |
| `commande-received-statut.component` | Renderer AG Grid — badge statut | Grille |
| `reception-concordance.component` | Panneau latéral : anomalies & montants | Grille uniquement |
| `reception-help.component` | Aide contextuelle (modal) | Les deux |

### 1.1 Flux de saisie actuel

```
Ouvrir BL
  ↓
[Mode séquentiel] ── Ligne n → Étape 1 (Qté + UG) → Étape 2 (Lot)
                              ↓                          ↓
                        goNext()              lotsComplete → goNext()
                              ↓
                    Toutes lignes → ReceptionFinalizeModal
                              ↓
                    checkPriceVariation → checkPutaway → finalizeSaisieEntreeStock
                              ↓
                    checkReliquat → printEtiquette

[Mode grille]  ── AG Grid éditable (inline qty / UG / CIP)
               ── LotInlineEditorComponent (full-width row expand)
               ── Bouton "Valider" → même flux finalize
```

---

## 2. Ce qui est bien fait ✅

### 2.1 Architecture & UX
- **Double mode** (séquentiel + grille) persisté en `localStorage` → l'opérateur choisit son style
  de travail.
- **Navigation clavier complète** : F8/F9 (ligne préc/suiv), F12 (valider), Tab entre champs,
  Entrée pour confirmer.
- **Signals Angular 20** pour la réactivité (draftQty, pmpPreview, progress, etc.) sans
  `ChangeDetectionStrategy.OnPush` explicite — plus lisible.
- **Effet guard `_lastLineId`** pour éviter les boucles infinies lors du reset du formulaire lot.

### 2.2 Données affichées
- **PMP prévisionnel** calculé en temps réel lors de la saisie (vue séquentielle).
- **Stock après réception** recalculé à la volée.
- **Alerte colisage (PCB)** : badge visuel si qté reçue ≠ multiple du conditionnement.
- **Concordance en mode grille** : lignes totales, écarts qté, écarts prix, non validées, montants
  commandé/reçu/écart.
- **Variation de prix** avec seuil configurable avant finalisation.
- **Reliquat automatique** proposé si des lignes sont partiellement servies.

### 2.3 Scanning
- **CIP 1D** (EAN-13/Code128) via `ScanDetectorService` — détection automatique de la douchette
  sans configuration.
- **DataMatrix GS1** avec extraction AI 01 (GTIN), AI 10 (lot), AI 17 (expiry), AI 21 (serial).
- **FMD** (Falsified Medicines Directive) : badge PRESENT / ABSENT / DUPLICATE affiché
  immédiatement.
- **Pré-remplissage lot** : après un scan DataMatrix sans auto-création de lot, les champs N° lot
  et péremption sont pré-remplis dans le formulaire séquentiel (`scanLotPrefill`, délai 10 s).
- **Lot commun** : appliquer un seul lot à toutes les lignes sans lot d'un coup — gain de temps
  pour les livraisons homogènes.

---

## 3. Axes d'amélioration — analyse comparative

### 3.1 Données manquantes par rapport aux standards WMS/PMS

Les champs ci-dessous existent **dans le modèle** (`AbstractOrderItem`) mais ne sont **pas
affichés** dans le menu de réception :

| Champ modèle | Intérêt métier | Où l'ajouter |
|---|---|---|
| `tva` | Vérification fiscale — taux TVA appliqué | Colonne grille + détail séquentiel |
| `produitCodeEan` | Code EAN-13 catalogue (≠ CIP) — utile pour la conformité GS1 | Info-bulle/ colonne grille |
| `couvertureStockJours` | Jours de couverture après réception → alerte rupture prévisible | Colonne grille optionnelle |
| `discountAmount` | Remise accordée par le fournisseur | Colonne grille + modal édition |
| `netAmount` | Montant net de la ligne | Colonne grille + pied de tableau |
| `datePeremption` (ligne) | Date d'expiration au niveau lot — alerte produit proche péremption | Vue séquentielle produit header |
| `quantityReturned` | Quantité déjà retournée — contexte avant saisie | Info-bulle colonne stock |

Données **non présentes dans le modèle** mais affichées par les concurrents (Pharmavitale, Winpharma,
LGPI, Pharmagest) :

| Donnée manquante | Intérêt métier | Priorité |
|---|---|---|
| **Taux de service** (%) du fournisseur sur cette commande | KPI achat — `lignes_servies / lignes_commandées` | 🔴 Haute |
| **Marge brute unitaire** (PV - PA) par ligne | Rentabilité immédiate visible à la réception | 🟡 Moyenne |
| **Numéro de commande fournisseur** (PO number) | Référence croisée avec le bon fournisseur | 🔴 Haute |
| **Conditions de livraison** (délai, Incoterm) | Contrôle conformité livraison | 🟢 Basse |
| **Indicateur chaîne du froid** | Alerter si produit thermosensible (<+8°C) | 🔴 Haute |
| **Récapitulatif TVA** par taux (0% / 5,5% / 10% / 20%) | Comptabilité, rapprochement facture | 🔴 Haute |
| **Numéros de série manquants** par ligne | Conformité FMD — suivi des boîtes sans 2D | 🔴 Haute |

---

### 3.2 Mode séquentiel — lacunes par rapport au mode grille

| Fonctionnalité | Mode grille | Mode séquentiel | Action recommandée |
|---|---|---|---|
| Panneau concordance (anomalies) | ✅ Panneau latéral | ❌ Absent | Ajouter un bandeau de synthèse compact en haut du main panel |
| Résumé BL (fournisseur, ref, montants) | ✅ Panneau latéral | ❌ Absent | Ajouter au-dessus de la liste de navigation |
| Historique des prix | ✅ Bouton par ligne | ❌ Absent | Ajouter dans le product-header séquentiel |
| Édition infos produit | ✅ Bouton par ligne | ❌ Absent | Ajouter un bouton "Modifier" dans le footer step qty |
| Feedback scan FMD | ✅ Badge inline barre | ⚠️ Champ scan absent | Intégrer le champ scan dans la vue séquentielle |
| Filtre / recherche lignes | ✅ Select + recherche texte | ❌ Absent | Ajouter un filtre rapide dans la nav gauche |
| Retour fournisseur | ✅ Bouton header | ❌ Seul header | OK (dans barre globale, accessible) |

---

### 3.3 Gestion du scanning — ce que font les autres

#### 3.3.1 Scan CIP 1D — bonnes pratiques non implémentées

| Pratique | Détail | Mise en œuvre suggérée |
|---|---|---|
| **Retour sonore** | Bip succès (fréquence haute) / bip erreur (fréquence basse) — standard WMS | `AudioContext` : jouer un tone court côté frontend |
| **Highlight ligne scannée** | La ligne reçue s'illumine 1-2 s en vert/rouge dans la grille | `gridApi.flashCells()` sur le rowNode du résultat |
| **Auto-scroll** vers la ligne | La grille défile automatiquement jusqu'à la ligne mise à jour | `gridApi.ensureNodeVisible()` |
| **Compteur de scans** | Afficher "×3" sur une ligne si scannée 3 fois — contrôle de saisie | Ajouter champ `scanCount` temporaire dans l'état local |
| **Mode scan dédié** | Certains WMS ont un "mode pistolet" qui capture tout le focus | Déjà partiellement présent via `ScanDetectorService` ; améliorer en bloquant les saisies clavier non-scan quand actif |

#### 3.3.2 Scan DataMatrix GS1 — bonnes pratiques non implémentées

| Pratique | Détail | Mise en œuvre suggérée |
|---|---|---|
| **AI 37 (quantité)** | Le DataMatrix peut encoder la quantité dans AI 37 → incrémenter de N au lieu de 1 | Parser AI 37 côté backend dans `scanReception()` → utiliser `result.scannedQty` |
| **AI 00 (SSCC palet)** | Scan d'un code palette → réceptionner toute la palette d'un coup | Endpoint dédié `/api/commandes/{id}/scan-palette` |
| **Vérification FMD batch** | Avant clôture, afficher un rapport de tous les numéros de série scannés | Ajouter un onglet "Traçabilité FMD" dans `ReceptionFinalizeModalComponent` |
| **Détection doublons inter-BL** | Vérifier si le numéro de série a été reçu dans un **autre** BL précédent | Déjà partiellement géré (`DUPLICATE` FMD status) — à étendre aux BL archivés |
| **Scan hors ligne (offline)** | Queue locale des scans si réseau coupé, synchronisation différée | Service Worker + IndexedDB |
| **Scan caméra** | Permettre la caméra du PC/mobile comme scanner de secours | `@zxing/ngx-scanner` ou intégration Tauri camera |

---

### 3.4 UX / Ergonomie — points d'amélioration

#### 3.4.1 Incohérences entre les deux modes

1. **Double flux de finalisation** : en mode séquentiel, la finalization passe par
   `ReceptionFinalizeModal` → `finalizeSansConfirmModal()` ; en mode grille, par `onConfirmFinalize()`
   → confirmDialog. Ces deux chemins font la même chose mais avec des UX différentes.  
   **→ Unifier dans un seul service `ReceptionFinalizeService`.**

2. **Champ scan absent en mode séquentiel** : le champ "Scanner CIP / DataMatrix" est dans la
   barre de filtres qui n'est visible qu'en mode *grille*. Un scan en mode séquentiel est capturé
   silencieusement par `ScanDetectorService` mais aucun feedback visuel dédié n'est fourni.  
   **→ Ajouter un micro-bandeau de feedback scan dans le panneau séquentiel.**

3. **`scanLotPrefill` expire après 10 s** : si l'opérateur tarde à valider la quantité, le
   pré-remplissage lot est perdu.  
   **→ Conserver le pré-remplissage jusqu'à la validation de la quantité, pas à durée fixe.**

#### 3.4.2 Qualité de l'information

1. **Pas d'indicateur de sauvegarde auto** : l'utilisateur ne sait pas si sa quantité a bien été
   enregistrée. Ajouter un mini indicateur "✓ Sauvegardé" / spinner discret sur la ligne.

2. **Colonne "Statut" dans la grille** affiche 4 états (Servi / Rupture / Excédent / Partiel) mais
   sans distinction entre « non commencé » et « en cours de saisie ».  
   **→ Ajouter l'état "À saisir" (qté reçue = null/0, jamais touchée).**

3. **Alerte PCB** visible dans la grille (badge) mais **absente dans le panneau concordance** et
   dans le **récapitulatif final**.  
   **→ Ajouter `ecartColisage` dans `ConcordanceStats`.**

4. **Aucune indication de la date de la dernière réception** du produit pour comparer les tarifs
   historiques — pourtant déjà disponible via l'historique prix.

#### 3.4.3 Raccourcis et accessibilité

| Raccourci manquant | Action | Commentaire |
|---|---|---|
| `Ctrl+G` / `Ctrl+S` | Basculer Séquentiel ↔ Grille | Actuellement uniquement par clic |
| `F5` | Rafraîchir les lignes sans recharger la page | Utile si autre poste a modifié |
| `F11` | Tout valider (marquer toutes comme complètes) | Même chose que bouton "Tout valider" |
| `Alt+H` | Ouvrir l'aide | Actuellement uniquement par clic |

---

### 3.5 Fonctionnalités présentes dans les PMS concurrents et absentes ici

| Fonctionnalité | Logiciels de référence | Priorité |
|---|---|---|
| **Rapprochement automatique facture fournisseur** après réception | Winpharma, Pharmagest | 🔴 (module `reconciliation-facture` existe mais non lié ici) |
| **Ajout de ligne hors commande** (produit non commandé livré en bonus) | LGPI, Pharmavitale | 🔴 Haute — gérer les produits bonus / substitutions non anticipées |
| **Commentaire/litige par ligne** | Pharmagest Néo, PharmaERP | 🟡 Moyenne — noter un problème sur une ligne sans bloquer la réception |
| **Photo de réception** | Solutions modernes (Syslog Pharma) | 🟢 Basse — documenter les dommages d'emballage |
| **Alerte stock max** dépassé après réception | Winpharma | 🟡 Moyenne — éviter le sur-stockage |
| **Calcul automatique de la remise** sur la ligne à partir du PPA (prix public achat) | LGPI | 🟡 Moyenne |
| **Éclatement SSCC** (palette → colis → unités) | Pharmagest, SAP WM | 🟢 Basse — surtout pour les grands dépôts |
| **Contrôle température** à la réception (produits froids) | Solutions hospitalières | 🔴 Haute pharmaceutique — obligatoire pour les thermosensibles |

---

## 4. Plan d'amélioration priorisé & propositions détaillées

### Sprint 1 — Quick wins (< 2 jours)

- [ ] **AX-01** Feedback sonore scan (bip OK / bip KO) via `AudioContext`
- [ ] **AX-02** `gridApi.flashCells()` + `ensureNodeVisible()` après un scan réussi en mode grille
- [ ] **AX-03** Ajouter `ecartColisage` dans `ConcordanceStats` et dans le panneau concordance
- [ ] **AX-04** Unifier `ReceptionFinalizeModal` pour les deux modes (supprimer la double logique)
- [ ] **AX-05** Corriger `scanLotPrefill` — ne pas expirer à durée fixe mais à la validation de la quantité

### Sprint 2 — Données & affichage (2–4 jours)

- [ ] **AX-06** Colonne `tva` dans la grille (optionnelle, masquée par défaut)
- [ ] **AX-07** Colonne `discountAmount` + `netAmount` dans la grille
- [ ] **AX-08** Indicateur **Taux de service** du BL (badge dans le header)
- [ ] **AX-09** Bandeau concordance compact dans le panneau séquentiel *(voir détail §4.1)*
- [ ] **AX-10** Indicateur **chaîne du froid** (badge thermosensible) dans le product header séquentiel
- [ ] **AX-11** **Numéro de commande fournisseur** (PO) affiché dans le résumé BL
- [ ] **AX-12** Colonne `couvertureStockJours` dans la grille (optionnelle)
- [ ] **AX-13** État "À saisir" dans la colonne Statut (distinguer 0 jamais saisi vs 0 saisi)

### Sprint 3 — Scan avancé (3–5 jours)

- [ ] **AX-14** Parser **AI 37** (quantité) dans le DataMatrix → incrémenter de N unités
- [ ] **AX-15** Afficher le **feedback scan** (badge produit + statut FMD) dans le mode séquentiel *(voir détail §4.2)*
- [ ] **AX-16** **Rapport FMD** dans `ReceptionFinalizeModal` : liste de tous les numéros de série scannés
- [ ] **AX-17** Raccourcis clavier manquants (Ctrl+G, F5, F11, Alt+H)

### Sprint 4 — Fonctionnalités métier (1–2 semaines)

- [ ] **AX-18** **Ajout de ligne hors commande** — permettre d'ajouter un produit non prévu dans le BL
- [ ] **AX-19** **Commentaire / litige par ligne** — zone de texte libre + flag visuel
- [ ] **AX-20** **Récapitulatif TVA** par taux dans le modal de finalisation
- [ ] **AX-21** **Alerte stock max** dépassé après réception
- [ ] **AX-22** Lien direct vers le **module de rapprochement facture** depuis le header du BL

---

## 4.1 — Proposition détaillée AX-09 : Concordance compacte en mode séquentiel

### Contexte & contrainte layout

Le composant séquentiel est un **flex row 2 colonnes** :
```
rh-seq (flex row)
├── rh-seq__nav   (flex 0 0 32rem — nav gauche)
│   ├── barre de progression (4px)
│   ├── nav-header  (compteur ligne + toggle "Masquer traitées")
│   ├── nav-list    (liste scrollable des lignes) ← flex: 1 1 0
│   └── nav-footer  (boutons Précédent / Suivant)
└── rh-seq__main  (flex 1 — panneau de saisie)
    ├── product-header  (nom + chips infos)
    ├── body (scrollable) — step qty ou step lot
    └── footer  (bouton valider)
```

Il ne faut pas modifier ce layout global. L'objectif est d'insérer des informations
de concordance **sans encombrer l'espace de saisie**.

---

### Option A — Mini-strip de concordance dans la nav (⭐ Recommandée)

**Principe :** Insérer un bandeau compact (`rh-seq__nav-concordance`) entre le `nav-header`
et la `nav-list`. Il affiche les anomalies sous forme de chips colorées sur une seule ligne.
Quand tout va bien, il est vert discret et prend très peu de hauteur (~28px). Quand il y a des
anomalies, il monte en badge orange/rouge pour attirer l'œil.

```
rh-seq__nav
├── barre de progression 4px
├── nav-header (compteur + toggle)
├── [NOUVEAU] rh-seq__nav-concordance  ← inséré ici
│     ┌────────────────────────────────────────────────────┐
│     │ ●5/12  ⚠3 écarts qté  🔴2 écarts prix  📦0 lots  │
│     └────────────────────────────────────────────────────┘
│     (hauteur fixe ~28px, fond vert si 0 anomalie)
├── nav-list (liste scrollable)
└── nav-footer
```

**Données affichées** (calculées depuis l'input `orderLines` déjà disponible via `computed` dans
`reception-sequential.component`) :

| Chip | Condition d'affichage | Couleur |
|---|---|---|
| `X/Y lignes` | Toujours | Badge bleu principal |
| `⚠ N écarts qté` | Si N > 0 | Jaune/orange |
| `🔴 N écarts prix` | Si N > 0 | Rouge |
| `📦 N lots manquants` | Si `showLotBtn` et N > 0 | Orange |
| `✅ BL conforme` | Si aucune anomalie | Vert discret |

**Avantage :** L'opérateur voit en temps réel la progression globale du BL sans quitter la
vue de saisie. Quand les anomalies tombent à 0, le bandeau vire au vert — signal fort de
conformité avant de cliquer "Valider".

**Impact code :**
- Ajouter un `computed` `navConcordance` dans `reception-sequential.component.ts`
- Ajouter le bloc HTML entre `nav-header` et `nav-list`
- Ajouter 8 lignes de SCSS pour `.rh-seq__nav-concordance`
- **Aucun nouveau service, aucun appel HTTP**

```typescript
// Ajout dans reception-sequential.component.ts
protected readonly navConcordance = computed(() => {
  const lines = this.orderLines();
  let ecartQte = 0, ecartPrix = 0, lotsManquants = 0;
  for (const l of lines) {
    if ((l.quantityReceivedTmp ?? 0) !== (l.quantityRequested ?? 0)) ecartQte++;
    if (l.costAmount && l.orderCostAmount && l.costAmount !== l.orderCostAmount) ecartPrix++;
    if (this.showLotBtn() && l.gestionLot !== false && (l.lots?.length ?? 0) === 0) lotsManquants++;
  }
  return { ecartQte, ecartPrix, lotsManquants, hasAnomaly: ecartQte + ecartPrix + lotsManquants > 0 };
});
```

```html
<!-- Entre nav-header et nav-list -->
<div class="rh-seq__nav-concordance"
     [class.rh-seq__nav-concordance--ok]="!navConcordance().hasAnomaly"
     [class.rh-seq__nav-concordance--warn]="navConcordance().hasAnomaly">
  @if (!navConcordance().hasAnomaly) {
    <i class="pi pi-check-circle"></i>
    <span>BL conforme</span>
  } @else {
    @if (navConcordance().ecartQte > 0) {
      <span class="rh-seq__nav-chip rh-seq__nav-chip--warn">
        <i class="pi pi-sort-alt"></i>{{ navConcordance().ecartQte }} qté
      </span>
    }
    @if (navConcordance().ecartPrix > 0) {
      <span class="rh-seq__nav-chip rh-seq__nav-chip--danger">
        <i class="pi pi-euro"></i>{{ navConcordance().ecartPrix }} prix
      </span>
    }
    @if (showLotBtn() && navConcordance().lotsManquants > 0) {
      <span class="rh-seq__nav-chip rh-seq__nav-chip--warn">
        <i class="pi pi-box"></i>{{ navConcordance().lotsManquants }} lots
      </span>
    }
  }
</div>
```

---

### Option B — Panneau latéral concordance plein (❌ Non recommandée ici)

Réutiliser `<app-reception-concordance>` dans la nav, comme en mode grille.
**Inconvénient :** prend ~200px de hauteur, réduit trop la liste des lignes dans la nav.
À réserver pour un éventuel panneau latéral dédié accessible par toggle.

---

## 4.2 — Proposition détaillée AX-15 : Feedback scan en mode séquentiel

### Contexte

**Situation actuelle :**
- Les scans (CIP et DataMatrix) sont capturés par `ScanDetectorService` dans
  `commande-received.component`
- L'appel `deliveryService.scanReception()` est fait **dans le parent**
- Le résultat `lastScanResult` est un `signal<IReceptionScanResult | null>` dans le parent
- En mode séquentiel, on voit **rien** : ni la ligne mise à jour, ni le statut FMD, ni les erreurs

**Utilisation réelle (contexte officine) :**
Aujourd'hui le scan CIP/EAN est le principal mode. Le DataMatrix n'est pas encore répandu
chez les grossistes du marché africain mais l'infrastructure doit le supporter pour
l'avenir. Le feedback visuel doit donc couvrir les deux cas sans surcharger l'UI.

---

### Fonctionnement global du scan — architecture actuelle

Le `ScanDetectorService` (basé sur `BaseScannerService`) fonctionne **par la vitesse de frappe** :

```
Scanner (douchette)
  → envoie chaque caractère très vite (< 30 ms entre touches) via HID
  → se termine par [Enter]

document.addEventListener('keydown', listener, capture: true)
  → intercepté AVANT les inputs
  → BaseScannerService.processKey(key)
      ├─ timestamps.length ≥ 2 ET intervalle ≤ 30ms → scanInProgress = true
      ├─ [Enter] + scanInProgress + buffer.length ≥ 6 → scan COMPLETE → onScanEvent$
      └─ Pause > 150ms → reset
```

**✅ Le scan fonctionne DÉJÀ globalement** sans que le curseur soit dans un input dédié :
`commande-received.component` enregistre le listener sur `document` avec `capture: true` — il
intercepte les touches avant tout élément de la page.

**⚠️ Le problème réel : pollution des inputs actifs**

Si le curseur est dans le champ `numLotInput` (formulaire lot step) au moment d'un scan CIP :

```
Scan CIP "3400935782791" + [Enter]
            │
            ├─ keydown capturé → ScanDetectorService accumule
            ├─ event NON preventDefault → "3400935782791" tapé dans numLotInput
            │                              ⚠️ draftLotNum = "3400935782791" (mauvaise valeur!)
            └─ [Enter] → scan complet → onScanReception() → +1 qté sur la ligne
                                        ✅ mais numLotInput pollué
```

**C'est le bug principal à corriger avant tout le reste.**

---

### Correction prioritaire — Bloquer la pollution des inputs (AX-15a) ✅ IMPLÉMENTÉ

#### Problème d'isolation du singleton root

`ScanDetectorService` est `providedIn: 'root'` → **singleton partagé** avec la vente et les autres
modules. Le buffer de scan est commun : une frappe rapide dans le module vente pouvait interférer
avec la réception.

#### Solution : `ReceptionScannerService` dédié

Fichier créé : `features/commande/feature/commande-received/reception-scanner.service.ts`

```typescript
@Injectable()  // ← PAS providedIn: 'root' — scopé au composant
export class ReceptionScannerService extends BaseScannerService {}
```

Déclaré dans `providers` du composant de réception :

```typescript
@Component({
  ...
  providers: [ReceptionScannerService],  // ← instance isolée, détruite avec le composant
})
export class CommandeReceivedComponent { ... }
```

**Garanties :**
- Buffer de scan **entièrement isolé** de la vente et des autres modules
- Instance détruite à la navigation → pas de fuite mémoire
- `ScanDetectorService` (vente, recherche produit) **inchangé** — zéro régression

#### Protection anti-pollution des inputs — `setupBarcodeScanner()`

```typescript
// AVANT (bug)
this.keydownListener = (event) => this.scanDetectorService.keyPressed(event.key);

// APRÈS
this.keydownListener = (event: KeyboardEvent): void => {
  const result = this.receptionScanner.processKey(event.key);
  // Dès qu'une frappe rapide est détectée (scan en cours) ET qu'un input est actif
  // → bloquer l'écriture dans cet input (mais continuer d'accumuler dans le buffer scan)
  if (result.isScanInProgress && this.isInputElementActive()) {
    event.preventDefault();
  }
};

private isInputElementActive(): boolean {
  const el = document.activeElement;
  if (el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement) {
    if (el === this.scanInputRef()?.nativeElement) return false; // champ scan dédié → OK
    return true;
  }
  return false;
}
```

> **Nuance** : le 1er caractère arrive toujours dans l'input (`isScanInProgress` n'est vrai qu'à
> partir du 2ème caractère rapide). Ce premier caractère parasite sera effacé par le `resetLotDraft()`
> déclenché lors de la navigation vers une nouvelle ligne après le scan.

#### Correction AX-05 — Suppression du timer fixe `scanLotPrefill` ✅ IMPLÉMENTÉ

```typescript
// AVANT — timer fixe 10 s (trop court si l'opérateur est lent)
this.scanPrefillTimer = setTimeout(() => this.scanLotPrefill.set(null), 10000);

// APRÈS — effacé à la validation de la ligne (consommé naturellement)
protected onSequentialLineChanged(updatedLine: IOrderLine): void {
  this.scanLotPrefill.set(null); // consommé → effacer
  ...
}
```

---

### Comportement scan selon l'étape active

#### Étape 1 (saisie quantité) — step "qty"

```
Scan CIP pendant step qty
              ↓
  ScanDetectorService → scan complet
              ↓
  onScanReception() → deliveryService.scanReception()
              ↓
  ┌─ Ligne COURANTE ───────────────────────────────────┐
  │  + 1 qté reçue                                     │
  │  Feedback visuel : bande verte (nom produit)       │
  │  draftQty mis à jour → champ "Reçu" se rafraîchit  │
  └────────────────────────────────────────────────────┘
  ┌─ AUTRE ligne ──────────────────────────────────────┐
  │  + 1 qté reçue sur la ligne concernée              │
  │  Navigation auto → currentLineId = ligne scannée   │
  │  Feedback visuel : bande verte + "≠ ligne courante"│
  └────────────────────────────────────────────────────┘
  ┌─ Non trouvé ───────────────────────────────────────┐
  │  Aucune mise à jour                                 │
  │  Feedback visuel : bande rouge                     │
  └────────────────────────────────────────────────────┘
```

**→ Les quantités ne sont PAS saisies directement par scan.** Le scan fait +1 et l'opérateur
peut ensuite ajuster manuellement dans le champ "Reçu" si nécessaire.

---

#### Étape 2 (saisie lot) — step "lot"

C'est ici que le comportement **diffère selon CIP vs DataMatrix**.

**CIP 1D scanné pendant step lot :**

```
Scan CIP pendant step lot
              ↓
  ScanDetectorService → scan complet
  + isActiveElementAnInput() = true (numLotInput focusé)
  → event.preventDefault() sur tous les chars sauf le 1er
  → nettoyer numLotInput après scan
              ↓
  onScanReception() → + 1 qté sur la ligne scannée
  → si ligne scannée ≠ ligne courante → navigation auto → resetLotDraft()
  → si même ligne → rester en step lot, draftQty mise à jour en background
  Feedback visuel : bande verte "CIP scanné — qté mise à jour"
```

> **L'opérateur continue la saisie du lot** après le scan. Le scan ne ferme pas le formulaire lot.

---

**DataMatrix scanné pendant step lot :**

```
Scan DataMatrix pendant step lot
              ↓
  Backend parse : GTIN + lot (AI10) + expiry (AI17) + série (AI21)
              ↓
  ┌─ lotAutoCreated = true ────────────────────────────────────────┐
  │  Lot créé en base automatiquement                              │
  │  → refreshCommande() → lineLots mis à jour                    │
  │  → si lotsComplete() → goNext() automatique                   │
  │  Feedback : bande verte + badge lot + badge FMD ✓             │
  └────────────────────────────────────────────────────────────────┘
  ┌─ lotAutoCreated = false ───────────────────────────────────────┐
  │  scanLotPrefill = { numLot: "A1B2C3", expiry: "06/2027" }     │
  │  → applyPrefill() → draftLotNum et draftLotExpiry remplis     │
  │  → focus sur le champ N° lot                                  │
  │  L'opérateur valide avec F12                                   │
  │  Feedback : bande verte + "Lot pré-rempli depuis DataMatrix"  │
  └────────────────────────────────────────────────────────────────┘
```

---

### Tableau récapitulatif — comportement complet

| Situation | Type scan | Curseur | Résultat attendu |
|---|---|---|---|
| Step qty, curseur libre | CIP | Aucun input | +1 qté, feedback vert |
| Step qty, curseur dans champ "Reçu" | CIP | `qtyInput` | +1 qté, preventDefault, feedback vert |
| Step qty, curseur libre | DataMatrix | Aucun input | +1 qté + lot auto ou prefill |
| Step lot, curseur dans N° lot | CIP | `numLotInput` | +1 qté, preventDefault, nettoyer champ, feedback vert |
| Step lot, curseur dans N° lot | DataMatrix | `numLotInput` | +1 qté, prefill appliqué, champ nettoyé puis pré-rempli |
| Step lot, curseur libre | CIP | Aucun input | +1 qté, feedback vert |
| Step lot, curseur libre | DataMatrix | Aucun input | +1 qté + lot auto ou prefill |
| N'importe where | CIP non trouvé | N'importe | Feedback rouge, rien mis à jour |

---

### Proposition : Bande de feedback scan flottante dans le panneau principal

**Principe :** Ajouter un `lastScanResult` en input de `reception-sequential.component`.
Le composant l'affiche sous le `product-header` dans une bande animée qui disparaît après 4 s.
Quand le scan correspond à la ligne courante, la bande est verte et un flash subtil met en
évidence la valeur de qté reçue.

```
rh-seq__main
├── product-header  (nom + chips)
├── [NOUVEAU] rh-seq__scan-feedback  ← apparaît après chaque scan
│     ┌────────────────────────────────────────────────────────────────────┐
│     │ ✅  DOLIPRANE 500mg CIP 3400935782791  · Qté reçue : 3            │
│     │     [badge lot A1B2C3]  [badge FMD ✓]              ← DataMatrix   │
│     └────────────────────────────────────────────────────────────────────┘
│   ou
│     ┌────────────────────────────────────────────────────────────────────┐
│     │ ❌  CIP 3400912345678 — Produit non trouvé dans ce BL             │
│     └────────────────────────────────────────────────────────────────────┘
│   → animation fade-in, disparaît après 4 s (ou au prochain scan)
├── body (saisie qty / lot)
└── footer
```

**Impact code :**

```typescript
// 1. Ajouter l'input dans reception-sequential.component.ts
lastScanResult = input<IReceptionScanResult | null>(null);

// 2. Ajouter un effect pour naviguer automatiquement si la ligne
//    scannée est différente de la ligne courante
constructor() {
  // ...existing effects...
  effect(() => {
    const result = this.lastScanResult();
    untracked(() => {
      if (!result?.found || !result.orderLineId) return;
      // Si la ligne scannée n'est pas la ligne courante → naviguer vers elle
      if (result.orderLineId !== this.currentLine()?.id) {
        this.currentLineId.set(result.orderLineId);
      }
    });
  });
}
```

```html
<!-- Dans commande-received.component.html — passer le signal en input -->
<app-reception-sequential
  [orderLines]="orderLines"
  [showLotBtn]="showLotBtn"
  [lotPrefill]="scanLotPrefill()"
  [lastScanResult]="lastScanResult()"
  (lineChanged)="onSequentialLineChanged($event)"
  (allLinesProcessed)="onSequentialAllDone()"
/>
```

```html
<!-- Dans reception-sequential.component.html — juste après product-header -->
@if (lastScanResult(); as scan) {
  <div class="rh-seq__scan-feedback"
       [class.rh-seq__scan-feedback--ok]="scan.found"
       [class.rh-seq__scan-feedback--err]="!scan.found">
    <i [class]="scan.found ? 'pi pi-check-circle' : 'pi pi-times-circle'"></i>
    <span class="rh-seq__scan-product">
      {{ scan.found ? scan.produitLibelle : (scan.warningMessage ?? 'Produit non trouvé') }}
    </span>
    @if (scan.found && scan.lotNumero) {
      <span class="rh-seq__scan-chip rh-seq__scan-chip--lot">
        <i class="pi pi-box"></i>{{ scan.lotNumero }}
      </span>
    }
    @if (scan.found && scan.fmdStatus === 'PRESENT') {
      <span class="rh-seq__scan-chip rh-seq__scan-chip--fmd-ok">
        <i class="pi pi-shield"></i>FMD ✓
      </span>
    }
    @if (scan.found && scan.fmdStatus === 'DUPLICATE') {
      <span class="rh-seq__scan-chip rh-seq__scan-chip--fmd-dup">
        <i class="pi pi-shield"></i>DOUBLON !
      </span>
    }
  </div>
}
```

---

> **Navigation auto sur scan hors ligne courante :** c'est la pratique des WMS modernes.
> Un préparateur qui scanne en vrac peut scanner n'importe quelle boîte dans n'importe quel
> ordre — le système navigue vers la bonne ligne automatiquement. Cela accélère considérablement
> la saisie dans les BL importants.

---

### Comportement du timer `scanLotPrefill` (correction AX-05 liée)

**Problème actuel :** le pré-remplissage expire après 10 s quelle que soit l'interaction.

**Solution :** Supprimer le timer à durée fixe. Effacer le `scanLotPrefill` uniquement quand :
1. L'utilisateur valide la quantité (`onValidateQty()`) → step lot s'ouvre avec le pré-remplissage
2. L'utilisateur navigue vers une autre ligne manuellement

```typescript
// Dans commande-received.component.ts — onSequentialLineChanged()
protected onSequentialLineChanged(updatedLine: IOrderLine): void {
  this.scanLotPrefill.set(null); // consommé → effacer
  // ...existing code...
}
```

---

## 5. Comparaison détaillée — Scan CIP vs DataMatrix

```
                    ┌────────────────────────────────────────────────────────┐
                    │          SCAN EN RÉCEPTION — ÉTAT ACTUEL               │
                    └────────────────────────────────────────────────────────┘

CIP 1D (EAN-13)          DataMatrix GS1-128
      │                         │
      ▼                         ▼
ScanDetectorService     ScanDetectorService
      │                         │
      ▼                         ▼
deliveryService             deliveryService
  .scanReception()          .scanReception()
      │                         │
      ├─ found? → +1 qté        ├─ found? → +1 qté
      │                         ├─ lotAutoCreated? → lot créé
      │                         ├─ FMD: PRESENT / ABSENT / DUPLICATE
      │                         └─ lot.numLot + expiryDate → scanLotPrefill
      │
      ▼
Badge feedback (barre globale)
⚠️ Absent dans vue séquentielle

                    ┌────────────────────────────────────────────────────────┐
                    │       AMÉLIORATIONS RECOMMANDÉES (SCAN)                │
                    └────────────────────────────────────────────────────────┘

CIP 1D                   DataMatrix GS1-128
      │                         │
      ├─ + bip sonore OK        ├─ + bip sonore OK/KO
      ├─ + flash ligne          ├─ + flash ligne
      ├─ + auto-scroll          ├─ + AI 37 → qty N
      ├─ + compteur scans       ├─ + SSCC palette (hors scope v1)
      └─ + feedback séquentiel  ├─ + rapport FMD batch
                                └─ + feedback dans vue séquentielle
```

---

## 6. Modèle de données — champs à valoriser

Les champs suivants sont **déjà dans le modèle backend** mais pas exploités côté frontend :

```typescript
// AbstractOrderItem (non utilisés en réception)
tva?: number;                // → Colonne grille + récap TVA
discountAmount?: number;     // → Colonne remise
netAmount?: number;          // → Colonne montant net
couvertureStockJours?: number; // → Colonne couverture
produitCodeEan?: string;     // → Info-bulle / filtre
quantityReturned?: number;   // → Info-bulle stock
datePeremption?: Date;       // → Alerte péremption produit header
```

> Ces champs peuvent être valorisés **sans modification backend** si le service
> `CommandeService.filterCommandeLines()` les retourne déjà dans le DTO.
> À vérifier avec `OrderLineDTO` côté Java.

---

## 7. Synthèse visuelle

```
Points forts ✅                     Lacunes 🔴
─────────────────────────          ──────────────────────────────
✅ Dual mode séq/grille            🔴 Scan feedback absent en séquentiel
✅ FMD / DataMatrix                🔴 Concordance absente en séquentiel
✅ PMP prévisionnel                🔴 Taux de service globale absent
✅ Alerte PCB                      🔴 Pas de ligne hors-commande
✅ Reliquat automatique            🔴 Pas d'alerte stock max
✅ Lot commun (batch)              🔴 Pas de récap TVA par taux
✅ Variation prix avant clôture    🔴 AI 37 DataMatrix non parsé
✅ Putaway integration             🔴 Pas de bip sonore / flash ligne
✅ Navigation clavier F8/F9/F12   🔴 Pas de commentaire/litige ligne
✅ Retour fournisseur depuis BL    🔴 Pas d'indicateur chaîne du froid
```

## 8. Options de scan plus efficientes — comparatif

### Inventaire de l'infrastructure existante (atouts cachés)

Avant de proposer de nouvelles options, l'audit du code révèle des composants **déjà présents**
mais non exploités en réception :

| Composant existant | Localisation | Statut en réception |
|---|---|---|
| `GlobalScannerService` (TIMING + STX/ETX) | `shared/global-scanner.service.ts` | ❌ Non utilisé |
| `PrefixSuffixScannerService` | `shared/scanner/prefix-suffix-scanner.service.ts` | ❌ Non utilisé |
| `serialport` crate Rust | `src-tauri/Cargo.toml` | ❌ Non branché front |
| `jSerialComm` Java | `pom.xml` | ✅ Utilisé imprimantes |
| `ZXing` Android (caméra) | `mobile-inventory/BarcodeScanner.kt` | ✅ Mobile inventaire |
| Param `APP_SCANNER_MODE` | ConfigurationService | ❌ Ignoré en réception |

---

### Option 1 — Mode STX/ETX (Préfixe/Suffixe) ⭐⭐⭐ **Recommandée immédiatement**

#### Principe

On configure le scanner physique pour encadrer chaque code-barres de deux caractères spéciaux :
- **STX** (`\x02` = Ctrl+B) en début de code
- **ETX** (`\x03` = Ctrl+C) en fin de code (au lieu de Enter)

```
Mode actuel (TIMING)         Mode STX/ETX
──────────────────────       ─────────────────────────
3 4 0 0 9 3 5 ... [Enter]   STX 3 4 0 0 9 3 5 ... ETX
                             │                       │
Détection par vitesse (30ms) Détection par délimiteurs
→ Peut être faussé par       → Impossible d'être un faux
  frappe rapide normale        positif, 100% fiable
```

#### Intégration dans le module réception

`GlobalScannerService` + `PrefixSuffixScannerService` **existent déjà** dans le projet et le
paramètre `APP_SCANNER_MODE` est déjà lu. Il suffit de faire utiliser ce service par la réception.

**Modification de `ReceptionScannerService`** — remplacer l'extension de `BaseScannerService`
par l'utilisation de `GlobalScannerService` :

```typescript
// reception-scanner.service.ts — NOUVEAU (remplacement du service actuel)
@Injectable()
export class ReceptionScannerService {
  private readonly globalScanner = inject(GlobalScannerService);

  /** Observable unifié : fonctionne en TIMING ou STX/ETX selon APP_SCANNER_MODE */
  readonly onScan$ = this.globalScanner.onScan$;

  activate(): void  { this.globalScanner.enable(); }
  deactivate(): void { this.globalScanner.disable(); }
  isScanActive(): boolean { return this.globalScanner.isScanActive(); }

  processKeyEvent(event: KeyboardEvent): { isScanInProgress: boolean } {
    return this.globalScanner.processKeyEvent(event);
  }
}
```

**Dans `commande-received.component.ts`** :

```typescript
// setupBarcodeScanner() — remplacer le listener
this.receptionScanner.activate(); // active le mode selon APP_SCANNER_MODE

this.receptionScanner.onScan$
  .pipe(takeUntilDestroyed(this.destroyRef))
  .subscribe(code => {
    this.scanValue = code;
    this.onScanReception();
  });

this.keydownListener = (event: KeyboardEvent): void => {
  const result = this.receptionScanner.processKeyEvent(event);
  if (result.isScanInProgress && this.isInputElementActive()) {
    event.preventDefault(); // Protection anti-pollution (TIMING uniquement — inutile en STX/ETX)
  }
};
```

**Avantages :**
- ✅ Zéro faux positif — une frappe normale ne déclenchera jamais un scan
- ✅ Zéro pollution des inputs — STX/ETX ne sont jamais des caractères saisissables
- ✅ `preventDefault` devient inutile en mode STX/ETX (mais reste pour TIMING)
- ✅ Infrastructure **déjà codée dans le projet**
- ✅ Mode configurable sans recompilation via `APP_SCANNER_MODE`
- ⚠️ Nécessite une configuration unique du scanner physique (menée une fois)

---

### Option 2 — Tauri Serial Port ⭐⭐⭐⭐⭐ **PRIORITÉ — déploiement desktop** ✅ Retenu

#### Principe

Configurer le scanner en **mode USB CDC (Virtual COM Port) ou série physique** (au lieu de HID clavier). Le scanner envoie les codes via un port COM virtuel → Tauri (Rust) lit le port → envoie l'événement à Angular.

> **Décision projet :** Cette option est la solution **privilégiée** pour le déploiement Tauri.
> Elle couvre les deux cas matériels identiques côté code :
> - **USB CDC** : douchette USB reprogrammée en Virtual COM (ex : COM6)
> - **Série physique** : scanner RS-232 via adaptateur USB-série (ex : COM3)
>
> La crate `serialport` et la commande `list_serial_ports()` ne font **aucune distinction**
> entre ces deux types — le même code Rust gère les deux.

```
Scanner → COM3 (9600 baud) ou USB CDC Virtual COM
              ↓
        Rust (serialport crate)
        déjà dans Cargo.toml !
              ↓
        tauri::emit("scan-event", code)
              ↓
        Angular listen("scan-event")
              ↓
        onScanReception()
```

#### Preuve de concept — côté Rust

```rust
// src-tauri/src/lib.rs
use serialport::SerialPort;
use tauri::AppHandle;

#[tauri::command]
async fn start_scanner_listener(app: AppHandle, port_name: String) {
    let port = serialport::new(&port_name, 9600)
        .timeout(std::time::Duration::from_millis(100))
        .open();

    if let Ok(mut port) = port {
        let mut buf = Vec::new();
        loop {
            let mut byte = [0u8; 1];
            if port.read(&mut byte).is_ok() {
                if byte[0] == b'\r' || byte[0] == b'\n' {
                    if !buf.is_empty() {
                        let code = String::from_utf8_lossy(&buf).to_string();
                        app.emit("scan-reception", &code).ok();
                        buf.clear();
                    }
                } else {
                    buf.push(byte[0]);
                }
            }
        }
    }
}
```

#### Côté Angular

```typescript
// Dans CommandeReceivedComponent.setupBarcodeScanner()
import { listen } from '@tauri-apps/api/event';

if (this.tauriPrinterService.isRunningInTauri()) {
  listen<string>('scan-reception', event => {
    this.scanValue = event.payload;
    this.onScanReception();
  }).then(unlisten => this.destroyRef.onDestroy(unlisten));
}
```

**Avantages :**
- ✅ `serialport` déjà dans `Cargo.toml` (feature `default = ["serialport"]`)
- ✅ Zéro interférence clavier — le scan contourne totalement le HID
- ✅ Fonctionne même si l'app est en arrière-plan
- ✅ Liste des ports COM disponibles via Tauri → UI de configuration
- ✅ **USB CDC et série physique traités de façon identique** — un seul code Rust pour les deux
- ⚠️ Nécessite scanner avec mode COM (la majorité des scanners professionnels le supportent)
- ⚠️ Uniquement pour déploiement Tauri desktop (non applicable navigateur web — voir décision Option 3)

---

### Option 3 — Web Serial API ~~⭐ **Alternative navigateur**~~ 🚫 **Non retenue**

> ❌ **Ce point ne sera pas implémenté.**  
> L'application cible un déploiement **desktop Tauri** uniquement. La Web Serial API navigateur
> (Chrome/Edge) introduit des contraintes inutiles (prompt d'autorisation par session, API
> expérimentale, non supportée sur Firefox/Safari). La lecture du port série est traitée
> entièrement côté Rust via la crate `serialport` (Option 2 ci-dessus).

~~Même concept que l'option 2 mais côté JavaScript, sans Tauri.~~

---

### Option 4 — Caméra (ZXing / @zxing/ngx-scanner) ⭐ **Fallback mobile/PC**

L'application mobile-inventory utilise déjà ZXing (Android). La même bibliothèque existe
pour Angular web :

```bash
npm install @zxing/ngx-scanner
```

```html
<!-- Composant caméra scan -->
<zxing-scanner [enable]="scannerEnabled"
               (scanSuccess)="onScanSuccess($event)"
               (scanError)="onScanError($event)"
               [formats]="[EAN_13, CODE_128, DATA_MATRIX]">
</zxing-scanner>
```

**Avantages :**
- ✅ Aucun scanner physique requis — caméra PC ou smartphone
- ✅ Support DataMatrix natif (ZXing supporte GS1 DataMatrix)
- ✅ Cohérent avec l'app Android existante
- ⚠️ Plus lent qu'un scanner laser pour une utilisation intensive
- ⚠️ Éclairage requis (pas adapté aux environnements sombres)
- ⚠️ Idéal comme fallback, pas comme mode principal

---

### Comparatif synthèse

| Option | Fiabilité | Conf. matériel | Code à écrire | Environnement | Recommandation |
|---|---|---|---|---|---|
| **STX/ETX (préfixe)** | ⭐⭐⭐⭐⭐ | Scanner physique (1 fois) | Minimal (déjà codé) | All | **Priorité 1** |
| **Tauri Serial Port (USB CDC / série)** | ⭐⭐⭐⭐⭐ | Scanner en mode COM | ~50 lignes Rust | Desktop Tauri | **Priorité 2 ✅ Retenu** |
| **TIMING actuel** | ⭐⭐⭐ | Aucune | Déjà fait | All | Fallback |
| ~~**Web Serial API**~~ | ~~⭐⭐⭐⭐~~ | ~~Scanner en mode COM~~ | ~~40 lignes JS~~ | ~~Chrome/Edge~~ | 🚫 **Non retenu** |
| **Caméra ZXing** | ⭐⭐ | Aucune | npm install | All | Fallback |

---

### Recommandation finale — Stratégie à 2 niveaux

```
Niveau 1 — Configuration scanner (immédiat, zéro code) :
  APP_SCANNER_MODE = STX/ETX
  → Configurer le scanner pour émettre STX/ETX
  → ReceptionScannerService délègue à GlobalScannerService
  → Zero false positive, zero input pollution

Niveau 2 — Tauri Serial Port : USB CDC (Virtual COM) ou série physique ✅ RETENU (sprint 3, ~2j) :
  → scanner branché en USB CDC (Virtual COM) ou série physique
  → Rust lit le port via crate serialport → emit('scan-reception')
  → Angular reçoit l'événement → onScanReception()
  → Complètement indépendant du clavier
  → USB CDC et série physique : même code Rust, zéro distinction

❌ Option Web Serial API (navigateur) : non retenue — cible Tauri desktop uniquement.
```

---

## 9. Douchette USB — gestion des deux modes de connexion

### Les deux modes USB d'une douchette de pharmacie

Une douchette connectée en USB peut fonctionner selon **deux protocoles distincts**, choisis par
une simple configuration du scanner (scan d'une étiquette dans le manuel) :

```
┌─────────────────────────────────────────────────────────────────────┐
│                     DOUCHETTE USB                                    │
│                                                                      │
│  Mode 1 : USB HID Clavier (défaut usine)                            │
│  ──────────────────────────────────────                              │
│  Scanner → USB HID → OS génère keydown events → navigateur          │
│  → document.addEventListener keydown → ReceptionScannerService      │
│  ✅ DÉJÀ GÉRÉ (code actuel)                                          │
│                                                                      │
│  Mode 2 : USB CDC / Virtual COM Port                                 │
│  ──────────────────────────────────────                              │
│  Scanner → USB CDC → OS crée COM virtuel (ex : COM6)                │
│  → Rust serialport lit COM6 → tauri::emit → Angular listen          │
│  ✅ GÉRÉ par la même implémentation Tauri Serial (§8, Option 2)      │
└─────────────────────────────────────────────────────────────────────┘
```

**Point clé :** en mode USB CDC, le port virtuel apparaît **identiquement** à un port série
physique. La crate `serialport` et `list_serial_ports()` ne font aucune distinction — le même
code Rust gère les deux.

---

### Détection du type de port — enrichir `list_serial_ports`

La crate `serialport` expose les métadonnées USB de chaque port. Ajouter une commande Tauri
dédiée pour distinguer les scanners USB CDC des ports série physiques :

```rust
// src-tauri/src/scanner.rs — commande à ajouter
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SerialPortInfo {
    pub port_name: String,
    pub port_type: String,            // "USB", "PCI", "Unknown"
    pub usb_vid: Option<u16>,         // Vendor ID ex: 0x0536 = Honeywell
    pub usb_pid: Option<u16>,         // Product ID
    pub manufacturer: Option<String>, // ex: "Honeywell"
    pub product: Option<String>,      // ex: "Voyager 1200g"
}

#[tauri::command]
pub async fn list_serial_ports_detailed() -> Result<Vec<SerialPortInfo>, String> {
    #[cfg(feature = "serialport")]
    {
        let ports = serialport::available_ports()
            .map_err(|e| e.to_string())?;

        Ok(ports.iter().map(|p| {
            match &p.port_type {
                serialport::SerialPortType::UsbPort(info) => SerialPortInfo {
                    port_name: p.port_name.clone(),
                    port_type: "USB".to_string(),
                    usb_vid: Some(info.vid),
                    usb_pid: Some(info.pid),
                    manufacturer: info.manufacturer.clone(),
                    product: info.product.clone(),
                },
                serialport::SerialPortType::PciPort => SerialPortInfo {
                    port_name: p.port_name.clone(),
                    port_type: "PCI".to_string(),
                    usb_vid: None, usb_pid: None,
                    manufacturer: None, product: None,
                },
                _ => SerialPortInfo {
                    port_name: p.port_name.clone(),
                    port_type: "Unknown".to_string(),
                    usb_vid: None, usb_pid: None,
                    manufacturer: None, product: None,
                },
            }
        }).collect())
    }
    #[cfg(not(feature = "serialport"))]
    Err("serialport non activé".to_string())
}
```

**Côté Angular — UI de sélection du port :**

```typescript
// Vendors USB connus pour les scanners de pharmacie
const SCANNER_USB_VENDORS: Record<number, string> = {
  0x0536: 'Honeywell / Metrologic',
  0x05E0: 'Zebra / Symbol',
  0x0C2E: 'Datalogic',
  0x08F0: 'Unitec',
  0x04B4: 'Newland / Cypress',
  0x0483: 'Opticon',
};

const ports = await invoke<SerialPortInfo[]>('list_serial_ports_detailed');

// Identifier automatiquement les scanners USB probables
const scannerSuggestions = ports.filter(p =>
  p.portType === 'USB' && p.usbVid && SCANNER_USB_VENDORS[p.usbVid]
);
// → Afficher un badge "Scanner détecté : Honeywell — COM6"
```

---

### Option 3 (bonus) — WebHID : USB sans passer par le clavier

Pour les douchettes USB HID, le navigateur expose l'**API WebHID** (disponible dans Tauri
WebView2, Chrome, Edge) permettant une lecture directe **sans passer par les événements clavier**,
et donc sans aucun risque de pollution d'inputs :

```typescript
// Connexion directe USB HID — bypass total du pipeline clavier
const [device] = await navigator.hid.requestDevice({
  filters: [
    { vendorId: 0x0536 }, // Honeywell/Metrologic
    { vendorId: 0x05E0 }, // Symbol/Zebra
    { vendorId: 0x0C2E }, // Datalogic
    { vendorId: 0x04B4 }, // Newland (courant en Afrique de l'Ouest)
  ]
});

await device.open();

// Un seul permis suffit par appareil (mémorisé par le navigateur)
device.addEventListener('inputreport', (event: HIDInputReportEvent) => {
  const code = decodeHidBarcode(event.data); // décode les bytes HID → string
  if (code) {
    this.ngZone.run(() => {
      this.scanValue = code;
      this.onScanReception();
    });
  }
});
```

> **Avantage** : aucune reprogrammation du scanner nécessaire (reste en mode HID par défaut),
> mais la communication contourne totalement le pipeline clavier.

---

### Cycle de vie — démarrage de l'écoute du port

**Moment optimal : `ngOnInit` du `CommandeReceivedComponent`**

```
[1] Utilisateur sélectionne un BL
        ↓
[2] CommandeReceivedComponent créé
    → commande input.required() déjà défini
        ↓ ngOnInit()
[3] setupBarcodeScanner()
    ├─ Tauri disponible ET port COM configuré ?
    │    └─ invoke('start_scanner_listener') → port ouvert ~50ms
    │       listen('scan-reception') → abonnement Tauri events
    │       listen('scan-reception-error') → fallback HID si débranché
    │       return (HID non activé)
    │
    └─ Sinon → setupHidFallback()
               (TIMING ou STX/ETX via ReceptionScannerService)
        ↓
[4] Port occupé UNIQUEMENT pendant qu'un BL est ouvert

[5] Validation BL / Retour → composant détruit
        ↓ destroyRef.onDestroy()
[6] invoke('stop_scanner_listener') → port libéré ✅
    Autre logiciel peut l'utiliser (caisse, Winpharma…)
```

**Pourquoi pas au démarrage de l'app :**

| Moment | Problème |
|---|---|
| Démarrage app | Port bloqué toute la session — conflit avec d'autres logiciels |
| Entrée liste commandes | Pas de `commandeId` pour `scanReception()` |
| `ngOnInit` du BL | ✅ Optimal — commande disponible, cycle clair |

---

### Gestion des erreurs et fallback automatique

```typescript
private async setupBarcodeScanner(): Promise<void> {
  if (this.tauriPrinterService.isRunningInTauri()) {
    const scannerPort = localStorage.getItem('reception-scanner-port');
    if (scannerPort) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const { listen }  = await import('@tauri-apps/api/event');

        await invoke('start_scanner_listener', { portName: scannerPort, baudRate: 9600 });
        this.scannerMode.set('SERIAL');

        const unlistenScan = await listen<{ code: string }>('scan-reception', event => {
          this.ngZone.run(() => { this.scanValue = event.payload.code; this.onScanReception(); });
        });
        // Débranché en cours de session → fallback silencieux
        const unlistenErr = await listen<string>('scan-reception-error', () => {
          this.ngZone.run(() => {
            this.notificationService.warn('Scanner déconnecté — mode clavier actif', 'Scanner');
            this.scannerMode.set('HID');
            this.setupHidFallback();
          });
        });
        this.destroyRef.onDestroy(async () => {
          unlistenScan(); unlistenErr();
          try { await invoke('stop_scanner_listener'); } catch { /* déjà fermé */ }
        });
        return;
      } catch {
        // Port occupé ou absent → fallback transparent
      }
    }
  }
  this.setupHidFallback();
}
```

---

### Tableau récapitulatif — douchette USB selon le mode

| Mode USB | Connexion logicielle | Config scanner | Pollution inputs | Code requis |
|---|---|---|---|---|
| **USB HID + TIMING** | `keydown` events | Aucune | ⚠️ Partielle (corrigé via preventDefault) | Déjà fait |
| **USB HID + STX/ETX** | `keydown` events | Programmer 1 fois | ✅ Non | Minimal |
| **USB CDC (Virtual COM)** | Tauri serialport | Programmer 1 fois | ✅ Non | ~50 lignes Rust |
| **WebHID direct** | `hid.requestDevice()` | Aucune | ✅ Non | ~30 lignes JS |

**Recommandation pratique pour une officine avec douchette USB neuve :**

```
1. Tester en mode TIMING d'abord (zéro config) — si ça suffit, s'arrêter là
2. Si faux positifs → programmer STX/ETX (scan d'une étiquette dans le manuel)
3. Si conflit clavier persiste → reprogrammer la douchette en USB CDC
   puis activer Tauri Serial Port (priorité 2 — ~50 lignes Rust, déjà planifié)

Note : Web Serial API navigateur → non retenu (cible Tauri desktop uniquement)
```

---

## 10. Mise à jour de codes CIP/EAN à la réception

> **📋 Note préliminaire — Audit du code réel (29 avril 2026)**
>
> Avant de proposer quoi que ce soit, le code source du composant `commande-received.component.ts`
> a été audité. Il révèle un mécanisme **déjà implémenté et bien conçu** pour les lignes
> provisoires. La section 10.3 redéfinit donc le vrai périmètre des lacunes.


### 10.1 Contexte & cas d'usage réels

Lors d'une réception de bon de livraison, le pharmacien ou le préparateur peut rencontrer le
cas où le **code-barres scanné sur la boîte physique ne correspond à aucune ligne de la commande**
— non pas parce que le produit est absent, mais parce que le **code a changé entre la commande
et la livraison**.

Les scénarios courants en officine française / africaine :

| Scénario | Cause | Fréquence |
|---|---|---|
| **Nouveau conditionnement** | Le labo change l'emballage (boîte de 30 → 28 cp) → nouveau CIP13 | ⭐⭐⭐ Très fréquent |
| **Changement de titulaire AMM** | Rachat d'un générique → nouveau titulaire → nouveau CIP | ⭐⭐ Fréquent |
| **Substitution fournisseur** | Grossiste livre l'équivalent générique avec CIP différent du princeps commandé | ⭐⭐⭐ Très fréquent |
| **GTIN GS1 non enregistré** | DataMatrix contient un GTIN-14 qui ne figure pas dans la fiche produit (ex : GTIN unité vs GTIN boîte) | ⭐⭐ Fréquent |
| **Produit hors catalogue** | Code totalement inconnu — bonus fournisseur, produit parapharmaceutique non référencé | ⭐ Occasionnel |
| **Erreur de saisie à la commande** | CIP mal saisi lors de la création de la commande | ⭐ Rare |

---

### 10.2 Mécanisme existant dans Pharma-Smart — Audit du code réel

Après lecture complète de `commande-received.component.ts`, le mécanisme est
**déjà bien structuré** autour du champ `provisionalCode` de l'entité `OrderLine`.

#### Ce qui est implémenté ✅

**Entité (`OrderLine.java`) :**
```java
@Column(name = "provisional_code")
private Boolean provisionalCode = Boolean.FALSE;
```

**Mode grille — colonne `produitCip` AG Grid :**
```typescript
// Éditable UNIQUEMENT si la ligne est provisoire
editable: (p: any) => !!p.data?.provisionalCode,

// Rendu visuel : italique orange + icône ⚠ + tooltip explicatif
cellRenderer: (p: any) => {
  if (p.data.provisionalCode) {
    return `<span style="font-style:italic;color:#856404">
              <i class="pi pi-exclamation-circle"></i>${cip || "Cliquer pour saisir"}
            </span>`;
  }
  return cip; // code normal
}
```

**`rowClassRules` — ligne entière colorée en fond jaune :**
```typescript
"pharma-row-provisional": p => !p.data?.__type && !!p.data?.provisionalCode,
```

**Filtre dédié :**
```typescript
{ label: "Code cip à mettre à jour", value: "PROVISOL_CIP" }
// → le pharmacien peut isoler toutes les lignes en attente de CIP
```

**`onCellValueChanged()` — deux comportements selon `provisionalCode` :**
```typescript
} else if (field === "produitCip") {
  if (!line.provisionalCode && newCip !== oldCip) {
    // Substitution sur ligne NON provisoire → confirmation dialog
    this.confirmDialog.onConfirm(
      () => { line.produitCip = newCip; this.commandeService.updateCip(line)... },
      "Substitution détectée",
      `Le CIP reçu (${newCip}) diffère du CIP commandé (${oldCip}).\nAccepter la substitution ?`,
      ...
      () => { line.produitCip = oldCip; this.gridApi?.refreshCells(...) }  // annuler
    );
  } else {
    // Ligne provisoire → mise à jour directe sans confirmation
    this.commandeService.updateCip(line).subscribe(() => this.refreshCommande());
  }
}
```

#### Synthèse : le mécanisme `provisionalCode` est-il user-friendly ?

**✅ Oui — pour le cas nominal (ouverture du BL, lignes provisoires visibles) :**

| Atout | Détail |
|---|---|
| Identification visuelle immédiate | Fond ligne jaune (`pharma-row-provisional`) + CIP italique orange + icône `⚠` |
| `singleClickEdit: true` | Un seul clic sur la cellule CIP ouvre l'éditeur — pas de double-clic |
| Scan utilisable dans la cellule | Une fois la cellule CIP en mode édition, le pharmacien peut scanner directement → le code s'injecte |
| Filtre PROVISOL_CIP | Isolation des lignes problématiques en 1 clic |
| Substitution sécurisée | Confirmation dialog avec ancien/nouveau CIP pour les lignes non provisoires |
| Aucune modale externe | Tout se passe en inline dans la grille — pas de changement de contexte |

**⚠️ Friction UX identifiée — cas scan avec code inconnu :**

Le seul point de friction réel est le **pont entre un scan `found = false` et les lignes
provisoires existantes**. Le flux actuel :

```
Pharmacien scanne une boîte dont le CIP a changé
         ↓
deliveryService.scanReception() → found = false
         ↓
Badge rouge "Produit non trouvé"
         ↓
❌ Aucune piste vers les lignes provisoires
         ↓
Le pharmacien doit :
  1. Remarquer le badge rouge
  2. Se souvenir du code scanné
  3. Trouver la ligne provisoire (scroll ou filtre PROVISOL_CIP)
  4. Cliquer sur la cellule CIP en mode édition
  5. RE-SCANNER ou re-taper le code manuellement
```

**Le scan a "perdu" le code.** L'opérateur doit scanner deux fois le même produit.

---

### 10.3 Comparaison avec les PMS concurrents — ce besoin est-il géré ?

| PMS | Lignes provisoires colorées | Édition CIP inline grille | Scan → auto-association | Substitution confirmée |
|---|---|---|---|---|
| **Pharmagest Néo** | ✅ | ✅ pop-up | ✅ popup résolution lors du scan | ✅ |
| **Winpharma** | ✅ (colonne "CIP reçu") | ✅ | ❌ repasse par un formulaire | ✅ |
| **LGPI** | ✅ | ✅ | ✅ recherche catalogue auto | ✅ |
| **Pharmavitale** | ⚠️ Indicateur texte | ❌ modal séparé | ❌ | ✅ |
| **Pharma-Smart actuel** | ✅ (`pharma-row-provisional`) | ✅ (`singleClickEdit`) | ❌ **Manquant** | ✅ (dialog) |

> **Conclusion comparative :** le mécanisme d'édition inline AG Grid de Pharma-Smart est
> **au niveau ou supérieur** à Winpharma et Pharmavitale. La seule lacune par rapport aux
> leaders (Pharmagest, LGPI) est l'**auto-association scan → ligne provisoire**.

---

### 10.4 La seule vraie lacune — AX-23 : Pont Scan-to-Provisional

Le mécanisme `provisionalCode` est conservé tel quel. La seule amélioration nécessaire est
**minimaliste** : quand un scan retourne `found = false`, détecter s'il existe des lignes
provisoires dans le BL et proposer une association directe.

#### Comportement cible

```
Pharmacien scanne une boîte (CIP a changé)
          ↓
found = false
          ↓
Pharma-Smart vérifie : y a-t-il des lignes provisionalCode = true dans ce BL ?
          ↓
┌─ Oui (cas fréquent) ──────────────────────────────────────────────────────────┐
│  Badge scan passe en ORANGE (pas rouge) :                                     │
│  ⚠ Code 3400935782799 inconnu — 2 ligne(s) provisoire(s) en attente de CIP   │
│  [→ Associer à la ligne provisoire]  ← bouton d'action dans le badge          │
│                                                                               │
│  Clic sur le bouton :                                                         │
│  1. Auto-filtre PROVISOL_CIP activé (ou gridApi.ensureNodeVisible())          │
│  2. gridApi.startEditingCell({ rowIndex, colKey: 'produitCip' })              │
│  3. Le scanned code pré-rempli dans la cellule (input de l'éditeur)           │
│  4. Le pharmacien voit la cellule CIP ouverte avec 3400935782799 déjà dedans │
│  5. Entrée → updateCip() → BL à jour                                         │
└───────────────────────────────────────────────────────────────────────────────┘
┌─ Non (aucune ligne provisoire) ───────────────────────────────────────────────┐
│  Badge rouge inchangé : ❌ Produit non trouvé dans ce BL                      │
│  + option [➕ Ajouter en hors-commande] ← AX-18                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

#### Implémentation — modification minimale de `onScanReception()`

```typescript
// Dans commande-received.component.ts — onScanReception()
protected onScanReception(): void {
  const raw = this.scanValue?.trim();
  if (!raw) return;
  this.scanValue = "";
  this.deliveryService.scanReception(this.currentCommande.id, raw).subscribe({
    next: res => {
      const result = res.body!;

      // NOUVEAU : si non trouvé, enrichir le résultat avec les lignes provisoires
      if (!result.found) {
        const provisionalLines = this.orderLines.filter(l => l.provisionalCode);
        result.provisionalLines = provisionalLines.map(l => ({ id: l.id!, libelle: l.produitLibelle! }));
        result.scannedCode = raw; // conserver le code scanné pour pré-remplissage
      }

      this.lastScanResult.set(result);
      // ...reste du code inchangé...
    }
  });
}

// Méthode d'association scan → première ligne provisoire
protected onAssocierScanToProvisional(): void {
  const scan = this.lastScanResult();
  if (!scan?.provisionalLines?.length || !scan.scannedCode) return;

  // Naviguer vers la première ligne provisoire
  const firstProvisionalId = scan.provisionalLines[0].id;
  const rowNode = this.gridApi?.getRowNode(String(firstProvisionalId));
  if (!rowNode) return;

  this.gridApi?.ensureNodeVisible(rowNode, 'middle');

  // Démarrer l'édition de la cellule CIP avec le code scanné pré-rempli
  setTimeout(() => {
    const rowIndex = rowNode.rowIndex;
    if (rowIndex == null) return;
    this.gridApi?.startEditingCell({ rowIndex, colKey: 'produitCip' });
    // Pré-remplir la valeur dans l'éditeur AG Grid
    setTimeout(() => {
      const eInput = document.querySelector<HTMLInputElement>('.ag-cell-editor input');
      if (eInput) {
        eInput.value = scan.scannedCode!;
        eInput.dispatchEvent(new Event('input'));
      }
    }, 50);
  }, 100);

  this.lastScanResult.set(null);
}
```

#### Modification HTML — badge scan enrichi

```html
<!-- Remplacement du bloc @if (!lastScanResult()!.found) dans la barre de scan -->
@if (!lastScanResult()!.found) {
  @if (lastScanResult()!.provisionalLines?.length) {
    <!-- Cas : code inconnu + lignes provisoires disponibles → badge orange + action -->
    <span class="cr-scan-badge cr-scan-badge--provisional">
      <i class="pi pi-exclamation-triangle"></i>
      Code inconnu —
      {{ lastScanResult()!.provisionalLines!.length }} ligne(s) provisoire(s) en attente
      <button type="button" class="cr-scan-provisional-btn"
              (click)="onAssocierScanToProvisional()">
        <i class="pi pi-link"></i> Associer
      </button>
    </span>
  } @else {
    <!-- Cas : code truly inconnu, aucune ligne provisoire → badge rouge existant -->
    <span class="cr-scan-badge cr-scan-badge--err">
      <i class="pi pi-times-circle"></i>
      {{ lastScanResult()!.warningMessage }}
    </span>
  }
}
```

**Impact total :**
- ~35 lignes TypeScript dans le composant existant
- ~12 lignes HTML dans le template existant
- Aucun nouveau composant, aucun endpoint backend, aucune migration
- Mécanisme `provisionalCode` + colonne éditable AG Grid : **inchangés**

---

### 10.5 Mode séquentiel — gap spécifique

Le mode séquentiel n'a pas le même mécanisme visuel car chaque ligne est traitée seule.
Quand la ligne courante a `provisionalCode = true`, il faut :

1. Afficher un badge "Code CIP provisoire" dans le `product-header` (informatif)
2. Si un scan `found = false` arrive sur cette ligne → proposer directement
   *"Associer ce code scanné à ce produit ?"* sans quitter l'étape qty

```typescript
// Dans reception-sequential.component.ts — effect sur lastScanResult()
effect(() => {
  const scan = this.lastScanResult();
  untracked(() => {
    if (!scan || scan.found) return;
    // Si la ligne courante est provisoire → proposer l'association directe
    if (this.currentLine()?.provisionalCode && scan.scannedCode) {
      this.pendingCipAssociation.set(scan.scannedCode);
    }
  });
});
```

```html
<!-- Dans product-header — alerte CIP provisoire -->
@if (currentLine()?.provisionalCode) {
  <div class="rh-seq__provisional-cip-banner">
    <i class="pi pi-exclamation-triangle"></i>
    <span>Code CIP provisoire — à mettre à jour</span>
    @if (pendingCipAssociation()) {
      <span class="rh-seq__provisional-scan-code">
        Code scanné : <strong>{{ pendingCipAssociation() }}</strong>
      </span>
      <button pButton type="button" size="small" severity="warning"
              icon="pi pi-check" label="Associer ce code"
              (click)="onConfirmCipAssociation()"></button>
    } @else {
      <button pButton type="button" size="small" severity="secondary"
              icon="pi pi-pencil" label="Modifier CIP"
              (click)="isEditingCip.set(true)"></button>
    }
  </div>
}
```

---

### 10.6 Révision du plan d'amélioration — AX-23 réduit

Maintenant que l'audit du code est fait, AX-23 se réduit à l'essentiel :

| ID | Intitulé | Travail réel | Priorité |
|---|---|---|---|
| **AX-23a** | Mode grille : enrichir `onScanReception()` pour détecter les lignes provisoires et stocker `scannedCode` + `provisionalLines` dans `lastScanResult` | ~15 lignes TS | 🔴 Haute |
| **AX-23b** | Mode grille : badge orange + bouton "Associer" dans le badge scan | ~12 lignes HTML | 🔴 Haute |
| **AX-23c** | Mode grille : méthode `onAssocierScanToProvisional()` → scroll + startEditingCell + pré-remplissage | ~20 lignes TS | 🔴 Haute |
| **AX-23d** | Mode séquentiel : banner `provisionalCode` dans `product-header` + association scan | ~25 lignes TS + HTML | 🟡 Moyenne |
| ~~AX-23e~~ | ~~Flag "CIP mis à jour"~~ | ~~Déjà implicite via `updated = true` en backend~~ | ~~Supprimé~~ |
| ~~AX-23f~~ | ~~Recherche catalogue multi-code~~ | Peut rester als — hors scope immédiat | 🟢 Basse |
| **AX-23g** | Rapport "lignes CIP mises à jour" dans `ReceptionFinalizeModal` | ~10 lignes | 🟢 Basse |

> **⏱ Effort total AX-23 révisé : ~70 lignes de code** (vs la proposition initiale d'un
> nouveau composant + endpoint backend). Le mécanisme `provisionalCode` + cellule AG Grid
> éditable est **conservé tel quel** — c'est la bonne approche.

---

### 10.7 Synthèse comparative révisée

| Fonctionnalité | Pharmagest | Winpharma | LGPI | Pharmavitale | **Pharma-Smart actuel** | **Pharma-Smart cible** |
|---|---|---|---|---|---|---|
| Lignes provisoires colorées | ✅ | ✅ | ✅ | ⚠️ | ✅ (fond jaune + italic) | ✅ Inchangé |
| Édition CIP inline grille | ✅ | ✅ | ✅ | ❌ | ✅ (singleClickEdit) | ✅ Inchangé |
| Scan → auto-association ligne provisoire | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ AX-23a/b/c |
| Substitution avec confirmation | ✅ | ✅ | ✅ | ✅ | ✅ (dialog) | ✅ Inchangé |
| Filtre "lignes CIP à mettre à jour" | ✅ | ✅ | ✅ | ❌ | ✅ (PROVISOL_CIP) | ✅ Inchangé |
| CIP provisoire visible en séquentiel | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ AX-23d |

> **Verdict final :** Le mécanisme `provisionalCode` + AG Grid éditable est **user-friendly,
> bien pensé, et conservé intégralement**. L'unique ajout nécessaire est le pont
> Scan-to-Provisional (~70 lignes) pour éviter que le pharmacien re-scanne la même boîte.


---

*Document généré automatiquement — référence projet : Pharma-Smart*

