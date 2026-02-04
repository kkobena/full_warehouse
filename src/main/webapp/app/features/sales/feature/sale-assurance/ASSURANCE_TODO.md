# 📋 TODO ASSURANCE (sale-assurance)

## 📍 État Actuel
**Fichier**: `app/features/sales/feature/sale-assurance/sale-assurance.component.ts` (797 lignes)  
**Status**: Fichier existant non analysé en détail

---

## 🔍 PHASE 1: ANALYSE DÉTAILLÉE (EN COURS)

### Étapes d'Analyse
1. ⬜ Lire tout le fichier sale-assurance.component.ts (797 lignes)
2. ⬜ Identifier structure actuelle
3. ⬜ Comparer avec ancien AssuranceComponent (entities/sales)
4. ⬜ Lister ce qui est fait vs ce qui manque
5. ⬜ Créer checklist précise

**Note**: Ce fichier sera complété après analyse détaillée du composant existant.

---

## 📋 CE QUI DEVRAIT ÊTRE FAIT (Basé sur Analyse Ancien Système)

### Fonctionnalités Spécifiques ASSURANCE

#### 1. ⬜ **Client Assuré OBLIGATOIRE dès Départ**
**Différence critique avec COMPTANT**:
- COMPTANT: Client optionnel → obligatoire si différé
- ASSURANCE: Client assuré **OBLIGATOIRE dès le départ**

**Workflow**:
```
1. Ouvrir ASSURANCE
2. Modal sélection client IMMÉDIATE
3. Si pas de client → Retour accueil
4. Si client → Vérifier tiers payants
5. Si pas de tiers payants → Error ou conversion
```

**À implémenter**:
```typescript
ngOnInit(): void {
  this.facade.setSaleType('ASSURANCE');
  
  // Force sélection client immédiate
  if (!this.selectedCustomer()) {
    this.openAssuranceCustomerModal();
  }
}

private openAssuranceCustomerModal(): void {
  // Modal DOIT retourner client avec tiers payants
  const modalRef = this.modalService.open(AssuranceCustomerListComponent, {
    backdrop: 'static',
    keyboard: false, // ← Pas d'échappement
    centered: true
  });
  
  modalRef.result.then(
    (customer) => {
      if (!this.isClientAssure(customer)) {
        this.notificationService.error(
          'Client non assuré',
          'Ce client n\'a pas de tiers payants actifs'
        );
        this.router.navigate(['/']); // Retour accueil
        return;
      }
      this.facade.setCustomer(customer);
    },
    () => {
      // Modal fermée sans sélection → Retour accueil OBLIGATOIRE
      this.notificationService.warning(
        'Vente annulée',
        'Un client assuré est obligatoire pour une vente ASSURANCE'
      );
      this.router.navigate(['/']);
    }
  );
}

private isClientAssure(customer: ICustomer): boolean {
  return customer.tiersPayants && 
         customer.tiersPayants.length > 0 &&
         customer.tiersPayants.some(tp => tp.enable);
}
```

#### 2. ⬜ **Gestion Tiers Payants**
**Types de tiers payants**:
- **Principal**: Obligatoire (ex: Mutuelle principale)
- **Complémentaire**: Optionnel (ex: Mutuelle secondaire)

**Données à gérer**:
```typescript
interface IClientTiersPayant {
  id: number;
  tiersPayantId: number;
  tiersPayantName: string;
  num: string; // Numéro carte/adhésion
  tauxCouverture: number; // Pourcentage couverture (ex: 80%)
  plafond?: number; // Plafond mensuel/annuel
  enable: boolean; // Actif ou non
  isPrincipal: boolean; // Principal ou complémentaire
}
```

