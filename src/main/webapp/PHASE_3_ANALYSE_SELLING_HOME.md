# Phase 3 : Analyse et Décomposition de SellingHomeComponent

## 📊 Analyse du God Component (1398 lignes)

### Structure actuelle

**Responsabilités identifiées :**
1. **Gestion de l'état** : 40+ propriétés de state, 14+ services injectés
2. **UI Navigation** : 3 onglets (Comptant, Assurance, Carnet)
3. **Gestion des produits** : Recherche, ajout, quantité, stock
4. **Gestion des clients** : Sélection, ajout, assignation
5. **Gestion des ventes** : Création, sauvegarde, finalisation, impression
6. **Gestion des paiements** : Modes de règlement, monnaie rendue
7. **Validation** : Stock, déconditionnement, force stock
8. **Raccourcis clavier** : Gestion des événements clavier
9. **Scan** : Intégration scanner barcode
10. **États métier** : Prévente, ventes en attente, édition

### Services actuels (14+)
- SalesService, CustomerService, ProduitService
- CurrentSaleService, SelectedCustomerService
- SelectModeReglementService, LastCurrencyGivenService
- TypePrescriptionService, UserCaissierService, UserVendeurService
- VoSalesService, BaseSaleService, RemiseCacheService
- HasAuthorityService, SaleStockValidator, etc.

### ViewChild Components (7)
- ComptantComponent
- AssuranceComponent
- CarnetComponent
- AssuranceDataComponent
- ProduitSearchAutocompleteScannerComponent
- QuantiteProdutSaisieComponent
- ConfirmDialogComponent, ToastAlertComponent

## 🎯 Stratégie de Décomposition

### 1. Architecture Proposée

```
app/features/sales/
├── ui/                                    # Composants de présentation
│   ├── product-search/
│   │   ├── product-search.component.ts    # Recherche produit + scan
│   │   └── product-search.component.html
│   ├── product-list/
│   │   ├── product-list.component.ts      # Liste des lignes de vente
│   │   └── product-list.component.html
│   ├── sale-summary/
│   │   ├── sale-summary.component.ts      # Résumé montants/totaux
│   │   └── sale-summary.component.html
│   ├── customer-selector/
│   │   ├── customer-selector.component.ts # Sélection/ajout client
│   │   └── customer-selector.component.html
│   ├── payment-selector/
│   │   ├── payment-selector.component.ts  # Modes de paiement
│   │   └── payment-selector.component.html
│   └── sale-actions/
│       ├── sale-actions.component.ts      # Boutons action (Sauver, Imprimer, etc.)
│       └── sale-actions.component.html
├── feature/                               # Composants containers
│   ├── sale-creation/
│   │   ├── sale-creation.component.ts     # Container principal (remplace SellingHomeComponent)
│   │   ├── sale-creation.component.html
│   │   ├── comptant-tab/                  # Onglet comptant
│   │   ├── assurance-tab/                 # Onglet assurance
│   │   └── carnet-tab/                    # Onglet carnet
│   └── pending-sales/
│       ├── pending-sales.component.ts     # Sidebar ventes en attente
│       └── pending-sales.component.html
└── data-access/
    ├── store/sales.store.ts               # ✅ Déjà créé
    ├── services/sales-api.service.ts      # ✅ Déjà créé
    └── facades/sales.facade.ts            # ✅ Déjà créé
```

### 2. Composants UI (Présentation) - Purs, sans logique métier

#### ProductSearchComponent
**Responsabilité** : Recherche produit + intégration scanner
**Inputs** :
- `minLength: number`
- `resultSize: number`
- `isScanned: boolean`
**Outputs** :
- `productSelected: EventEmitter<ProduitSearch>`
- `quantityAdded: EventEmitter<number>`
**Pas de services** - Tout via @Input/@Output

#### ProductListComponent
**Responsabilité** : Affichage liste des lignes de vente
**Inputs** :
- `salesLines: ISalesLine[]`
- `isEditable: boolean`
**Outputs** :
- `quantityChanged: EventEmitter<{line: ISalesLine, newQty: number}>`
- `lineRemoved: EventEmitter<ISalesLine>`
- `lineSelected: EventEmitter<ISalesLine>`
**Pas de services**

