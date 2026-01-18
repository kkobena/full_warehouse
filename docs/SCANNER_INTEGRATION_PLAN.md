# Plan d'intégration du Scanner DataMatrix/EAN pour la saisie des bons de livraison

## Objectif

Intégrer un système de scan de codes-barres (EAN-8, EAN-13, CIP, DataMatrix GS1) dans le processus de réception des bons de livraison pour :
- Accélérer la saisie des produits
- Capturer automatiquement les numéros de lot et dates de péremption
- Réduire les erreurs de saisie manuelle

---

## Architecture proposée

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Angular)                          │
├─────────────────────────────────────────────────────────────────────┤
│  Scanner Input Component                                            │
│  ├── Écoute des événements clavier (mode scanner HID)              │
│  ├── Détection automatique du type de code                         │
│  └── Envoi vers BarcodeParserService (frontend)                    │
│                                                                     │
│  commande-update.component.ts                                       │
│  ├── Mode réception : scan pour remplir lot + péremption           │
│  └── Mode commande : scan pour recherche produit                   │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼ HTTP REST API
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot)                       │
├─────────────────────────────────────────────────────────────────────┤
│  BarcodeResource.java                                               │
│  └── POST /api/barcode/parse                                        │
│                                                                     │
│  DataMatrixParserService.java  (DÉJÀ CRÉÉ)                         │
│  ├── parse(String barcodeData) → DataMatrixInfo                    │
│  ├── detectBarcodeType(String code) → BarcodeType                  │
│  └── Validation EAN-8, EAN-13, CIP-7, CIP-13, DataMatrix           │
│                                                                     │
│  StockEntryService.java                                             │
│  └── Nouvelle méthode : processScannedBarcode(...)                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1 : Backend - Extension du service de parsing

### 1.1 Créer le endpoint REST pour le parsing

**Fichier** : `src/main/java/com/kobe/warehouse/web/rest/BarcodeResource.java`

```java
@RestController
@RequestMapping("/api/barcode")
public class BarcodeResource {

    private final DataMatrixParserService parserService;
    private final ProduitRepository produitRepository;

    @PostMapping("/parse")
    public ResponseEntity<BarcodeParseResponse> parseBarcode(@RequestBody BarcodeParseRequest request) {
        // 1. Parser le code
        // 2. Rechercher le produit correspondant
        // 3. Retourner les infos enrichies
    }
}
```

### 1.2 Créer les DTOs de requête/réponse

**Fichier** : `src/main/java/com/kobe/warehouse/service/dto/BarcodeParseRequest.java`

```java
public record BarcodeParseRequest(
    String barcodeData,
    Long deliveryId,      // Optionnel : contexte du bon de livraison
    LocalDate orderDate   // Optionnel : date de la commande
) {}
```

**Fichier** : `src/main/java/com/kobe/warehouse/service/dto/BarcodeParseResponse.java`

```java
public record BarcodeParseResponse(
    DataMatrixInfo.BarcodeType barcodeType,
    String productCode,        // CIP ou EAN détecté
    String batchNumber,        // Numéro de lot (si DataMatrix)
    LocalDate expiryDate,      // Date péremption (si DataMatrix)
    LocalDate manufacturingDate,
    String serialNumber,
    // Infos produit si trouvé
    Long produitId,
    String produitLibelle,
    Integer produitStock,
    Integer regularUnitPrice,
    // Contexte livraison
    Long orderLineId,          // Si produit trouvé dans le bon
    boolean productFoundInDelivery
) {}
```

### 1.3 Étendre StockEntryService

**Fichier** : `src/main/java/com/kobe/warehouse/service/stock/StockEntryService.java`

Ajouter les méthodes :

```java
/**
 * Traite un code-barres scanné dans le contexte d'une réception.
 *
 * @param barcodeData Le code scanné (EAN, CIP ou DataMatrix)
 * @param deliveryId L'ID du bon de livraison en cours
 * @return Les informations parsées et le produit associé si trouvé
 */
BarcodeParseResponse processScannedBarcode(String barcodeData, Long deliveryId, LocalDate orderDate);

/**
 * Applique les données scannées (lot, péremption) à une ligne de réception.
 */
void applyScannedDataToLine(Long orderLineId, LocalDate orderDate, String batchNumber, LocalDate expiryDate);
```

