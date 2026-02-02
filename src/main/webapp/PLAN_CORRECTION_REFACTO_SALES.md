# Plan de Correction - Refactoring Module Sales

**Date**: 2 février 2026  
**Objectif**: Restaurer les fonctionnalités métier manquantes dans le nouveau module `app/features/sales` en se basant sur l'ancien module `app/entities/sales/selling-home`

---

## 📋 État des Lieux - Problèmes Identifiés

### 🔴 Problème 1: Insurance Data Bar ne s'affiche pas
- **Ancien**: Le composant `assurance-data` s'affiche automatiquement quand le client est sélectionné
- **Nouveau**: Le composant `insurance-data-bar` ne s'affiche pas même après sélection du client
- **Impact**: Impossible de créer une vente Assurance

### 🔴 Problème 2: Navigation du focus cassée
- **Ancien**: Gestion complète du focus entre les champs
- **Nouveau**: Pas de gestion automatique du focus
- **Impact**: Expérience utilisateur dégradée, workflow cassé

### 🔴 Problème 3: Comportement Scanner manquant
- **Ancien**: Scanner → ajout automatique du produit avec quantité 1
- **Nouveau**: Aucune distinction scanner vs sélection manuelle
- **Impact**: Workflow scanner non fonctionnel

---

## 🔍 Analyse Comparative Détaillée

### A. Gestion de la Recherche Client (ASSURANCE)

#### ✅ Ancien (`assurance-data.component.ts`)
```typescript
private handleQueryResponse(assuredCustomers: ICustomer[] | null, saleType: string): void {
  if (!assuredCustomers?.length) {
    this.handleNoCustomersFound(); // Ouvre modal création
    return;
  }

  if (assuredCustomers.length === 1) {
    // ✅ Sélection automatique si 1 seul résultat
    this.handleSingleCustomerFound(assuredCustomers[0], saleType);
  } else {
    // Ouvre liste si plusieurs résultats
    this.handleMultipleCustomersFound();
  }
}

private handleSingleCustomerFound(customer: ICustomer, saleType: string): void {
  this.selectedCustomerService.setCustomer(customer); // ✅ Sélectionne le client
  this.ayantDroit = null;
  this.setTiersPayantsForSaleType(customer, saleType); // ✅ Configure tiers payants
  this.firstRefBonFocus(); // ✅ Focus sur premier BON
  this.clearSearch();
}
```

#### ❌ Nouveau (`insurance-data-bar.component.ts`)
```typescript
private handleQueryResponse(assuredCustomers: ICustomer[] | null): void {
  if (!assuredCustomers?.length) {
    this.handleNoCustomersFound();
    return;
  }

  if (assuredCustomers.length === 1) {
    // ✅ Émet l'événement mais...
    this.customerSelected.emit(assuredCustomers[0]);
    this.setTiersPayants(customer);
    this.firstRefBonFocus();
    this.clearSearch();
  } else {
    this.handleMultipleCustomersFound();
  }
}
```

**🔧 Problème**: Dans `sale-assurance.component.ts`, l'événement est intercepté mais:
```typescript
onCustomerSelectedFromBar(customer: ICustomer): void {
  const newCustomer = { ...customer, tiersPayants: [...(customer.tiersPayants || [])] };
  this.facade.setCustomer(newCustomer); // ✅ Set customer dans store
  const currentSale = this.currentSale();
  if (currentSale && newCustomer.tiersPayants) {
    currentSale.tiersPayants = newCustomer.tiersPayants; // ⚠️ Mutation directe
  }
  // ❌ MANQUE: Pas de création de vente si pas de currentSale
  // ❌ MANQUE: La barre insurance-data-bar ne se rend pas visible
}
```

---

### B. Gestion du Focus Produit → Quantité → Mode Règlement

