# Plan d'intégration — Réception des bons de commande

**Date :** 2026-03-28
**Branche :** `refacto_commande_en_cours`

---

## 1. Diagnostic de l'état actuel

### 1.0 Infrastructure DataMatrix — déjà prête

Un service complet de parsing de codes-barres pharmaceutiques est présent mais **non encore
branché** dans la chaîne réception :

| Composant | Fichier | État |
|---|---|---|
| `DataMatrixParserService` (interface) | `service/stock/DataMatrixParserService.java` | ✅ Défini |
| `DataMatrixParserServiceImpl` | `service/stock/impl/DataMatrixParserServiceImpl.java` | ✅ Implémenté `@Service` |
| `DataMatrixInfo` (DTO record) | `service/dto/DataMatrixInfo.java` | ✅ Défini |
| `Lot` entity | `domain/Lot.java` | ✅ Champs compatibles |
| `StockEntryResource` | `web/rest/commande/StockEntryResource.java` | ✅ Endpoints existants |

**Ce que `DataMatrixParserService.parse(rawScan)` retourne depuis un seul scan 2D :**
```
DataMatrixInfo {
  gtin          → GTIN-14 (AI 01)
  cip13         → CIP France (AI 711) ou extrait du GTIN
  ean13         → EAN-13 extrait
  batchNumber   → Numéro de lot (AI 10)      → Lot.numLot
  expiryDate    → Date péremption (AI 17)    → Lot.expiryDate
  manufacturingDate → Date fabrication (AI 11) → Lot.manufacturingDate
  serialNumber  → N° série FMD (AI 21)       → traçabilité
}
```

**Codes supportés :** EAN-8, EAN-13, CIP-7, CIP-13, GS1 DataMatrix (avec ou sans séparateurs GS1).

**Contexte réglementaire :** Depuis le 9 février 2019 (Directive FMD 2011/62/UE), chaque boîte
pharmaceutique porte un DataMatrix GS1 obligatoire. Scanner le DataMatrix donne en une seule
opération : produit + lot + péremption + numéro de série — éliminant toute saisie manuelle de lot.

### 1.1 Onglets intégrés dans `suggestions-unified`

| Tab key | Composant monté | État |
|---|---|---|
| `REAPPRO` | `<app-suggestion-home />` | ✅ Intégré |
| `COMMANDES_A_PASSER` | `<app-commande-requested-home />` | ✅ Intégré |
| `BONS_DE_LIVRAISON` | `<app-list-bons />` | ⚠️ Partiel — liste seule, pas de master/detail |
| `ANALYSE` | `<app-semois-suggestions />` | ✅ Intégré |

### 1.2 Composants existants et leur sort

| Composant | État | Décision |
|---|---|---|
| `commande-received` | Utilise `p-table` | ✅ À conserver — migrer vers AG Grid |
| `commande-received-home` | Master/detail RECEIVED opérationnel | ♻️ Logique absorbée par `list-bons` — obsolète |
| `reception-hub` | Tabs imbriqués ngbNav | ❌ Pattern abandonné — ne pas brancher |
| `list-bons` | Liste historique + filtres | ✅ À enrichir — devient le gestionnaire BL unifié |
| `ReceptionConcordanceComponent` | Concordance temps réel (stats computed) | ✅ Avantage différenciateur — à conserver et valoriser |

### 1.3 Points techniques confirmés à la lecture du code

- `previousState()` dans `commande-received.component.ts:131` appelle déjà `this.retour.emit()` — **aucune correction nécessaire** pour le mode embedded
- `ReceptionConcordanceComponent` calcule en temps réel : écarts quantité, écarts prix, lots manquants, montants commandés/reçus — composant de qualité, à conserver dans le layout split-panel
- `list-bons` a déjà `isReceived()`, le dropdown statut (Tous/RECEIVED/CLOSED), les filtres date/fournisseur/texte — base solide

---

## 2. Décision UX finale — Deux patterns distincts selon le statut

### 2.1 Abandon des tabs imbriqués (`reception-hub`)

Monter `reception-hub` (ngbNav niveau 2) dans l'onglet `BONS_DE_LIVRAISON` (tabs custom niveau 1) crée une structure tabs-dans-tabs qui viole les principes W3C ARIA et les standards industrie. Aucun logiciel de référence officine (Winpharma, LGPI, Alliadis, iSoft, SurOrdonnance) n'utilise ce pattern.

### 2.2 Pattern retenu — Nature de l'interaction comme critère

| Statut bon | Nature | Pattern UX | Référence industrie |
|---|---|---|---|
| **RECEIVED** | Travail actif — saisie quantités, gestion lots | **Remplacement complet** (liste → détail plein écran) | Winpharma, LGPI, iSoft |
| **CLOSED** | Consultation — lecture seule | **Split-panel** (gauche métadonnées + droite lignes) | Winpharma, LGPI, Alliadis |