#### SaleSummaryComponent
**Responsabilité** : Affichage résumé montants
**Inputs** :
- `totalAmount: number`
- `discountAmount: number`
- `netAmount: number`
- `taxAmount: number`
**Outputs** : Aucun (pure display)
**Pas de services**

#### CustomerSelectorComponent
**Responsabilité** : Sélection/recherche client
**Inputs** :
- `selectedCustomer: ICustomer | null`
- `saleType: SaleType`
**Outputs** :
- `customerSelected: EventEmitter<ICustomer>`
- `customerRemoved: EventEmitter<void>`
- `customerAdd: EventEmitter<void>`
**Pas de services**

#### PaymentSelectorComponent
**Responsabilité** : Sélection mode paiement + montant
**Inputs** :
- `modes: IPaymentMode[]`
- `selectedMode: IPaymentMode | null`
- `amount: number`
**Outputs** :
- `modeSelected: EventEmitter<IPaymentMode>`
- `amountChanged: EventEmitter<number>`
**Pas de services**

#### SaleActionsComponent
**Responsabilité** : Boutons d'action
**Inputs** :
- `canSave: boolean`
- `canPrint: boolean`
- `canCancel: boolean`
- `isSaving: boolean`
- `saleType: SaleType`
**Outputs** :
- `save: EventEmitter<void>`
- `print: EventEmitter<void>`
- `cancel: EventEmitter<void>`
- `saveAsPrevent: EventEmitter<void>`
**Pas de services**

### 3. Composant Container - SaleCreationComponent

**Responsabilité** : Orchestration, logique métier, état
**Services injectés** :
- `SalesFacade` (nouveau store)
- `KeyboardShortcutsService`
- `GlobalScannerService`
- `ErrorService`, `TranslateService`

**Architecture** :
```typescript
export class SaleCreationComponent implements OnInit {
  private facade = inject(SalesFacade);
  
  // State depuis le store (computed signals)
  currentSale = this.facade.currentSale;
  salesLines = this.facade.salesLines;
  selectedCustomer = this.facade.selectedCustomer;
  totalAmount = this.facade.totalAmount;
  canSave = this.facade.canSave;
  
  // Handlers pour les événements UI
  onProductSelected(product: ProduitSearch) {
    this.facade.addProductToSale(product);
  }
  
  onQuantityChanged(data: {line: ISalesLine, newQty: number}) {
    this.facade.updateLineQuantity(data.line.id, data.newQty);
  }
  
  onSave() {
    this.facade.saveSale();
  }
}
```

### 4. Migration Progressive

**Étape 1** : Créer composants UI purs (sans store)
**Étape 2** : Créer SaleCreationComponent avec SalesFacade
**Étape 3** : Intégrer composants UI dans le container
**Étape 4** : Créer nouvelle route `/sales/new` (coexiste avec `/sales/selling`)
**Étape 5** : Tests et validation
**Étape 6** : Migration complète, suppression ancien composant

### 5. Compatibilité Backward

**Approche** : Dual routing temporaire
- Ancienne route : `/sales/selling` → SellingHomeComponent (inchangé)
- Nouvelle route : `/sales/new` → SaleCreationComponent (nouveau)
- Menu avec feature flag pour basculer entre les deux

**Adapter** : Créer `SalesServiceAdapter` qui expose l'API des anciens services mais utilise le nouveau store en interne

## 📝 Prochaines Actions

1. ✅ Créer composants UI de présentation (6 composants)
2. ✅ Créer SaleCreationComponent container
3. ✅ Intégrer SalesFacade
4. ✅ Créer routes et lazy loading
5. ✅ Tests de non-régression
6. ✅ Documentation migration

## ⚠️ Points de Vigilance

1. **Raccourcis clavier** : Ne pas casser les raccourcis existants
2. **Scanner** : Maintenir l'intégration scanner
3. **Impression** : Conserver la logique d'impression
4. **Validation stock** : Garder les validations métier
5. **Performance** : Optimiser avec OnPush + signals
6. **Tests** : Tester chaque composant isolément

## 🎯 Objectifs Phase 3

- Réduire SellingHomeComponent de 1398 → ~300 lignes
- Créer 6 composants UI réutilisables
- Intégrer le nouveau store (@ngrx/signals)
- Maintenir 100% compatibilité avec l'existant
- Améliorer testabilité et maintenabilité