#### ✅ Ancien Workflow Complet
```typescript
// 1. Sélection manuelle d'un produit
protected onSelectProduct(selectedProduit?: ProduitSearch): void {
  this.produitSelected = selectedProduit || null;
  if (!this.produitSelected) return;
  
  // ✅ Focus automatique sur quantité avec valeur 1
  this.quantyBox().reset(1);
  this.quantyBox().focusProduitControl();
}

// 2. Scan d'un produit → ajout automatique
protected onScannedProduct(scannedProduit: ProduitSearch): void {
  this.isScannedProduct.set(true);
  this.produitSelected = scannedProduit;
  
  // ✅ Ajout automatique avec quantité 1
  this.onAddNewQty(1);
}

// 3. Après ajout réussi
onSave(saveResponse: SaveResponse): void {
  if (saveResponse.success) {
    const selectedProduit = this.produitSelected;
    if (selectedProduit) {
      this.updateDisplayForProduct(...); // Afficheur client
    }
    this.updateProduitQtyBox(); // ✅ Focus retourne sur produit
  }
}

private updateProduitQtyBox(): void {
  this.produitSelected = null;
  this.isScannedProduct.set(false);
  this.produitbox().reset();
  
  if (this.quantyBox()) {
    this.quantyBox().reset(1);
  }
  
  // ✅ Focus retourne sur recherche produit
  this.produitbox().getFocus();
}

// 4. Enter dans champ produit vide avec vente en cours
protected onSaveKeyDown(saveSale: boolean): void {
  if (saveSale && this.currentSaleService.currentSale()?.salesLines.length > 0) {
    if (this.isVoSale() && this.currentSaleService.currentSale().amountToBePaid === 0) {
      this.save();
    } else {
      // ✅ Ouvre modal paiement, focus sur mode CASH
      this.manageAmountDiv();
    }
  }
}
```

#### ❌ Nouveau - Pas de Gestion du Focus
```typescript
// sale-assurance.component.ts
onProductSelected(product: ProduitSearch | null): void {
  if (!product) return;
  
  if (!this.hasCustomer()) {
    this.notificationService.warning(...);
    return;
  }
  
  this.facade.setSelectedProduct(product);
  // ❌ MANQUE: Pas de focus sur quantité
}

onAddQuantity(quantity: number): void {
  const product = this.selectedProduct();
  if (!product || !quantity || quantity <= 0) return;
  
  if (!this.hasCustomer()) {
    this.notificationService.warning(...);
    return;
  }
  
  const currentSale = this.currentSale();
  if (!currentSale) return;
  const line = createSalesLineFromProduct(product, quantity, currentSale);
  this.facade.addSalesLine(line);
  this.productAddedSuccess.emit();
  
  // ❌ MANQUE: Pas de reset du produit sélectionné
  // ❌ MANQUE: Pas de focus sur recherche produit
  // ❌ MANQUE: Pas de distinction scanner vs sélection manuelle
}
```

---

### C. Visibilité Conditionnelle Insurance Data Bar

#### ✅ Ancien
```html
<!-- selling-home.component.html -->
@if (active === 'assurance' || active === 'carnet') {
  <div class="insurance-data-bar">
    <!-- ✅ Toujours visible quand type = ASSURANCE/CARNET -->
    <jhi-assurance-data 
      (inputToFocusEvent)="getControlToFocus($event)" 
      #assuranceDataComponent>
    </jhi-assurance-data>
  </div>
}
```

```typescript
// assurance-data.component.html - Affichage conditionnel DANS le composant
@if (!selectedCustomerService.selectedCustomerSignal() || !currentSaleService.currentSale()) {
  <!-- ✅ Section recherche visible quand pas de client -->
  <div class="search-section">
    <input #searchInput ... />
  </div>
}

@if (selectedCustomerService.selectedCustomerSignal()) {
  <!-- ✅ Section données client visible quand client sélectionné -->
  <div class="customer-data-grid">
    <!-- Cards Assuré, Ayant droit, Tiers payants -->
  </div>
}
```

#### ❌ Nouveau
```html
<!-- sale-assurance.component.html -->
<div class="insurance-data-section">
  <app-insurance-data-bar
    #insuranceDataBar
    [customer]="selectedCustomer()"
    [tiersPayants]="selectedCustomer()?.tiersPayants || []"
    ...>
  </app-insurance-data-bar>
</div>
```

```html
<!-- insurance-data-bar.component.html -->
@if (!customer()) {
  <!-- ⚠️ Section recherche visible SI customer() est null -->
  <div class="search-section">...</div>
}

@if (customer()) {
  <!-- ⚠️ Section données client visible SI customer() existe -->
  <div class="customer-data-grid">...</div>
}
```

**🔧 Problème**: 
- Le signal `customer()` est un input du composant
- Il dépend de `selectedCustomer()` du parent
- Si `selectedCustomer()` n'est pas mis à jour correctement → composant ne s'affiche jamais