Le split-panel CLOSED est le pattern universel pour la consultation de documents archivés.
Le remplacement complet RECEIVED est cohérent avec `commande-requested-home` déjà en place.

---

## 3. Architecture finale

### 3.1 Vue d'ensemble

```
suggestions-unified — tab BONS_DE_LIVRAISON
  └── <app-list-bons />   (inchangé dans suggestions-unified)
        │
        ├── MODE LISTE  [editingReceived === null && selectedClosed === null]
        │    ├── Filtres : [Statut ▼] [Fournisseur ▼] [Du…] [Au…] [Recherche]
        │    ├── Carte RECEIVED  [badge orange "En attente de saisie"]
        │    │    └── Bouton "Saisir ▶"  → MODE SAISIE
        │    └── Carte CLOSED   [badge vert "Clôturé"]
        │         └── Clic carte → MODE CONSULTATION
        │
        ├── MODE SAISIE  [editingReceived !== null]
        │    └── Full-panel (pattern commande-requested-home)
        │         └── <app-commande-received [commande]="editingReceived"
        │                                    (retour)="onRetourSaisie()"
        │                                    (commandeChange)="onCommandeChange($event)" />
        │              ├── Header : [Import] [Tout valider] [Valider] [Export]
        │              ├── Filtres/tri lignes
        │              ├── col-md-3 : concordance + détails fournisseur
        │              └── col-md-9 : AG Grid éditable (migration p-table)
        │
        └── MODE CONSULTATION  [selectedClosed !== null]
             └── Split-panel
                  ├── Bouton retour ["← Bons de livraison"]
                  ├── Gauche col-md-4 : concordance (read-only) + métadonnées bon
                  └── Droite col-md-8 : AG Grid lignes (read-only, no editable cells)
```

### 3.2 Signaux à ajouter dans `list-bons.component.ts`

```typescript
// Master/detail RECEIVED (remplacement complet)
readonly editingReceived = signal<ICommande | null>(null);

// Split-panel CLOSED (consultation)
readonly selectedClosed = signal<ICommande | null>(null);
```

### 3.3 Flux utilisateur complet

**Flux RECEIVED :**
```
Tab "Bons de livraison"
  → list-bons MODE LISTE (filtre "Tous" par défaut)
  → Clic "Saisir ▶" sur carte orange
  → deliveryService.find(commandeId) → editingReceived.set(commande)
  → MODE SAISIE : commande-received plein écran
  → Saisie quantités, gestion lots, validation
  → "Valider" → finalisation → bon passe CLOSED
  → retour.emit() → onRetourSaisie() → editingReceived.set(null) + onSearch()
  → MODE LISTE : bon réappraît en vert "Clôturé"
```

**Flux CLOSED :**
```
Tab "Bons de livraison"
  → list-bons MODE LISTE
  → Clic sur carte verte
  → deliveryService.find(commandeId) → selectedClosed.set(commande)
  → MODE CONSULTATION : split-panel
     Gauche : concordance stats + fournisseur + référence + montants
     Droite : AG Grid read-only des lignes
  → Bouton "← Retour" → selectedClosed.set(null)
  → MODE LISTE
```

---

## 4. Plan de réalisation par sprint

---

### Sprint R-1 — `list-bons` : master/detail RECEIVED + split-panel CLOSED

**Durée estimée : 2–3h**
**Fichiers : `list-bons.component.ts`, `list-bons.component.html`**

#### R-1.1 — Modifications `list-bons.component.ts`

Ajouter les imports :
```typescript
import { signal } from '@angular/core';
import { DestroyRef, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeReceivedComponent } from '../../feature/commande-received/commande-received.component';
```

Ajouter dans la classe :
```typescript
readonly editingReceived = signal<ICommande | null>(null);
readonly selectedClosed  = signal<ICommande | null>(null);
private readonly destroyRef = inject(DestroyRef);

onEditerReceived(delivery: IDelivery): void {
  this.entityService.find(delivery.commandeId)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({ next: res => { if (res.body) this.editingReceived.set(res.body as unknown as ICommande); } });
}

onOuvrirClosed(delivery: IDelivery): void {
  this.entityService.find(delivery.commandeId)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({ next: res => { if (res.body) this.selectedClosed.set(res.body as unknown as ICommande); } });
}

onRetourSaisie(): void {
  this.editingReceived.set(null);
  this.onSearch();
}

onCommandeChange(c: ICommande | null): void {
  if (c) this.editingReceived.set(c);
  else this.onRetourSaisie();
}

onRetourConsultation(): void {
  this.selectedClosed.set(null);
}
```

Ajouter dans `imports[]` :
```typescript
CommandeReceivedComponent,
// + composants split-panel (ReceptionConcordanceComponent, AG Grid)
```

