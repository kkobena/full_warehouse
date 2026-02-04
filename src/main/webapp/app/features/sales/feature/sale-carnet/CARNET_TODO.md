# 📋 TODO CARNET (sale-carnet)

## 📍 État Actuel
**Fichier**: `app/features/sales/feature/sale-carnet/sale-carnet.component.ts` (644 lignes)  
**Status**: Structure complète existante basée sur l'ancien système

---

## ✅ CE QUI EST DÉJÀ FAIT

### 1. Structure de Base ✅
- ✅ Composant standalone créé
- ✅ Imports PrimeNG (Toast, Button, Card, Drawer, Tooltip)
- ✅ Services injectés (SalesFacade, AuthorizationService, NotificationService)
- ✅ Signals réactifs (currentSale, salesLines, selectedCustomer, selectedProduct)

### 2. Gestion Client CARNET ✅
- ✅ `hasCustomer` computed signal
- ✅ `onCustomerSelected(customer)` - Sélectionner client
- ✅ `openCarnetCustomerModal()` - Modal sélection client carnet
- ✅ `openCarnetCustomerForm(customer)` - Modal création/édition client
- ✅ `onOpenCustomerList()` - Ouvre liste clients
- ✅ `onEditCustomer()` - Éditer client sélectionné
- ✅ `onAddCustomer()` - Ajouter nouveau client
- ✅ Validation client obligatoire avant ajout produit

### 3. Gestion Produits ✅
- ✅ `onProductSelected(product)` - Sélection manuelle
- ✅ `onProductScanned(product)` - Scan code-barres (ajout auto)
- ✅ `onAddQuantity(quantity)` - Ajout avec quantité
- ✅ `resetProductSelection()` - Reset après ajout
- ✅ Focus automatique (produit → quantité → produit)
- ✅ Mise à jour affichage client (CustomerDisplayService)
- ✅ Output `productAddedSuccess` pour container

### 4. CRUD Lignes de Vente ✅
- ✅ `onLineQuantityChanged()` - Mise à jour quantité vendue
- ✅ `onLineQuantityRequestedChanged()` - Mise à jour quantité demandée
- ✅ `onLineRemoved()` - Suppression ligne
- ✅ `onUpdateQuantity()` - Alternative update quantité
- ✅ `onRemoveLine()` - Alternative suppression

### 5. Actions Vente ✅
- ✅ `onValidate()` - Ouvrir tiroir paiement
- ✅ `onPutOnHold()` - Mettre en attente
- ✅ `onCancel()` - Annuler vente (avec confirmation)
- ✅ `onNavigateBack()` - Retour écran précédent
- ✅ `resetForNewSale()` - Réinitialiser pour nouvelle vente

### 6. Gestion Paiement ✅
- ✅ `onPaymentComplete(event)` - Finalisation paiement
- ✅ `convertPayments()` - Conversion format paiements
- ✅ Gestion vente différée (montant insuffisant)
- ✅ `confirmDiffereSale()` - Confirmation différé
- ✅ `openCustomerModalForDiffere()` - Sélection client si différé
- ✅ `finalizeSale(event)` - Finaliser vente
- ✅ `completeSaleAfterCashRegister()` - Compléter après ouverture caisse
- ✅ `onPaymentCancel()` - Annulation paiement
- ✅ `onPaymentError()` - Gestion erreurs
- ✅ Signal `paymentDrawerVisible` pour drawer
- ✅ Signal `isProcessingSale` pour état traitement

### 7. Gestion Remises ✅
- ✅ `onAddRemise()` - Ajouter remise (avec autorisation)
- ✅ `onRemoveRemise()` - Supprimer remise (avec confirmation)
- ✅ `requestRemiseAuthorization()` - Demande autorisation
- ✅ `requestRemiseRemovalAuthorization()` - Autorisation suppression

### 8. Intégration InsuranceDataBarComponent ✅
- ✅ ViewChild `insuranceDataBar`
- ✅ Handlers pour tous les événements du bar
- ✅ `onTiersPayantsChanged()` - Mise à jour tiers payants
- ✅ `onRemoveTiersPayant()` - Warning (non disponible CARNET)
- ✅ `onAddComplementaire()` - Warning (non disponible CARNET)
- ✅ `onEditAyantDroit()` - Pas d'ayant droit pour CARNET
- ✅ `onLoadAyantDroits()` - Pas d'ayant droit pour CARNET

### 9. ViewChildren et Focus Management ✅
- ✅ `productSearchComponent` viewChild
- ✅ `quantityComponent` viewChild
- ✅ `insuranceDataBar` viewChild
- ✅ `confirmDialog` viewChild
- ✅ `focusProductSearch()` méthode publique