**Calculs effectués par le Backend**:
```typescript
// Le backend retourne déjà ces valeurs calculées:
// - partAssurance: montant pris en charge par les tiers payants
// - partClient: montant à payer par le client
// - montantPrisEnCharge par tiers payant

// Exemple réponse backend:
// {
//   salesAmount: 100000,
//   partAssurance: 80000,    // ← Calculé côté backend
//   partClient: 20000,        // ← Calculé côté backend
//   tiersPayants: [
//     {
//       tiersPayantId: 45,
//       montantPrisEnCharge: 80000  // ← Calculé côté backend
//     }
//   ]
// }
```

**Signals à utiliser** (valeurs venant du backend):
```typescript
readonly partAssurance = computed(() => this.currentSale()?.partAssurance || 0);
readonly partClient = computed(() => this.currentSale()?.partClient || 0);
readonly tiersPayants = computed(() => this.currentSale()?.tiersPayants || []);
```

**⚠️ IMPORTANT**: Ne PAS recalculer côté frontend. Le backend gère tous les calculs de répartition assurance/client.

#### 3. ⬜ **InsuranceDataBarComponent - Affichage Détaillé**
**À afficher dans le bar**:
```html
<app-insurance-data-bar
  #insuranceDataBar
  [customer]="selectedCustomer()"
  [tiersPayants]="tiersPayants()"
  [partAssurance]="partAssurance()"
  [partClient]="partClient()"
  [salesAmount]="currentSale()?.salesAmount"
  (customerSelected)="onCustomerSelected($event)"
  (openCustomerList)="onOpenCustomerList()"
  (addComplementaire)="onAddComplementaire()"
  (removeTiersPayant)="onRemoveTiersPayant($event)"
  (editCustomer)="onEditCustomer()"
  (editAyantDroit)="onEditAyantDroit()"
/>
```

**Informations à afficher**:
- Nom client + numéro carte
- Liste tiers payants (principal + complémentaires)
- Montant total vente
- Part assurance (avec détail par tiers payant)
- Part client (à payer)
- Boutons actions: Ajouter complémentaire, Supprimer tiers payant

#### 4. ⬜ **Paiement Part Client UNIQUEMENT**
**Différence critique**:
- COMPTANT: Paiement du TOTAL vente
- ASSURANCE: Paiement de la PART CLIENT uniquement

**À adapter dans PaymentModeComponent**:
```typescript
// Passer amountToBePaid = partClient (pas salesAmount)
<app-payment-mode
  [amountToBePaid]="partClient()"
  [saleType]="'ASSURANCE'"
  (paymentComplete)="onPaymentComplete($event)"
/>
```

**Calcul monnaie**:
```typescript
onPaymentComplete(event: PaymentCompleteEvent): void {
  const partClient = this.partClient();
  const entryAmount = event.totalPaid || 0;
  const restToPay = partClient - entryAmount; // ← Sur part client seulement
  
  // ... rest of logic
}
```

#### 5. ⬜ **Validation et Finalisation ASSURANCE**
**Backend endpoint**: `/api/sales/assurance` (POST)

**Payload spécifique**:
```json
{
  "customerId": 123,
  "salesLines": [...],
  "payments": [...],
  "tiersPayants": [
    {
      "tiersPayantId": 45,
      "tauxCouverture": 80,
      "montantPrisEnCharge": 80000,
      "isPrincipal": true
    }
  ],
  "montantVerse": 20000,
  "partAssurance": 80000,
  "partClient": 20000,
  "salesAmount": 100000,
  "type": "ASSURANCE"
}
```

**À créer dans SalesFacade**:
```typescript
saveAssuranceSale(): Observable<ISales> {
  const currentSale = this.currentSale();
  if (currentSale.type !== 'ASSURANCE') {
    throw new Error('Sale type must be ASSURANCE');
  }
  
  // Calculer parts avant envoi
  currentSale.partAssurance = this.calculatePartAssurance();
  currentSale.partClient = this.calculatePartClient();
  
  return this.apiService.saveAssuranceSale(currentSale);
}
```

#### 6. ⬜ **Modal Clients Assurés**
**Composant**: Créer `AssuranceCustomerListComponent` (ou adapter existant)