#### R-1.2 — Modifications `list-bons.component.html`

Structure générale :
```html
@if (editingReceived()) {

  <!-- ══ MODE SAISIE ══════════════════════════════════════════════════ -->
  <div class="view-panel view-panel--detail">
    <app-commande-received
      [commande]="editingReceived()!"
      (retour)="onRetourSaisie()"
      (commandeChange)="onCommandeChange($event)"
    />
  </div>

} @else if (selectedClosed()) {

  <!-- ══ MODE CONSULTATION (split-panel) ═══════════════════════════════ -->
  <div class="view-panel view-panel--split">

    <!-- Bouton retour -->
    <div class="sp-breadcrumb">
      <p-button (onClick)="onRetourConsultation()" icon="pi pi-arrow-left"
                label="Bons de livraison" [text]="true" size="small" />
    </div>

    <div class="sp-layout">
      <!-- Gauche : concordance + métadonnées -->
      <div class="sp-left">
        <app-reception-concordance
          [orderLines]="selectedClosed()!.orderLines ?? []"
          [showLotInfo]="false"
        />
        <!-- Carte métadonnées : fournisseur, référence, date, montants -->
        <div class="reception-summary-card">
          <!-- ... reprendre le contenu actuel du col-md-3 de commande-received ... -->
        </div>
      </div>
      <!-- Droite : AG Grid read-only -->
      <div class="sp-right">
        <ag-grid-angular
          [theme]="theme"
          [columnDefs]="closedColumnDefs"
          [rowData]="selectedClosed()!.orderLines ?? []"
          style="width: 100%; height: 100%;"
        />
      </div>
    </div>

  </div>

} @else {

  <!-- ══ MODE LISTE (contenu actuel) ═══════════════════════════════════ -->
  <div class="view-panel">
    <!-- filtres, cartes, pagination — inchangés -->

    <!-- Ajout sur carte RECEIVED : bouton "Saisir ▶" -->
    @if (isReceived(bon)) {
      <button (click)="onEditerReceived(bon); $event.stopPropagation()"
              class="bc__action bc__action--primary"
              pTooltip="Saisir la réception" tooltipPosition="top">
        <i class="pi pi-inbox"></i>
      </button>
    }

    <!-- Ajout sur carte CLOSED : clic → split-panel -->
    <!-- (le clic sur la carte entière appelle onOuvrirClosed(bon)) -->

  </div>
}
```

---

### Sprint R-2 — Migration AG Grid dans `commande-received`

**Durée estimée : 2–3h**
**Fichiers : `commande-received.component.ts`, `commande-received.component.html`**

#### R-2.1 — Cartographie des colonnes p-table → AG Grid

| Colonne | `field` | Éditable | Détail |
|---|---|---|---|
| `#` | — | Non | `valueGetter: p => p.node.rowIndex + 1` |
| Code CIP | `produitCip` | Si `provisionalCode === true` | `cellEditor: 'agTextCellEditor'` |
| Description | `produitLibelle` | Non | — |
| Stock init | `initStock` | Non | Formatter nombre |
| P.A | `orderCostAmount` | Non | Montant |
| P.A Machine | `costAmount` | Non | — |
| P.U | `orderUnitPrice` | Non | — |
| P.U Machine | `regularUnitPrice` | Non | — |
| **Marge %** | calculé | Non | `(PU - PA) / PU × 100` — **nouveau** |
| Qté cmdée | `quantityRequested` | Non | — |
| **Qté reçue** | `quantityReceivedTmp` | **Oui** | `cellEditor: 'agNumberCellEditor'` |
| **Qté Ug** | `freeQty` | **Oui** | `cellEditor: 'agNumberCellEditor'` |
| Stock après | calculé | Non | `valueGetter: computeAfterStock(data)` |
| Statut | calculé | Non | `cellRenderer` badge CSS |
| Lots | `lots` | Non | Conditionnel `showLotColumn()` |
| Actions | — | Non | `cellClicked` dispatcher |

#### R-2.2 — Row coloring `rowClassRules`

```typescript
protected rowClassRules = {
  'row-price-variation': (p: RowClassParams) => this.orderLineTableColor(p.data) === 'row-price-variation',
  'row-qty-partial':     (p: RowClassParams) => this.orderLineTableColor(p.data) === 'row-qty-partial',
  'row-excess':          (p: RowClassParams) => this.orderLineTableColor(p.data) === 'row-excess',
};
```

#### R-2.3 — Actions par ligne (`cellClicked` dispatcher)

Pattern identique à `commande-requested` :
```typescript
onCellClicked(event: CellClickedEvent): void {
  const line = event.data as IOrderLine;
  switch (event.colDef.colId) {
    case 'action-history':   this.onShowPriceHistory(line); break;
    case 'action-edit':      this.editLigneInfos(line);     break;
    case 'action-lot':       this.onAddLot(line);           break;
    case 'action-delete':    this.confirmDeleteItem(line);  break;
    case 'action-etiquette': this.printEtiquetteForLine(line); break; // P4
  }
}
```