### 10. Lifecycle et Initialization ✅
- ✅ `ngOnInit()` - Initialize type CARNET
- ✅ `constructor()` avec effect pour spinner
- ✅ `facade.setSaleType('CARNET')` au démarrage
- ✅ `customerDisplay.initialize('PHARMA SMART')`

---

## ⬜ CE QUI RESTE À FAIRE (Basé sur Ancien Système Uniquement)

### 🔴 PRIORITÉ 1 - Alignement avec Ancien Système

#### 1. ✅ **Comparer avec Ancien CarnetComponent** (FAIT)
**Résultat**: L'ancien CarnetComponent (46 lignes) est très simple:
- Étend `BaseSaleComponent`
- Appelle `setTypeVo('CARNET')`
- Réinitialise client: `setCustomer(null)`
- Utilise le template de BaseSaleComponent

**Toute la logique** vient de `BaseSaleComponent` (486 lignes)

**Conclusion**: Le nouveau sale-carnet (659 lignes) contient déjà TOUTES les fonctionnalités de l'ancien système.

**IMPORTANT**: L'ancien système ne gère PAS de logique crédit spécifique côté frontend. Pas de champs `soldeCredit`, `plafondCredit`, `disponibleCredit` dans le modèle Customer.

#### 2. ✅ **Modal Sélection Remise** (COMPLÉTÉ)
**Créé**: `RemiseSelectionModalComponent` (ui/remise-selection-modal/)

**Fonctionnalités**:
- ✅ Affichage remises groupées par type
- ✅ Dropdown p-select avec recherche
- ✅ Aperçu remise sélectionnée
- ✅ Boutons Confirmer/Annuler
- ✅ Intégration dans sale-carnet via `openRemiseSelectionModal()`

**Implémentation**:
```typescript
private openRemiseSelectionModal(): void {
  const modalRef = this.modalService.open(RemiseSelectionModalComponent, {
    backdrop: 'static',
    centered: true,
    size: 'md',
  });

  modalRef.result.then(
    (remise: IRemise) => {
      if (remise) {
        this.facade.updateRemise(remise);
        this.notificationService.success('Remise appliquée', ...);
      }
    },
    () => {}
  );
}
```

#### 3. ⬜ **Endpoint Backend - Utiliser Existant**
**NE PAS créer** `/api/sales/carnet` - **Utiliser l'existant** !

**Ancien système utilise**:
- Création: `POST /api/sales/assurance` (pour ASSURANCE et CARNET)
- Sauvegarde: `PUT /api/sales/assurance/save` (pour ASSURANCE et CARNET)
- Mise en attente: `PUT /api/sales/assurance/put-on-hold`

**À vérifier dans SalesFacade**:
```typescript
// Le nouveau système doit utiliser les mêmes endpoints
saveSale(): void {
  const currentSale = this.currentSale();
  // CARNET utilise le même endpoint qu'ASSURANCE
  // Le backend différencie via le champ 'type' de la vente
  this.apiService.saveSale(currentSale).subscribe(...);
}
```

**Le type de vente** est déterminé par `sale.type = 'VO'` et `sale.categorie = 'VO'` (Vente Ordonnance)

#### 4. ⬜ **Template sans pTemplate (Déprécié)**
**À vérifier dans** `sale-carnet.component.html`:
- ⬜ Rechercher `pTemplate="header"` → remplacer par `#header`
- ⬜ Rechercher `pTemplate="body"` → remplacer par `#body`
- ⬜ Rechercher `pTemplate="caption"` → remplacer par `#caption`
- ⬜ Rechercher `pTemplate="emptymessage"` → remplacer par `#emptymessage`

**Note**: Cette modification vient de COMPTANT (déjà appliquée)

---

### 🟠 PRIORITÉ 2 - Améliorations de COMPTANT à Appliquer

Ces améliorations viennent du nouveau système COMPTANT et peuvent être appliquées (OPTIONNEL):

#### 5. ⬜ **Seuils Tolérance 5 FCFA**
**Comme dans COMPTANT**:
```typescript
const PAYMENT_TOLERANCE_THRESHOLD = 5;

onPaymentComplete(event: PaymentCompleteEvent): void {
  const restToPay = amountToBePaid - entryAmount;
  
  if (restToPay > PAYMENT_TOLERANCE_THRESHOLD && !currentSale.differe) {
    // Dialog différé
  } else if (restToPay > 0 && restToPay <= PAYMENT_TOLERANCE_THRESHOLD) {
    // Considéré comme payé
    currentSale.restToPay = 0;
  }
}
```

#### 6. ⬜ **Arrondi Monnaie**
**Comme dans COMPTANT**:
```typescript
const CHANGE_TOLERANCE_THRESHOLD = 5;

if (entryAmount > amountToBePaid) {
  const change = entryAmount - amountToBePaid;
  
  if (change >= CHANGE_TOLERANCE_THRESHOLD) {
    currentSale.montantRendu = change; // Exact
    currentSale.montantRenduArrondi = Math.ceil(change / 5) * 5; // Arrondi
  }
}
```