---

### D. Gestion Enter dans Champ Produit Vide

#### ✅ Ancien
```html
<!-- selling-home.component.html -->
<jhi-produit-search-autocomplete-scanner
  #produitbox
  (onKeyEnter)="onSaveKeyDown($event)"
  ...>
</jhi-produit-search-autocomplete-scanner>
```

```typescript
protected onSaveKeyDown(saveSale: boolean): void {
  // ✅ Si vente en cours et Enter dans champ vide
  if (saveSale && this.currentSaleService.currentSale()?.salesLines.length > 0) {
    if (this.isVoSale() && this.currentSaleService.currentSale().amountToBePaid === 0) {
      this.save(); // Sauvegarde directe si montant = 0
    } else {
      this.manageAmountDiv(); // ✅ Ouvre modal paiement
    }
  }
}

protected manageAmountDiv(): void {
  if (this.isComptant()) {
    this.comptantComponent().openAmountToBePaidModal();
  } else if (this.isVoSale()) {
    this.showComptantDiv = true;
    // ✅ Focus sur mode règlement CASH
    this.canFocusLastModeInput = true;
  }
}
```

#### ❌ Nouveau
```html
<!-- sale-assurance.component.html -->
<app-product-search
  [autofocus]="hasCustomer()"
  (productSelected)="onProductSelected($event)">
</app-product-search>
```

**❌ MANQUE**: Pas d'événement `onKeyEnter` géré dans le nouveau composant

---

## 🎯 Plan d'Implémentation

### Phase 1: Correction Insurance Data Bar (CRITIQUE)

#### 1.1 Créer la vente ASSURANCE automatiquement après sélection client

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
onCustomerSelectedFromBar(customer: ICustomer): void {
  // Cloner l'objet pour forcer la réactivité
  const newCustomer = { ...customer, tiersPayants: [...(customer.tiersPayants || [])] };
  this.facade.setCustomer(newCustomer);
  
  // ✅ AJOUT: Créer la vente si elle n'existe pas
  const currentSale = this.currentSale();
  if (!currentSale) {
    this.facade.initializeAssuranceSale();
  }
  
  // ✅ AJOUT: Mettre à jour les tiers payants de la vente
  if (newCustomer.tiersPayants) {
    // Cette logique doit être ajoutée au store/facade
    this.facade.updateSaleTiersPayants(newCustomer.tiersPayants);
  }
}
```

**Fichier**: `app/features/sales/data-access/facades/sales.facade.ts`

**Action**: Ajouter méthode
```typescript
updateSaleTiersPayants(tiersPayants: IClientTiersPayant[]): void {
  const currentSale = this.store.currentSale();
  if (currentSale) {
    this.store.updateSale({
      ...currentSale,
      tiersPayants: tiersPayants,
    });
  }
}
```

#### 1.2 Afficher insurance-data-bar basé sur présence de client OU type vente

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.html`

**Action**: Modifier la condition d'affichage
```html
<!-- AVANT -->
<div class="insurance-data-section">
  <app-insurance-data-bar ...>
  </app-insurance-data-bar>
</div>

<!-- APRÈS -->
<div class="insurance-data-section">
  <!-- ✅ Toujours afficher pour ASSURANCE (comme ancien) -->
  <app-insurance-data-bar
    #insuranceDataBar
    [customer]="selectedCustomer()"
    [tiersPayants]="currentSale()?.tiersPayants || []"
    [saleType]="'ASSURANCE'"
    [ayantDroit]="currentSale()?.ayantDroit || null"
    (customerSelected)="onCustomerSelectedFromBar($event)"
    ...>
  </app-insurance-data-bar>
</div>
```

---

### Phase 2: Gestion du Focus (HAUTE PRIORITÉ)

#### 2.1 Ajouter ViewChild pour composants

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
export class SaleAssuranceComponent implements OnInit {
  // ✅ AJOUT: ViewChild pour gestion du focus
  productSearchComponent = viewChild<ProductSearchComponent>('produitbox');
  quantityComponent = viewChild<QuantiteProdutSaisieComponent>('quantityBox');
  insuranceDataBar = viewChild<InsuranceDataBarComponent>('insuranceDataBar');
  paymentModeComponent = viewChild<PaymentModeComponent>('paymentMode');
  