#### R-2.4 — Mise à jour de ligne après édition (`applyTransaction`)

```typescript
this.deliveryService.updateQuantityReceived(line).subscribe({
  next: res => this.gridApi?.applyTransaction({ update: [res.body] }),
});
```

#### R-2.5 — Footer totaux

Div `grid-caption` sous la grille (pattern `commande-requested`) :
```html
<div class="grid-caption">
  <span class="grid-caption__count">{{ orderLines.length }} ligne(s)</span>
  <span class="grid-caption__totals ms-auto">
    Achat : <strong>{{ currentCommande.grossAmount | number }} F</strong>
    &nbsp;·&nbsp;
    Vente : <strong>{{ currentCommande.orderAmount | number }} F</strong>
  </span>
</div>
```

#### R-2.6 — Colonne Lots conditionnelle

```typescript
effect(() => {
  this.columnDefs = this.buildColumnDefs(this.showLotColumn());
});
```

#### R-2.7 — Imports à ajouter / supprimer

Ajouter :
```typescript
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridApi, GridReadyEvent, CellClickedEvent,
         CellValueChangedEvent, RowClassParams, themeQuartz } from 'ag-grid-community';
```

Supprimer : `TableModule`, `ButtonGroup` (plus utilisés).

---

### Sprint R-3 — Scan DataMatrix dans la réception

**Durée estimée : 4–5h (backend + frontend)**
**Priorité : 🟠 Fort — gain opérationnel majeur + conformité FMD**

Ce sprint remplace l'approche CIP-only initialement prévue par une intégration complète du
`DataMatrixParserService` déjà implémenté. Le backend parse tous les formats (1D et 2D) via un
seul endpoint. Un scan 2D crée automatiquement le lot sans saisie manuelle.

---

#### R-3.1 — Trois scénarios de scan

| Type de scan | Douchette | CIP | Lot auto | Péremption auto | FMD |
|---|---|---|---|---|---|
| CIP-7 / CIP-13 / EAN-13 (1D) | Linéaire | ✅ | ❌ | ❌ | ❌ |
| DataMatrix sans AI lot (2D) | 2D | ✅ | ❌ | ❌ | ✅ serial |
| DataMatrix complet (2D) | 2D | ✅ | **✅ auto** | **✅ auto** | ✅ serial |

---

#### R-3.2 — Backend : nouveau DTO `ReceptionScanResultDTO`

```java
// service/dto/ReceptionScanResultDTO.java
public record ReceptionScanResultDTO(
    boolean found,               // ligne commande identifiée ?
    Long orderLineId,            // id de la ligne mise à jour
    String produitLibelle,       // nom produit (feedback UI)
    String produitCip,           // CIP identifié
    boolean lotAutoCreated,      // lot créé automatiquement depuis DataMatrix ?
    String lotNumero,            // numLot auto-créé (si lotAutoCreated)
    LocalDate lotPeremption,     // date péremption auto-remplie
    String warningMessage,       // substitution, CIP absent, etc.
    DataMatrixParserService.BarcodeType barcodeType  // type détecté
) {}
```

---

#### R-3.3 — Backend : méthode service `processScanReception()`

À ajouter dans `StockEntryService` (interface + impl) :