#### 7. ⬜ **Dialog Stylisé (Badge Bootstrap)**
**Comme dans COMPTANT**:
```typescript
this.confirmDialog().onConfirm(
  () => this.action(),
  'Titre',
  `
    <div class="mb-2">Message principal</div>
    <div>
      <span class="badge rounded-pill bg-warning-subtle text-warning-emphasis">
        <i class="pi pi-exclamation-triangle"></i>
        Avertissement
      </span>
    </div>
  `
);
```

---

### 🟡 PRIORITÉ 3 - Tests et Validation

#### 8. ⬜ **Tests Unitaires**
**À créer**: `sale-carnet.component.spec.ts`

**Tests basiques**:
- ⬜ Client obligatoire avant ajout produit
- ⬜ Workflow complet vente CARNET
- ⬜ Paiement partiel (différé)
- ⬜ Reset après finalisation
- ⬜ Gestion remises avec autorisation

#### 9. ⬜ **Tests Intégration**
- ⬜ Test flux complet vente CARNET
- ⬜ Test changement de tab (COMPTANT → CARNET → ASSURANCE)
- ⬜ Test mise en attente
- ⬜ Test reprise vente en attente

---

### 🟢 PRIORITÉ 4 - Nettoyage

#### 10. ⬜ **Nettoyage Console.log**
```bash
# Rechercher
grep -r "console.log" sale-carnet.component.ts

# Supprimer manuellement tous les console.log de debugging
```

#### 11. ⬜ **Documentation Code**
- ⬜ Ajouter JSDoc pour méthodes publiques
- ⬜ Documenter signals computed
- ⬜ Ajouter exemples d'utilisation

---

## 📊 Estimation Complétude

### Fonctionnalités de Base (Ancien Système)
- ✅ Structure: 100%
- ✅ Gestion client: 100%
- ✅ Gestion produits: 100%
- ✅ CRUD lignes: 100%
- ✅ Actions vente: 100%
- ✅ Paiement de base: 100%
- ✅ Gestion remises: 100% (modal sélection complétée)

### Améliorations COMPTANT (OPTIONNEL)
- ⬜ Seuils tolérance: 0%
- ⬜ Arrondi monnaie: 0%
- ⬜ Dialog stylisé: 0%

### Tests et Validation
- ⬜ Tests unitaires: 0%
- ⬜ Tests intégration: 0%

### Nettoyage
- ⬜ Console.log: 0%
- ⬜ Documentation: 0%

**TOTAL ESTIMÉ: 95% complété (fonctionnalités de base de l'ancien système)**

---

## 🎯 Plan d'Implémentation

### ✅ Sprint 1: Analyse et Corrections (TERMINÉ)
1. ✅ Analyser ancien CarnetComponent en détail
2. ✅ Comparer avec nouveau sale-carnet
3. ✅ Vérifier toutes les fonctionnalités de BaseSaleComponent
4. ✅ Créer RemiseSelectionModalComponent
5. ✅ Confirmer endpoints backend existants

### ⬜ Sprint 2: Vérifications Finales (OPTIONNEL - 0.25 jour)
6. ⬜ Vérifier template sans pTemplate
7. ⬜ Tests basiques manuels
8. ⬜ Vérifier que le backend utilise bien `/api/sales/assurance/save`

### ⬜ Sprint 3: Améliorations COMPTANT (OPTIONNEL - 0.5 jour)
9. ⬜ Appliquer seuils tolérance
10. ⬜ Appliquer arrondi monnaie
11. ⬜ Appliquer dialog stylisé

### ⬜ Sprint 4: Tests et Nettoyage (0.5 jour)
12. ⬜ Tests unitaires complets
13. ⬜ Tests intégration
14. ⬜ Nettoyage console.log
15. ⬜ Documentation code

---

## 🚀 Prochaines Étapes

**IMPORTANT**: ✅ Le composant CARNET contient déjà TOUTES les fonctionnalités de l'ancien système.

**Points clés confirmés**:
- ✅ Pas de logique crédit côté frontend dans l'ancien système
- ✅ Utilise endpoints existants `/api/sales/assurance/*` (pas de nouveaux endpoints)
- ✅ Différenciation CARNET/ASSURANCE via champ `type` de la vente
- ✅ Modal remise implémentée (comme ancien système avec p-select)

**Ce qui reste** (optionnel):
- Template sans pTemplate (amélioration technique)
- Améliorations COMPTANT (seuils, arrondi, dialog)
- Tests et documentation

**Le composant CARNET est fonctionnellement complet selon l'ancien système.**