---

## Phase 2 : Frontend - Composant Scanner

### 2.1 Créer le service de scan côté frontend

**Fichier** : `src/main/webapp/app/shared/services/barcode-scanner.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class BarcodeScannerService {
  private readonly http = inject(HttpClient);
  private scanBuffer = '';
  private scanTimeout: any;

  // Signal pour émettre les codes scannés
  scannedCode = signal<string | null>(null);

  /**
   * Initialise l'écoute des événements clavier pour le scanner HID
   */
  initScannerListener(): void {
    document.addEventListener('keypress', this.handleKeyPress.bind(this));
  }

  /**
   * Parse un code-barres via le backend
   */
  parseBarcode(barcodeData: string, deliveryId?: number, orderDate?: string): Observable<BarcodeParseResponse> {
    return this.http.post<BarcodeParseResponse>('/api/barcode/parse', {
      barcodeData,
      deliveryId,
      orderDate
    });
  }

  /**
   * Détecte si c'est un scan rapide (scanner) vs frappe manuelle
   */
  private handleKeyPress(event: KeyboardEvent): void {
    // Les scanners envoient les caractères très rapidement
    // suivi d'un Enter
  }
}
```

### 2.2 Créer le composant d'input scanner

**Fichier** : `src/main/webapp/app/shared/scanner/scanner-input.component.ts`

```typescript
@Component({
  selector: 'jhi-scanner-input',
  standalone: true,
  template: `
    <div class="scanner-input-container">
      <p-iconfield>
        <p-inputicon class="pi pi-barcode" />
        <input
          #scannerInput
          pInputText
          [placeholder]="placeholder"
          (keydown.enter)="onScan($event)"
          (paste)="onPaste($event)"
        />
      </p-iconfield>
      @if (isScanning()) {
        <p-progressSpinner [style]="{width: '20px', height: '20px'}" />
      }
    </div>
  `
})
export class ScannerInputComponent {
  @Output() barcodeScanned = new EventEmitter<BarcodeParseResponse>();
  @Output() scanError = new EventEmitter<string>();

  @Input() deliveryId?: number;
  @Input() orderDate?: string;
  @Input() placeholder = 'Scanner un code-barres...';

  isScanning = signal(false);
}
```

### 2.3 Modifier commande-update.component.html

**Fichier** : `src/main/webapp/app/entities/commande/commande-update.component.html`

#### Mode Réception (isReceiption = true) - Ajout de la zone de scan

```html
<!-- Après la toolbar de filtres, ligne ~290 -->
@if (isReceiption) {
  <!-- Zone de scan pour la réception -->
  <div class="scanner-section">
    <div class="scanner-header">
      <i class="pi pi-barcode"></i>
      <span>Scanner un produit (DataMatrix / EAN / CIP)</span>
      <p-toggleButton
        [(ngModel)]="scanModeActive"
        onLabel="Scan actif"
        offLabel="Scan inactif"
        onIcon="pi pi-check"
        offIcon="pi pi-times"
      />
    </div>

    @if (scanModeActive) {
      <div class="scanner-controls">
        <jhi-scanner-input
          [deliveryId]="commande?.id"
          [orderDate]="commande?.orderDate"
          (barcodeScanned)="onBarcodeScanned($event)"
          (scanError)="onScanError($event)"
          placeholder="Scanner ou coller un code DataMatrix..."
        />

        <!-- Affichage du dernier scan -->
        @if (lastScanResult) {
          <div class="last-scan-result" [class.success]="lastScanResult.produitId" [class.warning]="!lastScanResult.produitId">
            <div class="scan-info">
              <span class="scan-type">{{ lastScanResult.barcodeType }}</span>
              @if (lastScanResult.produitLibelle) {
                <span class="scan-product">{{ lastScanResult.produitLibelle }}</span>
              }
              @if (lastScanResult.batchNumber) {
                <span class="scan-lot">Lot: {{ lastScanResult.batchNumber }}</span>
              }
              @if (lastScanResult.expiryDate) {
                <span class="scan-expiry">Pér: {{ lastScanResult.expiryDate | date:'dd/MM/yyyy' }}</span>
              }
            </div>
          </div>
        }
      </div>
    }
  </div>
}
```