```java
// Interface
ReceptionScanResultDTO processScanReception(Long commandeId, String rawScan);

// Implémentation dans StockEntryServiceImpl
@Transactional
public ReceptionScanResultDTO processScanReception(Long commandeId, String rawScan) {

    // 1. Parser le code scanné (tous formats : 1D et 2D)
    Optional<DataMatrixInfo> parsedOpt = dataMatrixParserService.parse(rawScan);
    if (parsedOpt.isEmpty()) {
        return new ReceptionScanResultDTO(false, null, null, null,
            false, null, null, "Format de code non reconnu",
            DataMatrixParserService.BarcodeType.UNKNOWN);
    }
    DataMatrixInfo parsed = parsedOpt.get();
    String cip = parsed.getProductCode();  // CIP13 > EAN13 > GTIN

    // 2. Identifier la ligne commande
    OrderLine line = orderLineRepository
        .findByCommandeIdAndCip(commandeId, cip)
        .orElse(null);

    if (line == null) {
        return new ReceptionScanResultDTO(false, null, null, cip,
            false, null, null,
            "CIP " + cip + " absent de la commande",
            dataMatrixParserService.detectBarcodeType(rawScan));
    }

    // 3. Incrémenter la quantité reçue de 1
    int newQty = (line.getQuantityReceived() == null ? 0 : line.getQuantityReceived()) + 1;
    line.setQuantityReceived(newQty);
    updateItem(line);  // méthode existante dans StockEntryServiceImpl

    // 4. Créer le lot automatiquement si DataMatrix contient batch + péremption
    boolean lotCreated = false;
    String lotNumero = null;
    LocalDate lotPeremption = null;

    boolean gestionLotActif = configurationService
        .getParamAsBoolean(Params.APP_GESTION_LOT);

    if (gestionLotActif && parsed.hasBatchInfo() && parsed.hasExpiryDate()) {
        LotDTO lotDto = new LotDTO();
        lotDto.setOrderLineId(line.getId());
        lotDto.setNumLot(parsed.batchNumber());
        lotDto.setExpiryDate(parsed.expiryDate());
        if (parsed.manufacturingDate() != null)
            lotDto.setManufacturingDate(parsed.manufacturingDate());
        lotDto.setQuantity(1);
        lotService.addLot(lotDto);  // service existant

        lotCreated = true;
        lotNumero = parsed.batchNumber();
        lotPeremption = parsed.expiryDate();
    }

    return new ReceptionScanResultDTO(
        true,
        line.getId(),
        line.getProduit() != null ? line.getProduit().getLibelle() : null,
        cip,
        lotCreated,
        lotNumero,
        lotPeremption,
        null,
        dataMatrixParserService.detectBarcodeType(rawScan)
    );
}
```

**Injection à ajouter dans `StockEntryServiceImpl` :**
```java
private final DataMatrixParserService dataMatrixParserService;
private final LotService lotService;
private final ConfigurationService configurationService;
```

---

#### R-3.4 — Backend : nouvel endpoint REST

Dans `StockEntryResource.java` :

```java
@PostMapping("/commandes/entree-stock/scan-reception")
public ResponseEntity<ReceptionScanResultDTO> scanReception(
    @RequestParam Long commandeId,
    @RequestBody String rawScan
) {
    ReceptionScanResultDTO result = stockEntryService.processScanReception(commandeId, rawScan);
    return ResponseEntity.ok(result);
}
```

---

#### R-3.5 — Frontend : modèle `IReceptionScanResult`

```typescript
// shared/model/reception-scan-result.model.ts
export interface IReceptionScanResult {
  found: boolean;
  orderLineId?: number;
  produitLibelle?: string;
  produitCip?: string;
  lotAutoCreated: boolean;
  lotNumero?: string;
  lotPeremption?: string;       // ISO date string
  warningMessage?: string;
  barcodeType: 'EAN_8' | 'EAN_13' | 'CIP_7' | 'CIP_13' | 'DATAMATRIX' | 'UNKNOWN';
}
```

---

#### R-3.6 — Frontend : méthode `scanReception()` dans `delivery.service.ts`

```typescript
scanReception(commandeId: number, rawScan: string): Observable<IReceptionScanResult> {
  return this.http.post<IReceptionScanResult>(
    `${this.resourceUrl}/entree-stock/scan-reception`,
    rawScan,
    {
      headers: { 'Content-Type': 'text/plain' },
      params: { commandeId: String(commandeId) },
    }
  );
}
```

---

#### R-3.7 — Frontend : champ scan dans `commande-received.component.html`

Ajouter dans la barre de filtres, entre les filtres existants et les boutons d'action :

```html
<!-- Champ scan dédié — barre outils commande-received -->
<div class="scan-reception-field">
  <p-iconfield>
    <p-inputicon class="pi pi-qrcode" />
    <input
      #scanInputRef
      (keyup.enter)="onScanReception()"
      [(ngModel)]="scanValue"
      pInputText
      placeholder="Scanner CIP ou DataMatrix…"
      class="scan-input"
      autocomplete="off"
    />
  </p-iconfield>

  <!-- Feedback dernier scan -->
  @if (lastScanResult()) {
    <div class="scan-feedback"
         [class.scan-feedback--ok]="lastScanResult()!.found"
         [class.scan-feedback--warn]="!lastScanResult()!.found">
      @if (lastScanResult()!.found) {
        <i class="pi pi-check-circle"></i>
        <span>{{ lastScanResult()!.produitLibelle }}</span>
        @if (lastScanResult()!.lotAutoCreated) {
          <span class="scan-lot-badge">
            <i class="pi pi-box"></i>
            Lot {{ lastScanResult()!.lotNumero }}
            · {{ lastScanResult()!.lotPeremption | date:'MM/yyyy' }}
          </span>
        }
        @if (lastScanResult()!.barcodeType === 'DATAMATRIX') {
          <span class="scan-type-badge scan-type-badge--2d">2D</span>
        }
      } @else {
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ lastScanResult()!.warningMessage }}</span>
      }
    </div>
  }
</div>
```

---

#### R-3.8 — Frontend : logique dans `commande-received.component.ts`