**Critères de sélection**:
- ✅ Client DOIT avoir au moins 1 tiers payant
- ✅ Tiers payant DOIT être actif (`enable: true`)
- ✅ Afficher info tiers payant dans liste
- ✅ Filtre par nom/téléphone/numéro carte

**Template**:
```html
<p-table [value]="assuredCustomers">
  <ng-template #header>
    <tr>
      <th>Client</th>
      <th>Téléphone</th>
      <th>Tiers Payant</th>
      <th>N° Carte</th>
      <th>Taux</th>
      <th>Action</th>
    </tr>
  </ng-template>
  <ng-template #body let-customer>
    <tr>
      <td>{{ customer.firstName }} {{ customer.lastName }}</td>
      <td>{{ customer.phone }}</td>
      <td>
        <div *ngFor="let tp of customer.tiersPayants">
          <span class="badge bg-primary">{{ tp.tiersPayantName }}</span>
        </div>
      </td>
      <td>{{ customer.tiersPayants[0]?.num }}</td>
      <td>{{ customer.tiersPayants[0]?.tauxCouverture }}%</td>
      <td>
        <p-button 
          (onClick)="selectCustomer(customer)"
          icon="pi pi-check"
          severity="success"
        />
      </td>
    </tr>
  </ng-template>
</p-table>
```

#### 7. ⬜ **Gestion Ayants Droit**
**Concept**: Un client principal peut avoir des ayants droit (enfants, conjoint)

**À implémenter**:
```typescript
onEditAyantDroit(): void {
  const customer = this.selectedCustomer();
  if (!customer) return;
  
  const modalRef = this.modalService.open(AyantDroitFormComponent, {
    size: 'lg',
    backdrop: 'static'
  });
  modalRef.componentInstance.customerId = customer.id;
  
  modalRef.result.then(
    (ayantDroit) => {
      if (ayantDroit) {
        // Sélectionner l'ayant droit comme bénéficiaire
        this.facade.setAyantDroit(ayantDroit);
      }
    }
  );
}

onLoadAyantDroits(): void {
  const customer = this.selectedCustomer();
  if (!customer || !customer.id) return;
  
  this.customerService.getAyantsDroits(customer.id).subscribe(
    ayantsDroits => {
      // Afficher liste pour sélection
      this.showAyantDroitsList(ayantsDroits);
    }
  );
}

private showAyantDroitsList(ayantsDroits: IAyantDroit[]): void {
  const modalRef = this.modalService.open(AyantDroitListComponent, {
    size: 'md'
  });
  modalRef.componentInstance.ayantsDroits = ayantsDroits;
  
  modalRef.result.then(
    (selected) => {
      if (selected) {
        this.facade.setAyantDroit(selected);
      }
    }
  );
}
```

#### 8. ⬜ **Ajout Tiers Payant Complémentaire**
**Méthode**:
```typescript
onAddComplementaire(): void {
  const customer = this.selectedCustomer();
  if (!customer) return;
  
  // Modal sélection tiers payant complémentaire
  const modalRef = this.modalService.open(TiersPayantSelectionComponent, {
    size: 'lg',
    backdrop: 'static'
  });
  modalRef.componentInstance.customerId = customer.id;
  modalRef.componentInstance.excludeExisting = this.tiersPayants().map(tp => tp.tiersPayantId);
  
  modalRef.result.then(
    (tiersPayant) => {
      if (tiersPayant) {
        this.addTiersPayantToSale(tiersPayant);
      }
    }
  );
}

private addTiersPayantToSale(tiersPayant: IClientTiersPayant): void {
  const currentSale = this.currentSale();
  if (!currentSale) return;
  
  const existingTiersPayants = currentSale.tiersPayants || [];
  currentSale.tiersPayants = [...existingTiersPayants, {
    ...tiersPayant,
    isPrincipal: false // Complémentaire
  }];
  
  // Recalculer parts
  this.notificationService.success(
    'Tiers payant ajouté',
    `${tiersPayant.tiersPayantName} (${tiersPayant.tauxCouverture}%)`
  );
}
```