  // ... reste du code
}
```

#### 2.2 Implémenter logique de focus après sélection produit

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
onProductSelected(product: ProduitSearch | null): void {
  if (!product) return;

  if (!this.hasCustomer()) {
    this.notificationService.warning(
      'Client requis',
      'Veuillez sélectionner un client assuré avant d\'ajouter des produits'
    );
    return;
  }

  this.facade.setSelectedProduct(product);
  
  // ✅ AJOUT: Focus sur quantité après sélection
  setTimeout(() => {
    this.quantityComponent()?.focusProduitControl();
    this.quantityComponent()?.reset(1);
  }, 100);
}
```

#### 2.3 Réinitialiser et focus après ajout produit

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
onAddQuantity(quantity: number): void {
  const product = this.selectedProduct();
  if (!product || !quantity || quantity <= 0) return;

  if (!this.hasCustomer()) {
    this.notificationService.warning(
      'Client requis',
      'Veuillez sélectionner un client assuré avant d\'ajouter des produits'
    );
    return;
  }

  const currentSale = this.currentSale();
  if (!currentSale) return;
  
  const line = createSalesLineFromProduct(product, quantity, currentSale);
  this.facade.addSalesLine(line);
  
  // ✅ AJOUT: Reset après succès
  this.resetProductSelection();
}

private resetProductSelection(): void {
  // ✅ Réinitialiser le produit sélectionné
  this.facade.setSelectedProduct(null);
  
  // ✅ Réinitialiser le composant de recherche
  this.productSearchComponent()?.reset();
  
  // ✅ Réinitialiser la quantité
  this.quantityComponent()?.reset(1);
  
  // ✅ Focus sur recherche produit
  setTimeout(() => {
    this.productSearchComponent()?.getFocus();
  }, 100);
}
```

#### 2.4 Ajouter méthodes reset() et getFocus() dans ProductSearchComponent

**Fichier**: `app/features/sales/ui/product-search/product-search.component.ts`

**Action**:
```typescript
import { Component, viewChild, signal, inject, output, computed, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-product-search',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ... autres propriétés
})
export class ProductSearchComponent {
  // ViewChild pour accéder à l'autocomplete
  private autocomplete = viewChild<any>('autocomplete');
  
  // Signals pour l'état
  protected searchTerm = signal('');
  protected selectedProduct = signal<ProduitSearch | null>(null);
  
  // Outputs
  readonly productSelected = output<ProduitSearch>();
  
  /**
   * Méthode publique pour réinitialiser le composant
   * Appelée après ajout d'un produit
   */
  public reset(): void {
    this.searchTerm.set('');
    this.selectedProduct.set(null);
    if (this.autocomplete()) {
      this.autocomplete().clear();
    }
  }
  
  /**
   * Méthode publique pour mettre le focus
   * Appelée après ajout d'un produit ou changement d'onglet
   */
  public getFocus(): void {
    setTimeout(() => {
      const inputEl = this.autocomplete()?.inputEL?.nativeElement;
      if (inputEl) {
        inputEl.focus();
        inputEl.select();
      }
    }, 100);
  }
}
```

---

### Phase 3: Gestion Scanner vs Sélection Manuelle (MOYENNE PRIORITÉ)

#### 3.1 Ajouter distinction scanner dans ProductSearchComponent

**Fichier**: `app/features/sales/ui/product-search/product-search.component.ts`

**Action**:
```typescript
import { Component, viewChild, signal, inject, output, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-product-search',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ... autres propriétés
})
export class ProductSearchComponent {
  // Services injectés
  private produitService = inject(ProduitService);
  private notificationService = inject(NotificationService);
  private destroyRef = inject(DestroyRef);
  
  // Signals pour l'état
  private isFromScanner = signal(false);
  protected selectedProduct = signal<ProduitSearch | null>(null);
  
  // ✅ AJOUT: Outputs pour distinguer scanner vs sélection
  readonly productScanned = output<ProduitSearch>();
  readonly productSelected = output<ProduitSearch>();
  
  protected onProductSelect(event: any): void {
    const product = event.value as ProduitSearch;
    if (!product) return;
    
    // ✅ Différencier scanner vs sélection manuelle
    if (this.isFromScanner()) {
      this.productScanned.emit(product);
      this.isFromScanner.set(false);
    } else {
      this.productSelected.emit(product);
    }
    
    this.selectedProduct.set(product);
  }
  