### 2.4 Modifier commande-update.component.ts

**Fichier** : `src/main/webapp/app/entities/commande/commande-update.component.ts`

Ajouter les propriétés et méthodes :

```typescript
// Propriétés
protected scanModeActive = false;
protected lastScanResult: BarcodeParseResponse | null = null;
private readonly barcodeScannerService = inject(BarcodeScannerService);

// Méthodes
protected onBarcodeScanned(result: BarcodeParseResponse): void {
  this.lastScanResult = result;

  if (result.productFoundInDelivery && result.orderLineId) {
    // Produit trouvé dans le bon - scroll vers la ligne et ouvrir édition
    this.scrollToOrderLine(result.orderLineId);

    // Si DataMatrix avec lot/péremption, proposer d'appliquer
    if (result.batchNumber || result.expiryDate) {
      this.proposeApplyScannedData(result);
    }
  } else if (result.produitId) {
    // Produit trouvé mais pas dans ce bon
    this.openInfoDialog(
      `Produit "${result.produitLibelle}" trouvé mais n'est pas dans ce bon de livraison`,
      'warn',
      'Produit hors bon'
    );
  } else {
    // Produit non trouvé
    this.openInfoDialog(
      `Code "${result.productCode}" non trouvé dans la base`,
      'error',
      'Produit inconnu'
    );
  }
}

protected proposeApplyScannedData(result: BarcodeParseResponse): void {
  const orderLine = this.orderLines.find(ol => ol.id === result.orderLineId);
  if (!orderLine) return;

  this.confimDialog().onConfirm(
    () => this.applyScannedDataToLine(orderLine, result),
    'Appliquer les données scannées',
    `Voulez-vous appliquer :\n- Lot: ${result.batchNumber || 'N/A'}\n- Péremption: ${result.expiryDate || 'N/A'}\nau produit ${orderLine.produitLibelle} ?`
  );
}

private applyScannedDataToLine(orderLine: IOrderLine, result: BarcodeParseResponse): void {
  // Appeler le service pour appliquer lot et péremption
  this.deliveryService.applyScannedData({
    orderLineId: orderLine.id,
    orderDate: orderLine.orderDate,
    batchNumber: result.batchNumber,
    expiryDate: result.expiryDate
  }).subscribe({
    next: () => {
      this.refreshCommande();
      this.alert().show('Succès', 'Données appliquées avec succès', 'success');
    },
    error: (err) => this.onCommonError(err)
  });
}
```

---

## Phase 3 : Flux utilisateur détaillé

### 3.1 Scénario 1 : Scan EAN-13 / CIP simple

```
1. Utilisateur scanne un code EAN-13 : "3400930000001"
2. Frontend détecte le code et appelle POST /api/barcode/parse
3. Backend :
   - Parse le code → type: CIP_13, cip13: "3400930000001"
   - Recherche produit par CIP
   - Vérifie si présent dans le bon de livraison
4. Frontend :
   - Si trouvé dans bon → scroll vers ligne + highlight
   - Si trouvé mais pas dans bon → warning
   - Si non trouvé → error
```

### 3.2 Scénario 2 : Scan DataMatrix avec lot et péremption

```
1. Utilisateur scanne un DataMatrix : "010340093000000117251231102024A21ABC123"
2. Frontend envoie au backend
3. Backend parse :
   - AI 01 (GTIN): 03400930000001 → EAN-13: 3400930000001
   - AI 17 (Expiry): 251231 → 2025-12-31
   - AI 10 (Batch): 2024A
   - AI 21 (Serial): ABC123
4. Backend recherche produit et vérifie présence dans bon
5. Frontend :
   - Affiche les infos parsées
   - Propose d'appliquer lot "2024A" et péremption "31/12/2025"
   - Si confirmé → met à jour la ligne