#### 9. ⬜ **Suppression Tiers Payant**
**Règles**:
- ⚠️ Impossible de supprimer le tiers payant PRINCIPAL
- ✅ Possible de supprimer les complémentaires

**Méthode**:
```typescript
onRemoveTiersPayant(tiersPayant: IClientTiersPayant): void {
  if (tiersPayant.isPrincipal) {
    this.notificationService.error(
      'Suppression impossible',
      'Impossible de supprimer le tiers payant principal. Pour une vente sans assurance, utilisez le mode COMPTANT.'
    );
    return;
  }
  
  this.confirmDialog().onConfirm(
    () => {
      const currentSale = this.currentSale();
      if (!currentSale) return;
      
      currentSale.tiersPayants = currentSale.tiersPayants?.filter(
        tp => tp.id !== tiersPayant.id
      );
      
      this.notificationService.success(
        'Tiers payant retiré',
        `${tiersPayant.tiersPayantName} a été retiré de la vente`
      );
    },
    'Retirer tiers payant',
    `Êtes-vous sûr de vouloir retirer ${tiersPayant.tiersPayantName} ?`
  );
}
```

---

## ⬜ AMÉLIORATIONS DE COMPTANT À APPLIQUER

### 1. ⬜ **Seuils Tolérance 5 FCFA (sur part client)**
```typescript
const PAYMENT_TOLERANCE_THRESHOLD = 5;

onPaymentComplete(event: PaymentCompleteEvent): void {
  const partClient = this.partClient();
  const entryAmount = event.totalPaid || 0;
  const restToPay = partClient - entryAmount;
  
  if (restToPay > PAYMENT_TOLERANCE_THRESHOLD && !currentSale.differe) {
    // Dialog différé
  } else if (restToPay > 0 && restToPay <= PAYMENT_TOLERANCE_THRESHOLD) {
    // Considéré comme payé
    currentSale.restToPay = 0;
  }
}
```

### 2. ⬜ **Arrondi Monnaie (sur part client)**
```typescript
const CHANGE_TOLERANCE_THRESHOLD = 5;

// Si client paie plus que part client
if (entryAmount > partClient) {
  const change = entryAmount - partClient;
  
  if (change >= CHANGE_TOLERANCE_THRESHOLD) {
    currentSale.montantRendu = change; // Exact
    currentSale.montantRenduArrondi = Math.ceil(change / 5) * 5; // Arrondi
  }
}
```

### 3. ⬜ **Double Monnaie (exact + arrondi)**
Déjà dans sales.model.ts:
```typescript
export interface ISales {
  montantRendu?: number;
  montantRenduArrondi?: number;
}
```

### 4. ⬜ **Dialog Stylisé Bootstrap**
```typescript
this.confirmDialog().onConfirm(
  () => this.action(),
  'Confirmation',
  `
    <div class="mb-3">
      <div class="mb-2">Montant total: <strong>${salesAmount} FCFA</strong></div>
      <div class="mb-2">
        <span class="badge rounded-pill bg-info-subtle text-info-emphasis me-2">
          <i class="pi pi-shield"></i>
          Part assurance: ${partAssurance} FCFA
        </span>
      </div>
      <div>
        <span class="badge rounded-pill bg-warning-subtle text-warning-emphasis">
          <i class="pi pi-wallet"></i>
          Part client: ${partClient} FCFA
        </span>
      </div>
    </div>
  `
);
```

### 5. ⬜ **Signal isDiffere**
```typescript
readonly isDiffere = signal<boolean>(false);

onPaymentComplete(event: PaymentCompleteEvent): void {
  // ...
  if (currentSale.differe) {
    this.isDiffere.set(true);
  }
}

// Passer au PaymentModeComponent
<app-payment-mode [isDiffere]="isDiffere()" />
```