  protected onBarcodeScanned(barcode: string): void {
    // ✅ Marquer comme provenant du scanner
    this.isFromScanner.set(true);
    
    // Chercher le produit et émettre via productScanned
    this.produitService.findByCodeEan(barcode)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res.body) {
            this.selectedProduct.set(res.body);
            this.productScanned.emit(res.body);
          }
        },
        error: () => {
          this.notificationService.error('Produit introuvable', 'Code-barres non reconnu');
        }
      });
  }
}
```

#### 3.2 Gérer les deux types de sélection dans sale-assurance

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.html`

**Action**:
```html
<app-product-search
  #produitbox
  [autofocus]="hasCustomer()"
  (productSelected)="onProductSelected($event)"
  (productScanned)="onProductScanned($event)">
</app-product-search>
```

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
// Sélection manuelle → focus sur quantité
onProductSelected(product: ProduitSearch | null): void {
  if (!product || !this.hasCustomer()) return;
  
  this.facade.setSelectedProduct(product);
  
  // ✅ Focus sur quantité
  setTimeout(() => {
    this.quantityComponent()?.focusProduitControl();
    this.quantityComponent()?.reset(1);
  }, 100);
}

// Scanner → ajout automatique avec quantité 1
onProductScanned(product: ProduitSearch): void {
  if (!product || !this.hasCustomer()) return;
  
  this.facade.setSelectedProduct(product);
  
  // ✅ Ajout automatique
  const currentSale = this.currentSale();
  if (!currentSale) return;
  
  const line = createSalesLineFromProduct(product, 1, currentSale);
  this.facade.addSalesLine(line);
import { Component, signal, output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-product-search',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p-autoComplete
      (onKeyDown)="onKeyDown($event)"
      ...>
    </p-autoComplete>
  `
})
export class ProductSearchComponent {
  // Signals
  protected searchTerm = signal('');
  
  // ✅ AJOUT: Output pour Enter dans champ vide
  readonly enterPressed = output<void>();
  
  protected onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      const inputValue = this.searchTerm();
      
      // ✅ Si champ vide → émettre événement
      if (!inputValue || inputValue.trim().length === 0) {
        this.enterPressed.emit();
        event.preventDefault();
      }
    }
  }
}
      // ✅ Si champ vide → émettre événement
      if (!inputValue || inputValue.trim().length === 0) {
        this.enterPressed.emit();
        event.preventDefault();
      }
    }
  }
}
```

**Template**:
```html
<p-autoComplete
  (onKeyDown)="onKeyDown($event)"
  ...>
</p-autoComplete>
```

#### 4.2 Gérer Enter dans sale-assurance

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.html`

**Action**:
```html
<app-product-search
  #produitbox
  [autofocus]="hasCustomer()"
  (productSelected)="onProductSelected($event)"
  (productScanned)="onProductScanned($event)"
  (enterPressed)="onProductSearchEnter()">
</app-product-search>
```

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
onProductSearchEnter(): void {
  const currentSale = this.currentSale();
  
  // ✅ Si vente en cours avec des lignes
  if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
    const amountToBePaid = this.amountToBePaid();
    
    // Si montant à payer = 0 → sauvegarder directement
    if (amountToBePaid === 0) {
      this.onSave();
    } else {
      // ✅ Ouvrir modal paiement
      this.showPaymentModal.set(true);
      
      // ✅ Focus sur premier mode règlement
      setTimeout(() => {
        this.paymentModeComponent()?.focusFirstMode();
      }, 300);
    }
  }
}
```
import { Component, viewChild, ElementRef, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-payment-mode',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ... autres propriétés
})
export class PaymentModeComponent {
  // ViewChild pour accéder au premier input de mode règlement
  private firstModeInput = viewChild<ElementRef>('firstModeInput');
  
  /**
   * Méthode publique pour mettre le focus sur le premier mode (CASH)
   * Appelée depuis le parent après ouverture du modal paiement
   */
  public focusFirstMode(): void {
    setTimeout(() => {
      const inputEl = this.firstModeInput()?.nativeElement;
      if (inputEl) {
        inputEl.focus();
        inputEl.select();
      }
  /**
   * Méthode publique pour mettre le focus sur le premier mode (CASH)
   */
  public focusFirstMode(): void {
    setTimeout(() => {
      this.firstModeInput()?.nativeElement?.focus();
      this.firstModeInput()?.nativeElement?.select();
    }, 100);
  }
}
```

---

### Phase 5: Gestion Tiers Payants et BON (BASSE PRIORITÉ)

#### 5.1 Focus sur premier BON après sélection client

**Fichier**: `app/features/sales/ui/insurance-data-bar/insurance-data-bar.component.ts`

**Action**: Vérifier que `firstRefBonFocus()` fonctionne correctement
```typescript
private firstRefBonFocus(): void {
  setTimeout(() => {
    const bonInputs = this.bonInputs();
    if (bonInputs && bonInputs.length > 0) {
      const firstInput = bonInputs[0].nativeElement;
      firstInput.focus();
import { Component, signal, output, viewChildren, ElementRef, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-insurance-data-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ... autres propriétés
})
export class InsuranceDataBarComponent {
  // ViewChildren pour accéder aux inputs BON
  private bonInputs = viewChildren<ElementRef>('tpInput');
  
  // Signals
  protected selectedTiersPayants = signal<IClientTiersPayant[]>([]);
  
  // ✅ AJOUT: Output pour notifier la complétion des BON
  readonly bonInputComplete = output<void>();
  
  protected onBonEnter(tp: IClientTiersPayant): void {
    const tiersPayants = this.selectedTiersPayants();
    const currentIndex = tiersPayants.findIndex(item => item.id === tp.id);
    
    if (currentIndex < tiersPayants.length - 1) {
      // Focus sur BON suivant
      this.focusAndSelectBonInput(currentIndex + 1);
    } else {
      // ✅ Dernier BON → émettre événement pour focus produit
      setTimeout(() => {
        this.bonInputComplete.emit();
      }, 100);
    }
  }
  
  private focusAndSelectBonInput(index: number): void {
    setTimeout(() => {
      const inputs = this.bonInputs();
      if (inputs && inputs[index]) {
        const inputEl = inputs[index].nativeElement;
        inputEl.focus();
        inputEl.select();
      }rsPayant): void {
  const tiersPayants = this.selectedTiersPayants();
  const currentIndex = tiersPayants.findIndex(item => item.id === tp.id);
  
  if (currentIndex < tiersPayants.length - 1) {
    // Focus sur BON suivant
    this.focusAndSelectBonInput(currentIndex + 1);
  } else {
    // ✅ Dernier BON → émettre événement pour focus produit
    // Le parent doit gérer ce focus
    setTimeout(() => {
      // Émettre un événement custom au parent
      this.bonInputComplete.emit();
    }, 100);
  }
}
```

#### 5.3 Gérer événement bonInputComplete dans sale-assurance

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.html`

**Action**:
```html
<app-insurance-data-bar
  ...
  (bonInputComplete)="onBonInputComplete()">
</app-insurance-data-bar>
```

**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts`

**Action**:
```typescript
onBonInputComplete(): void {
  // ✅ Focus sur recherche produit
  setTimeout(() => {
    this.productSearchComponent()?.getFocus();
  }, 100);
}
```

---

## 📝 Checklist de Validation

### ✅ Insurance Data Bar
- [ ] Barre visible dès l'ouverture de l'onglet ASSURANCE
- [ ] Recherche client fonctionnelle (Enter déclenche la recherche)
- [ ] 1 seul résultat → sélection automatique
- [ ] Plusieurs résultats → modal liste clients
- [ ] 0 résultat → modal création client
- [ ] Affichage des données client après sélection (Assuré, Ayant droit, Tiers payants)
- [ ] Création automatique de la vente ASSURANCE après sélection client

### ✅ Focus et Navigation
- [ ] Focus sur recher - Bonnes Pratiques Angular 20+
- **Timing des focus**: Utiliser `setTimeout` avec délai suffisant (100-300ms)
- **Change Detection**: Tous les composants doivent utiliser `ChangeDetectionStrategy.OnPush`
- **Signals obligatoires**: Utiliser `signal()`, `computed()`, et `effect()` pour la gestion d'état
- **Gestion mémoire**: Toujours utiliser `takeUntilDestroyed(this.destroyRef)` pour les observables
- **Événements personnalisés**: Utiliser `output<T>()` au lieu de `@Output()` et `EventEmitter`
- **ViewChild/ViewChildren**: Utiliser les fonctions `viewChild()` et `viewChildren()` au lieu des décorateurs
- **Injection**: Utiliser `inject()` au lieu de constructor injection
- **Control Flow**: Utiliser `@if`, `@for`, `@switch` au lieu de `*ngIf`, `*ngFor`, `*ngSwitch`
- **Standalone**: Ne PAS définir `standalone: true` (c'est le défaut en Angular 20+)
- **Host bindings**: Utiliser l'objet `host` dans le décorateur au lieu de `@HostBinding`/`@HostListener`
- [ ] Après ajout produit → reset et focus sur recherche produit
- [ ] Enter dans recherche produit vide avec vente → modal paiement avec focus sur CASH

### ✅ Scanner vs Sélection Manuelle
- [ ] Scanner → ajout automatique avec quantité 1
- [ ] Sélection manuelle → focus sur quantité
- [ ] Pas d'affichage des métadonnées produit après scan
- [ ] Affichage des métadonnées après sélection manuelle

### ✅ Gestion Paiement
- [ ] Enter dans produit vide → modal paiement
- [ ] Focus automatique sur mode CASH dans modal
- [ ] Si montant = 0 → sauvegarde directe (pas de modal)

### Standards de Code Angular 20+

Tous les composants modifiés doivent respecter:

```typescript
import { Component, signal, computed, effect, inject, input, output, 
         viewChild, viewChildren, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-example',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // PAS de standalone: true (défaut en Angular 20+)
  template: `
    @if (showContent()) {
      <div>{{ content() }}</div>
    }
    @for (item of items(); track item.id) {
      <div>{{ item.name }}</div>
    }
  `
})
export class ExampleComponent {
  // Injection via inject()
  private service = inject(SomeService);
  private destroyRef = inject(DestroyRef);
  
  // Inputs/Outputs via fonctions
  readonly data = input<string>();
  readonly dataChange = output<string>();
  
  // ViewChild via fonction
  private childComponent = viewChild<ChildComponent>('child');
  
  // State via signals
  protected localState = signal('initial');
  protected derivedState = computed(() => this.localState().toUpperCase());
  
  constructor() {
    // Effects pour réactions
    effect(() => {
      console.log('State changed:', this.localState());
    });
  }
  
  ngOnInit(): void {
    // Observables avec cleanup auto
    this.service.getData()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(data => this.localState.set(data));
  }
  
  protected updateState(value: string): void {
    // Utiliser set() ou update(), JAMAIS mutate()
    this.localState.set(value);
    this.dataChange.emit(value);
  }
}
```

---

**Auteur**: GitHub Copilot  
**Version**: Angular 20+ Best Practiceson Recommandé

1. **Phase 1.1 + 1.2** (CRITIQUE): Créer vente auto + afficher insurance-data-bar
2. **Phase 2.1 + 2.2** (HAUTE): ViewChild et focus après sélection produit
3. **Phase 2.3 + 2.4** (HAUTE): Reset et focus après ajout
4. **Phase 3.1 + 3.2** (MOYENNE): Distinction scanner vs sélection
5. **Phase 4** (MOYENNE): Gestion Enter → paiement
6. **Phase 5** (BASSE): Focus BON et compléments

---

## 📌 Notes Importantes

### Règles Métier à Respecter
1. ❌ **NE PAS** ajouter de validation ou logique métier qui n'existe pas dans l'ancien
2. ✅ **COPIER** exactement le workflow de l'ancien module
3. ✅ **TESTER** chaque phase indépendamment avant de passer à la suivante

### Points d'Attention
- **Timing des focus**: Utiliser `setTimeout` avec délai suffisant (100-300ms)
- **Réactivité Angular**: Forcer la détection de changements si nécessaire
- **Gestion mémoire**: Toujours utiliser `takeUntilDestroyed(this.destroyRef)`
- **Événements personnalisés**: Utiliser `output<T>()` pour les communications parent-enfant

### Fichiers Principaux à Modifier
1. `app/features/sales/feature/sale-assurance/sale-assurance.component.ts` (PRINCIPAL)
2. `app/features/sales/ui/insurance-data-bar/insurance-data-bar.component.ts`
3. `app/features/sales/ui/product-search/product-search.component.ts`
4. `app/features/sales/ui/payment-mode/payment-mode.component.ts`
5. `app/features/sales/data-access/facades/sales.facade.ts`

---

**Auteur**: GitHub Copilot  
**Révision**: À valider avec l'équipe avant implémentation
