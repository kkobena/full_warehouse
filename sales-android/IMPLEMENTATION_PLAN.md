# Plan d'Implémentation Workflows Vente Mobile

## ✅ Déjà Implémenté

### WORKFLOW 3 : Gestion Client
- ✅ Recherche clients (uninsured/assured)
- ✅ Sélection client avec CustomerSelectionDialog
- ✅ Affichage client dans include_customer_info_display.xml (expand/collapse)
- ✅ Gestion tiers payants (affichage, ajout, modification, suppression)
- ✅ Gestion ayant droit (sélection, création)

### Interface Utilisateur
- ✅ Layout optimisé pour petit écran
- ✅ Sections expand/collapse (Client, Panier)
- ✅ Type de vente selector (Comptant, Assurance, Carnet)
- ✅ Recherche produit
- ✅ Panier avec RecyclerView

## ❌ À Implémenter

### WORKFLOW 1 : Ajout de Produit
#### 1.1 Validation Stock (CRITIQUE)
- ❌ Dialog validation stock avec options :
  - Stock insuffisant → Proposer Force Stock (si autorisé)
  - Quantité excessive → Proposer Force Stock (si autorisé)
  - Déconditionnement → Proposer déconditionnement
- ❌ Logique de validation stock multi-niveaux
- ❌ Vérification permission force stock utilisateur

#### 1.2 Dialogs Spécifiques
- ❌ `ForceStockDialog` - Confirmation force stock
- ❌ `DeconditionnementDialog` - Gestion déconditionnement

### WORKFLOW 2 : Modification de Produit
#### 2.1 Modification Quantité
- ❌ Dialog modification quantité avec validation stock
- ❌ API call `PUT /api/sales/comptant/update-item-quantity`

#### 2.2 Modification Prix (AVEC AUTORISATION)
- ❌ Vérifier permission `PR_MODIFICATION_PRIX_VENTE`
- ❌ Si non autorisé → Dialog demande credentials
- ❌ Dialog saisie nouveau prix
- ❌ API call `POST /api/sales/comptant/update-item-price`

#### 2.3 Suppression Ligne (AVEC AUTORISATION)
- ❌ Vérifier permission `PR_SUPPRIME_PRODUIT_VENTE`
- ❌ Si non autorisé → Dialog demande credentials
- ❌ Dialog confirmation suppression
- ❌ API call `DELETE /api/sales/comptant/delete-item/{id}`

### WORKFLOW 5 : Finalisation Vente
#### 5.1 Finalisation COMPTANT
- ❌ Dialog sélection mode(s) paiement
- ❌ Gestion paiement multiple (max 2 modes)
- ❌ Calcul monnaie à rendre
- ❌ Validation caisse ouverte
- ❌ API call `POST /api/sales/comptant/finalize`
- ❌ Impression ticket (optionnel)

#### 5.2 Finalisation ASSURANCE/CARNET
- ❌ Vérifications pré-finalisation :
  - Client sélectionné
  - Tiers payants présents
  - Numéros de bon renseignés
  - Plafond non dépassé
- ❌ Calcul répartition (Part Assurance / Part Client)
- ❌ Dialog saisie paiement part client
- ❌ API call `POST /api/sales/vo/finalize`

### WORKFLOW 6 : Transformation de Vente
- ❌ Bouton "Transformer en Assurance" (visible en mode Comptant)
- ❌ Dialog sélection client assuré
- ❌ API call `POST /api/sales/transform`

### WORKFLOW 7 : Modes de Paiement
- ❌ `PaymentModesDialog` - Sélection et saisie montants
- ❌ Validation somme = montant total
- ❌ Support multi-paiement (max 2)
- ❌ Champs spécifiques :
  - CHEQUE → Numéro chèque
  - MOBILE → Numéro transaction
  - VIREMENT → Référence

## 📋 Ordre d'Implémentation Recommandé

### Phase 1 : Actions Panier (PRIORITÉ HAUTE)
1. Modification quantité produit (avec validation stock)
2. Suppression ligne (avec autorisation si requis)
3. Modification prix (avec autorisation si requis)

### Phase 2 : Validation Stock & Dialogs
4. Dialog validation stock (Force Stock / Déconditionnement)
5. Dialog demande autorisation (credentials)

### Phase 3 : Finalisation (PRIORITÉ CRITIQUE)
6. Dialog paiement (modes + montants)
7. Finalisation COMPTANT
8. Finalisation ASSURANCE/CARNET

### Phase 4 : Fonctionnalités Avancées
9. Transformation de vente
10. Vente différée
11. Impression ticket

## 🔧 Composants à Créer

### Dialogs
- `ProductQuantityDialog.kt` - Modification quantité
- `ProductPriceDialog.kt` - Modification prix
- `AuthorizationDialog.kt` - Demande credentials utilisateur autorisé
- `PaymentModesDialog.kt` - Sélection modes paiement
- `StockValidationDialog.kt` - Confirmation force stock / déconditionnement

### ViewModel Methods
- `updateProductQuantity(saleLineId, quantity)`
- `updateProductPrice(saleLineId, price, authUserId?)`
- `removeProductLine(saleLineId, authUserId?)`
- `finalizeSale(payments)`
- `transformSaleType(newType, customerId?)`

### Repository Methods
- Déjà existants dans SalesRepository

## 📝 Notes Importantes

1. **Autorisations** :
   - Vérifier permissions en local (TokenManager + user permissions)
   - Si non autorisé, demander credentials autre utilisateur

2. **Validation Stock** :
   - Toujours valider avant ajout/modification quantité
   - Proposer force stock si autorisé
   - Bloquer si non autorisé

3. **Types de Vente** :
   - COMPTANT : Client optionnel
   - ASSURANCE/CARNET : Client + Tiers Payants obligatoires

4. **Paiement** :
   - Max 2 modes de paiement
   - Somme exacte = montant total
   - Gérer monnaie à rendre (COMPTANT)

5. **Caisse** :
   - Vérifier caisse ouverte avant finalisation
   - Si fermée, proposer ouverture ou bloquer