### 6. ⬜ **Modal Client Autonome**
Utiliser `CustomerSelectionModalComponent` créé pour COMPTANT, mais:
- Filtrer uniquement clients avec tiers payants
- Afficher info tiers payant

### 7. ⬜ **Template sans pTemplate**
Vérifier et remplacer:
- `pTemplate="header"` → `#header`
- `pTemplate="body"` → `#body`
- `pTemplate="caption"` → `#caption`

### 8. ⬜ **Style Lignes Avoir**
```html
<tr [class.pharma-row-warning]="line.quantitySold < line.quantityRequested">
```

---

## 🔍 ANALYSE À EFFECTUER

### Fichiers à Lire
1. ⬜ `sale-assurance.component.ts` (797 lignes) - COMPLET
2. ⬜ `sale-assurance.component.html` - Template
3. ⬜ `sale-assurance.component.scss` - Styles
4. ⬜ `insurance-data-bar.component.ts` - Composant UI bar assurance
5. ⬜ `insurance-data-bar.component.html` - Template bar

### Comparaison à Faire
- ⬜ Ancien `AssuranceComponent` (extends BaseSaleComponent) vs Nouveau
- ⬜ Ancien `assurance-data.component.ts` vs Nouveau `insurance-data-bar`
- ⬜ Workflow ancien vs nouveau

### Checklist à Créer
Après analyse, créer checklist détaillée:
```
✅ Ce qui est fait
⬜ Ce qui manque
🔄 Ce qui est partiel
❌ Ce qui est incorrect
```

---

## 📊 Estimation Complétude (À Déterminer Après Analyse)

**Actuellement**: Inconnu (fichier pas analysé)

**Estimation approximative basée sur pattern CARNET**:
- Structure de base: 50-80% (composant existe)
- Fonctionnalités spécifiques ASSURANCE: 20-40%
- Améliorations COMPTANT: 0%
- Tests: 0%

**À affiner après analyse détaillée**.

---

## 🎯 Plan d'Implémentation (Après Analyse)

### Phase 1: Analyse Complète (0.5 jour)
1. ✅ Lire tout sale-assurance.component.ts
2. ✅ Identifier structure actuelle
3. ✅ Lister fonctionnalités présentes
4. ✅ Créer checklist détaillée
5. ✅ Comparer avec ancien système

### Phase 2: Fonctionnalités Critiques (2-3 jours)
1. ⬜ Client assuré obligatoire dès départ
2. ⬜ Calcul parts assurance/client
3. ⬜ InsuranceDataBar complet
4. ⬜ Paiement part client uniquement
5. ⬜ Backend endpoint ASSURANCE

### Phase 3: Gestion Tiers Payants (1-2 jours)
6. ⬜ Modal clients assurés
7. ⬜ Ajout complémentaire
8. ⬜ Suppression complémentaire
9. ⬜ Gestion ayants droit
10. ⬜ Validation tiers payants

### Phase 4: Améliorations COMPTANT (1 jour)
11. ⬜ Seuils tolérance 5 FCFA
12. ⬜ Arrondi monnaie
13. ⬜ Dialog stylisé
14. ⬜ Template moderne
15. ⬜ Signal isDiffere

### Phase 5: Fonctionnalités Avancées (1-2 jours)
16. ⬜ Impression facture ASSURANCE (2 factures)
17. ⬜ Mise en attente
18. ⬜ Historique ventes assurance client
19. ⬜ Tests unitaires
20. ⬜ Nettoyage et polish

---

## 🚀 Prochaines Étapes

1. **PRIORITAIRE**: Analyser sale-assurance.component.ts complet
2. Créer checklist détaillée basée sur analyse
3. Commencer implémentation fonctionnalités critiques
4. Tester avec cas réels

**Note**: Ce fichier sera mis à jour après analyse détaillée.