```

### 3.3 Scénario 3 : Scan multiple (même produit, lots différents)

```
1. Scan produit X avec lot "LOT001" péremption 2025-06
2. Application sur ligne existante ou création nouveau lot
3. Scan produit X avec lot "LOT002" péremption 2025-09
4. Proposition de créer un second lot pour la même ligne
```

---

## Phase 4 : Modifications de l'interface

### 4.1 Styles CSS à ajouter

**Fichier** : `src/main/webapp/app/entities/commande/commande-update.component.scss`

```scss
.scanner-section {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;

  .scanner-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.75rem;

    i { color: var(--primary-color); }
    span { font-weight: 600; flex: 1; }
  }

  .scanner-controls {
    display: flex;
    gap: 1rem;
    align-items: center;
  }

  .last-scan-result {
    padding: 0.5rem 1rem;
    border-radius: 4px;

    &.success { background: var(--green-50); border: 1px solid var(--green-200); }
    &.warning { background: var(--yellow-50); border: 1px solid var(--yellow-200); }

    .scan-info {
      display: flex;
      gap: 1rem;
      font-size: 0.875rem;

      .scan-type {
        font-weight: 600;
        color: var(--primary-color);
      }
      .scan-lot, .scan-expiry {
        color: var(--text-color-secondary);
      }
    }
  }
}

// Highlight de la ligne scannée
.pharma-table tr.scanned-highlight {
  animation: highlight-pulse 2s ease-out;
}

@keyframes highlight-pulse {
  0% { background-color: var(--primary-100); }
  100% { background-color: transparent; }
}
```

---

## Phase 5 : Tests et validation

### 5.1 Tests unitaires Backend

```java
@SpringBootTest
class DataMatrixParserServiceTest {

    @Test
    void shouldParseEan13() {
        var result = service.parse("3400930000001");
        assertThat(result).isPresent();
        assertThat(result.get().cip13()).isEqualTo("3400930000001");
    }

    @Test
    void shouldParseDataMatrixWithLotAndExpiry() {
        // AI 01 + AI 17 + AI 10
        var code = "010340093000000117251231102024A";
        var result = service.parse(code);

        assertThat(result).isPresent();
        assertThat(result.get().batchNumber()).isEqualTo("2024A");
        assertThat(result.get().expiryDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    }
}
```

### 5.2 Tests E2E Frontend

```typescript
describe('Scanner Integration', () => {
  it('should parse and apply DataMatrix data', () => {
    // 1. Naviguer vers réception
    // 2. Activer mode scan
    // 3. Simuler scan DataMatrix
    // 4. Vérifier affichage résultat
    // 5. Confirmer application
    // 6. Vérifier mise à jour ligne
  });
});
```

---

## Estimation des tâches

| Phase | Tâche | Complexité | Priorité |
|-------|-------|------------|----------|
| 1.1 | Créer BarcodeResource.java | Moyenne | P1 |
| 1.2 | Créer DTOs requête/réponse | Faible | P1 |
| 1.3 | Étendre StockEntryService | Moyenne | P1 |
| 2.1 | Créer BarcodeScannerService | Moyenne | P1 |
| 2.2 | Créer ScannerInputComponent | Moyenne | P2 |
| 2.3 | Modifier template HTML | Moyenne | P2 |
| 2.4 | Modifier component TS | Haute | P2 |
| 3.x | Implémenter flux utilisateur | Haute | P2 |
| 4.1 | Ajouter styles CSS | Faible | P3 |
| 5.x | Tests unitaires et E2E | Moyenne | P3 |

---

## Dépendances et prérequis

1. **Hardware** : Scanner code-barres USB en mode HID (clavier)
2. **Backend** : Service `DataMatrixParserService` déjà créé
3. **Frontend** : PrimeNG 20+ (ToggleButton, IconField, etc.)

---

## Points d'attention

1. **Performance** : Le parsing doit être < 100ms pour une UX fluide
2. **Gestion des erreurs** : Codes illisibles, produits non trouvés
3. **Mode scanner** : Différencier frappe clavier vs scan rapide
4. **DataMatrix partiels** : Certains peuvent ne pas avoir lot/péremption
5. **Produits avec plusieurs CIP** : Gérer les alias de codes