```typescript
// Nouveaux signaux
protected scanValue = '';
readonly lastScanResult = signal<IReceptionScanResult | null>(null);
private scanFeedbackTimer: ReturnType<typeof setTimeout> | null = null;

onScanReception(): void {
  const raw = this.scanValue.trim();
  if (!raw) return;

  this.deliveryService.scanReception(this.currentCommande.id, raw).subscribe({
    next: (result: IReceptionScanResult) => {
      this.scanValue = '';           // vider immédiatement le champ
      this.lastScanResult.set(result);

      // Effacer le feedback après 3 secondes
      if (this.scanFeedbackTimer) clearTimeout(this.scanFeedbackTimer);
      this.scanFeedbackTimer = setTimeout(() => this.lastScanResult.set(null), 3000);

      if (result.found) {
        // Recharger la ligne mise à jour dans la grille
        const updatedLine = this.orderLines.find(l => l.id === result.orderLineId);
        if (updatedLine) {
          updatedLine.quantityReceivedTmp = (updatedLine.quantityReceivedTmp ?? 0) + 1;
          this.orderLines = [...this.orderLines]; // déclenche la détection de changement
          this.gridApi?.applyTransaction({ update: [updatedLine] });
          this.gridApi?.ensureNodeVisible(
            this.gridApi.getRowNode(String(result.orderLineId))
          );
        }

        // Si DataMatrix avec lot → la grille doit afficher le lot auto-créé
        // (recharger les lignes si lot créé pour rafraîchir la colonne Lots)
        if (result.lotAutoCreated) {
          this.onFilterCommandeLines(); // recharge lignes avec lots depuis backend
        }

      } else {
        // CIP absent de la commande → notification + alerte substitution si CIP connu
        this.notificationService.warn(result.warningMessage ?? 'Produit non trouvé', 'Scan');
      }
    },
    error: () => {
      this.scanValue = '';
      this.notificationService.error('Erreur lors du traitement du scan', 'Scan');
    },
  });
}
```

---

#### R-3.9 — Intégration avec `ScanDetectorService` (douchette hardware)

`commande-product-search` utilise déjà `ScanDetectorService.onScanEvent$` pour les douchettes
hardware. Le champ scan de réception doit écouter le même service quand la vue est active :

```typescript
// Dans ngOnInit() de commande-received.component.ts
private readonly scanDetector = inject(ScanDetectorService);

ngOnInit(): void {
  // ... code existant ...

  // Écoute des scans douchette hardware (en plus de la saisie clavier)
  this.scanDetector.onScanEvent$
    .pipe(
      filter(e => e.type === 'complete'),
      takeUntilDestroyed(this.destroyRef)
    )
    .subscribe(e => {
      this.scanValue = e.code;
      this.onScanReception();
    });
}
```

---

### Sprint R-4 — Proposition automatique de reliquat à la finalisation

**Durée estimée : 1–2h**
**Fichiers : `commande-received.component.ts`**
**Priorité : 🟠 Fort — fonctionnalité attendue par tous les logiciels référence**

#### Point d'insertion

Dans la méthode `onFinalize()`, après l'appel à `finalizeSaisieEntreeStock()`, détecter les lignes
partielles et proposer la création d'un reliquat :

```typescript
private checkReliquat(): void {
  const lignesPartielles = this.orderLines.filter(l =>
    (l.quantityReceivedTmp ?? 0) < (l.quantityRequested ?? 0)
  );
  if (lignesPartielles.length === 0) return;

  const totalManquant = lignesPartielles.reduce(
    (sum, l) => sum + ((l.quantityRequested ?? 0) - (l.quantityReceivedTmp ?? 0)), 0
  );

  this.confirmDialog.confirm({
    message: `${lignesPartielles.length} article(s) non servis (${totalManquant} unités manquantes).
               Créer un reliquat automatique ?`,
    header: 'Articles manquants',
    accept: () => this.commandeService.createReliquat(this.currentCommande.id).subscribe({
      next: reliquat => this.notificationService.success(
        `Reliquat #${reliquat.id} créé`, 'Reliquat'
      ),
    }),
  });
}
```

**Prérequis backend :** Endpoint `POST /api/commandes/{id}/reliquat` (à vérifier s'il existe déjà
via `reliquatDeCommandeId` dans le modèle).

---

### Sprint R-5 — Taux de service fournisseur + alerte substitution

**Durée estimée : 3h (frontend + backend)**
**Priorité : 🟡 Modéré**

#### R-5.1 — Taux de service dans le header de `commande-received`

Ajouter dans `commande-status-bar` ou en dessous :

```
[Cerp Rhin]   Taux de service : 94.2% ↑   Délai moyen : 1.2j
```

Nécessite un endpoint backend : `GET /api/fournisseurs/{id}/stats-service`
Retourne : `{ tauxService: 94.2, delaiMoyen: 1.2, periodeJours: 30 }`

#### R-5.2 — Alerte substitution produit

Dans `onUpdateCip()` et lors de l'import EDI, si CIP reçu ≠ CIP commandé (et `provisionalCode ===
false`) :

```typescript
if (!orderLine.provisionalCode && newCip !== orderLine.produitCip) {
  this.confirmDialog.confirm({
    message: `Le CIP reçu (${newCip}) diffère du CIP commandé (${orderLine.produitCip}).
               Accepter la substitution et mettre à jour le produit ?`,
    header: '⚠ Substitution détectée',
    accept: () => this.commandeService.updateCip({ ...orderLine, produitCip: newCip }).subscribe(),
  });
}
```

---

## 5. Récapitulatif des fichiers impactés

### Fichiers à créer

| Fichier | Sprint | Rôle |
|---|---|---|
| `service/dto/ReceptionScanResultDTO.java` | R-3 | DTO résultat d'un scan (found, lot, péremption, type code) |
| `shared/model/reception-scan-result.model.ts` | R-3 | Modèle TypeScript correspondant |

### Fichiers à modifier — Backend

| Fichier | Sprint | Modification |
|---|---|---|
| `service/stock/StockEntryService.java` | R-3 | Ajouter `processScanReception()` dans l'interface |
| `service/stock/impl/StockEntryServiceImpl.java` | R-3 | Implémenter `processScanReception()` — injecter `DataMatrixParserService`, `LotService`, `ConfigurationService` |
| `web/rest/commande/StockEntryResource.java` | R-3 | Ajouter `POST /commandes/entree-stock/scan-reception` |
| `service/stock/impl/StockEntryServiceImpl.java` | R-4 | Ajouter `createReliquat()` dans le service |

### Fichiers à modifier — Frontend

| Fichier | Sprint | Modification principale |
|---|---|---|
| `ui/list-bons/list-bons.component.ts` | R-1 | Signaux `editingReceived` + `selectedClosed`, méthodes navigation, import AG Grid |
| `ui/list-bons/list-bons.component.html` | R-1 | 3 modes `@if` : LISTE / SAISIE / CONSULTATION split-panel |
| `feature/commande-received/commande-received.component.ts` | R-2 | Imports AG Grid, `buildColumnDefs()`, `rowClassRules`, `onGridReady()`, `onCellClicked()`, colonne Marge |
| `feature/commande-received/commande-received.component.html` | R-2 | Remplacer `<p-table>` par `<ag-grid-angular>` + `grid-caption` |
| `entities/commande/delevery/delivery.service.ts` | R-3 | Ajouter méthode `scanReception(commandeId, rawScan)` |
| `feature/commande-received/commande-received.component.ts` | R-3 | Signaux `scanValue` + `lastScanResult`, `onScanReception()`, écoute `ScanDetectorService` |
| `feature/commande-received/commande-received.component.html` | R-3 | Champ scan + bloc feedback dans la barre de filtres |
| `feature/commande-received/commande-received.component.ts` | R-4 | `checkReliquat()` dans `onFinalize()` |
| `ui/commande-status-bar/commande-status-bar.component.ts` | R-5 | Taux de service fournisseur |

### Fichiers inchangés

| Fichier | Raison |
|---|---|
| `service/stock/DataMatrixParserService.java` | Interface intacte — injectée dans StockEntryServiceImpl |
| `service/stock/impl/DataMatrixParserServiceImpl.java` | Implémentation intacte — aucune modification |
| `service/dto/DataMatrixInfo.java` | Record inchangé |
| `suggestions-unified.component.html/ts` | Continue à monter `<app-list-bons />` — aucune modification |
| `commande-requested.component.html/ts` | Déjà sur AG Grid, sert de référence |
| `reception-concordance.component.ts` | Réutilisé tel quel dans le split-panel CLOSED |

### Fichiers obsolètes (ne plus utiliser)

| Fichier | Raison |
|---|---|
| `feature/reception-hub/` | Pattern tabs imbriqués abandonné |
| `feature/commande-received-home/` | Logique absorbée par `list-bons` |

---

## 6. Analyse comparative des fonctionnalités

### Ce que Pharma-Smart a et que les concurrents n'ont pas

| Fonctionnalité | Avantage |
|---|---|
| **Concordance temps réel** (`ReceptionConcordanceComponent`) | Calcul live des écarts quantité/prix/lots/montants — absent ou basique chez tous les concurrents |
| **PharmaML / EDI intégré** | Intégration complète vs partielle chez certains |
| **Gestion lots/péremption** | Présente et complète |

### Ce que les concurrents ont et que Pharma-Smart n'a pas encore

| # | Fonctionnalité | Winpharma | LGPI | Alliadis | Pharma-Smart | Sprint |
|---|---|---|---|---|---|---|
| 1 | Split-panel CLOSED | ✅ | ✅ | ✅ | 🔲 | **R-1** |
| 2 | Full-panel RECEIVED | ✅ | ✅ | ✅ | 🔲 | **R-1** |
| 3 | Marge brute colonne | ⚠️ | ✅ | ✅ | 🔲 | **R-2** |
| 4 | Étiquette par ligne | ✅ | ⚠️ | ✅ | 🔲 | **R-2** |
| 5 | **Scan 1D CIP/EAN réception** | ✅ | ✅ | ✅ | ❌ | **R-3** |
| 6 | **Scan 2D DataMatrix — lot + péremption auto** | ✅ | ✅ | ✅ | ❌ (infra prête) | **R-3** |
| 7 | **Reliquat automatique** | ✅ | ✅ | ✅ | ❌ | **R-4** |
| 8 | Taux de service fournisseur | ✅ | ✅ | ✅ | ❌ | R-5 |
| 9 | Alerte substitution produit | ✅ | ✅ | ✅ | ❌ | R-5 |
| 10 | Génération avoir depuis réception | ✅ | ✅ | ⚠️ | ❌ | Backlog |
| 11 | PCB / conditionnement alerte | ⚠️ | ✅ | ✅ | ❌ | Backlog |
| 12 | Validation FMD / NMVS (serial number) | ✅ LGPI | ✅ | ❌ | ❌ (AI 21 parsé) | Backlog |
| 13 | Réconciliation facture fournisseur | ❌ | ✅ | ❌ | ❌ | Backlog |

---

## 7. Critères de validation (Definition of Done)

### Sprint R-1

- [ ] Clic "Saisir ▶" sur carte RECEIVED → `commande-received` en plein écran
- [ ] Bouton retour revient à la liste et recharge (le bon doit apparaître CLOSED)
- [ ] Clic sur carte CLOSED → split-panel gauche/droite
- [ ] Panel gauche : concordance stats + métadonnées fournisseur/référence/montants
- [ ] Panel droite : lignes de commande en lecture seule
- [ ] Bouton "← Bons de livraison" ferme le split-panel
- [ ] `suggestions-unified` non modifié

### Sprint R-2

- [ ] AG Grid utilisé dans `commande-received` (plus de `p-table`)
- [ ] Édition inline qty reçue, free qty, CIP provisoire fonctionnels
- [ ] Actions par ligne : historique prix, édition produit, lots, suppression, étiquette
- [ ] Row coloring variations prix/quantité visible
- [ ] Colonne Marge % affichée
- [ ] Colonne Lots conditionnelle selon `APP_GESTION_LOT`
- [ ] Footer totaux Achat/Vente sous la grille
- [ ] `applyTransaction` pour mise à jour ligne (pas de rechargement complet)
- [ ] Aucune régression sur finalisation (putaway, étiquettes, modale confirmation)

### Sprint R-3 — Scan DataMatrix

**Backend**
- [ ] `ReceptionScanResultDTO` créé avec tous les champs
- [ ] `processScanReception()` implémenté dans `StockEntryServiceImpl`
- [ ] `DataMatrixParserService` injecté dans `StockEntryServiceImpl`
- [ ] `LotService` injecté — lot créé automatiquement si DataMatrix + `APP_GESTION_LOT`
- [ ] Endpoint `POST /commandes/entree-stock/scan-reception` opérationnel
- [ ] Tests : scan CIP-13 1D, scan DataMatrix complet, CIP absent

**Frontend**
- [ ] `delivery.service.ts` : méthode `scanReception()` ajoutée
- [ ] Modèle `IReceptionScanResult` créé
- [ ] Champ scan visible dans la barre de filtres de `commande-received`
- [ ] Feedback visuel : ligne produit + badge lot/péremption si DataMatrix
- [ ] Badge "2D" si DataMatrix détecté
- [ ] Feedback auto-effacé après 3 secondes
- [ ] Scan 1D (CIP) → quantité incrémentée, `applyTransaction` grille
- [ ] Scan 2D (DataMatrix complet) → quantité incrémentée + colonne Lots rafraîchie
- [ ] Scan CIP absent → `warningMessage` affiché en orange
- [ ] Douchette hardware : `ScanDetectorService` écouté en plus de la saisie clavier
- [ ] Le champ scan reçoit le focus automatiquement à l'ouverture du MODE SAISIE

### Sprint R-4

- [ ] Après finalisation avec lignes partielles → boîte de dialogue reliquat
- [ ] Accepter → reliquat créé, notification avec numéro
- [ ] Refuser → clôture normale sans reliquat

### Sprint R-5

- [ ] Taux de service fournisseur affiché dans le header `commande-received`
- [ ] Alerte substitution si CIP EDI ≠ CIP commandé (hors provisoire)
