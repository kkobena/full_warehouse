# Plan d'action : Ventes Assurance & Carnet - Module Mobile

## Etat des lieux

### Ce qui est deja implemente (Mobile)

| Composant | Assurance | Carnet | Notes |
|-----------|-----------|--------|-------|
| SaleType sealed class | OK | OK | Comptant/Assurance/Carnet avec validation |
| Modeles de donnees (Sale, Customer, ClientTiersPayant, TiersPayant, ThirdPartySaleLine, InsuranceData, CarnetData, PrioriteTiersPayant) | OK | OK | Complets et bien conçus |
| API endpoints (SalesApiService) | OK | OK | POST/PUT /api/sales/assurance, add-item, save, put-on-hold, add-remise, finalize-prevente |
| API endpoints (CustomerApiService) | OK | OK | assured, uninsured, tiers-payants, ayant-droits |
| API endpoints (TiersPayantApiService) | OK | OK | CRUD complet |
| Repositories (SalesRepository, CustomerRepository, TiersPayantRepository) | OK | OK | Result<T> pattern, 60+ methodes |
| UnifiedSaleViewModel - gestion type | OK | OK | changeSaleType, routage API par type |
| UnifiedSaleViewModel - client | OK | OK | Recherche assured/uninsured, selection, remplacement |
| UnifiedSaleViewModel - tiers payants | OK | OK | add/remove/update taux/numBon/num |
| UnifiedSaleViewModel - produits | OK | OK | Ajout avec routage comptant vs VO |
| UnifiedSaleViewModel - finalisation | OK | OK | finalizeComptantSale / finalizeVOSale |
| UnifiedSaleViewModel - prevente | OK | OK | putOnHold, finalizePrevente |
| UnifiedSaleViewModel - force stock / deconditionnement | OK | OK | Gestion erreurs backend |
| UI - ChipGroup type de vente | OK | OK | Comptant/Assurance/Carnet |
| UI - Recherche client assured | OK | OK | Debounce, RecyclerView resultats |
| UI - Affichage tiers payants (CustomerTiersPayantAdapter) | OK | OK | Mode Assurance (rang, boutons) vs Carnet (simplifie) |
| UI - Edition numBon (prescription) | OK | - | TextWatcher dans adapter |
| UI - Layout ayant droit (XML) | OK | - | Boutons Select/Create/Remove existent |
| UI - Payment dialog | OK | OK | Montant base sur amountToBePaid |
| Impression recu | OK | OK | Sunmi printer, tous types |

---

## Ecarts Web vs Mobile

### Priorite 1 - Bloquant (fonctionnalite de base incomplete)

#### 1.1 Gestion des ayants droit (Assurance uniquement)

**Web :** Workflow complet - chargement liste ayants droit, selection, creation via formulaire, modification, suppression. L'ayant droit est inclus dans la vente (`ayantDroitId`).

**Mobile :** Le layout XML existe avec les boutons (Select, Create, Remove). Le ViewModel a `_selectedAyantDroit` avec `selectAyantDroit()` / `removeAyantDroit()`. **Mais aucun appel API pour charger la liste des ayants droit du client, ni dialog de creation.**

**Actions :**
- [ ] Implementer `loadAyantDroits(customerId)` dans le ViewModel (appel `CustomerApiService.getAyantDroits`)
- [ ] Creer un Dialog/BottomSheet `AyantDroitSelectionDialog` avec liste des ayants droit
- [ ] Brancher le bouton "Selectionner" de `include_customer_info_display.xml` sur ce dialog
- [ ] Creer un Dialog `AyantDroitCreateDialog` avec formulaire (nom, prenom, lien de parente)
- [ ] Brancher le bouton "Creer" sur ce dialog
- [ ] S'assurer que `Sale.ayantDroitId` est renseigne lors de la finalisation VO
- [ ] Brancher le bouton "Supprimer" sur `viewModel.removeAyantDroit()`

**Fichiers impactes :**
- `ui/viewmodel/UnifiedSaleViewModel.kt` - ajout methodes
- `ui/activity/UnifiedSaleActivity.kt` - branchement boutons
- `ui/dialog/AyantDroitSelectionDialog.kt` - **nouveau**
- `ui/dialog/AyantDroitCreateDialog.kt` - **nouveau**
- `res/layout/dialog_ayant_droit_selection.xml` - **nouveau**
- `res/layout/dialog_ayant_droit_create.xml` - **nouveau**
- `res/layout/item_ayant_droit.xml` - **nouveau**
- `ui/adapter/AyantDroitAdapter.kt` - **nouveau**

---

#### 1.2 Decouverte / Ajout de tiers payant (Assurance)

**Web :** L'utilisateur peut ajouter un tiers payant complementaire depuis la barre assurance (`AddComplementaireComponent`). Recherche dans la liste systeme, selection, ajout a la vente. API: `PUT /api/sales/add-assurance/{saleId}/{saleDate}`.

**Mobile :** Le bouton "Ajouter tiers payant" existe dans le layout mais **n'a pas de handler implemente**. L'API existe (`SalesApiService`).

**Actions :**
- [ ] Creer `TiersPayantSearchDialog` - dialog avec recherche et liste des tiers payants systeme
- [ ] Brancher le bouton "Ajouter tiers payant" sur ce dialog
- [ ] Appeler `viewModel.addClientTiersPayant()` apres selection
- [ ] Si vente existe sur backend : appeler API add-assurance puis recharger la vente
- [ ] Si vente locale : ajouter au store local
- [ ] Mettre a jour la liste `CustomerTiersPayantAdapter` apres ajout

**Fichiers impactes :**
- `ui/dialog/TiersPayantSearchDialog.kt` - **nouveau**
- `res/layout/dialog_tiers_payant_search.xml` - **nouveau**
- `res/layout/item_tiers_payant_search.xml` - **nouveau**
- `ui/adapter/TiersPayantSearchAdapter.kt` - **nouveau**
- `ui/activity/UnifiedSaleActivity.kt` - branchement bouton
- `ui/viewmodel/UnifiedSaleViewModel.kt` - methode addTiersPayantToSale avec appel API

---

### Priorite 2 - Important (affecte l'utilisabilite)

#### 2.1 Affichage du detail de couverture (Assurance/Carnet)



**Web :** Le `SaleSummaryComponent` affiche : Total | Part tiers payant (detail par assureur si multiple) | Part client (a payer). Le `PaymentModeComponent` n'affiche le paiement que si `amountToBePaid > 0`.

**Mobile :** Le `PaymentDialogFragment` recoit le montant a payer mais **n'affiche pas le detail de la repartition** (part assurance vs part client).

**Actions :**
- [ ] Ajouter dans la zone total de l'Activity (ou dans le PaymentDialog) : "Part assurance: X FCFA" / "Part client: X FCFA"
- [ ] Si multiple tiers payants : afficher le detail par assureur (nom + montant)
- [ ] Utiliser `Sale.partTiersPayant`, `Sale.amountToBePaid`, `Sale.thirdPartySaleLines`
- [ ] Pour Carnet : afficher "Pris en charge par carnet: X FCFA" / "Reste a payer: X FCFA"

**Fichiers impactes :**
- `ui/activity/UnifiedSaleActivity.kt` - zone totaux
- `ui/dialog/PaymentDialogFragment.kt` - entete du dialog
- `res/layout/` - layouts concernes

---

#### 2.2 Remise / Discount pour ventes Comptant et Carnet

**Web :** Support complet des remises pour Comptant et Carnet via `PUT /api/sales/comptant/add-remise`, `PUT /api/sales/assurance/add-remise` et endpoints remove-remise correspondants. **Non disponible pour Assurance.**

**Mobile :** L'API existe dans `SalesApiService` mais **n'est pas branchee dans l'UI ni le ViewModel pour les ventes Carnet**.

**Actions :**
- [ ] Ajouter un bouton/menu "Appliquer remise" visible pour Comptant et Carnet uniquement (masque pour Assurance)
- [ ] Reutiliser le `DiscountDialog` existant (ou en creer un)
- [ ] Router vers l'endpoint correct selon le type : comptant/add-remise (Comptant) ou assurance/add-remise (Carnet)
- [ ] Mettre a jour le total apres application/suppression

**Fichiers impactes :**
- `ui/viewmodel/UnifiedSaleViewModel.kt` - methodes remise par type
- `ui/activity/UnifiedSaleActivity.kt` - visibilite bouton selon saleType + branchement
- `ui/dialog/DiscountDialog.kt` - reutilisation ou adaptation

---

#### 2.3 Validation montant verse (tous types de vente)

**Regle :** Si `montantVerse < amountToBePaid` (avec tolerance de 5 FCFA), bloquer la finalisation et afficher un message d'erreur. Pas de mode differe sur mobile.

**Applicable a :** Comptant, Assurance, Carnet, Depot (les 4 types).

**Actions :**
- [ ] Detecter dans `finalizeSale()` si `montantVerse < amountToBePaid - 5` (tolerance 5 FCFA)
- [ ] Si insuffisant : afficher Snackbar/Toast "Montant verse insuffisant" et bloquer la finalisation
- [ ] L'utilisateur doit corriger le montant ou annuler

**Fichiers impactes :**
- `ui/viewmodel/UnifiedSaleViewModel.kt` - validation avant finalisation
- `ui/activity/UnifiedSaleActivity.kt` - affichage message

---

### Priorite 3 - Enhancement (complete la parite)

#### 3.1 Type de prescription configurable

**Web :** Selecteur de type de prescription (`PRESCRIPTION` / `NON_PRESCRIPTION`) et flag `sansBon`.

**Mobile :** Hardcode a `"PRESCRIPTION"` et `sansBon = false`.

**Actions :**
- [ ] Ajouter un selecteur (Spinner ou RadioGroup) pour le type de prescription dans la zone assurance
- [ ] Ajouter un Switch/Checkbox pour "Sans bon"
- [ ] Mettre a jour le modele Sale avec les valeurs selectionnees

---

#### 3.2 Creation de client carnet dans le flow

**Web :** Formulaire complet `CustomerCarnetComponent` pour creer un client carnet (nom, tiers payant, taux, matricule).

**Mobile :** `CustomerRepository.createCarnetCustomer()` existe mais **aucun dialog de creation**.

**Actions :**
- [ ] Creer `CreateCarnetCustomerDialog` avec formulaire
- [ ] Champs : nom, prenom, telephone, tiers payant (recherche), taux, matricule
- [ ] Appeler `CustomerRepository.createCarnetCustomer()`
- [ ] Apres creation, selectionner automatiquement le nouveau client

---

#### 3.3 Creation de client assure dans le flow

**Web :** Formulaire pour creer un client assure avec ses tiers payants.

**Mobile :** API existe (`CustomerApiService.createAssuredCustomer`) mais **pas de dialog de creation dans le flow de vente**.

**Actions :**
- [ ] Creer `CreateAssuredCustomerDialog` avec formulaire
- [ ] Champs : nom, prenom, telephone, tiers payant principal (recherche), taux, matricule
- [ ] Option d'ajouter des tiers payants complementaires
- [ ] Appeler `CustomerApiService.createAssuredCustomer()`

---

#### 3.4 Gestion du paiement sans montant (couverture 100%)

**Web :** Si `amountToBePaid <= 0` (assurance couvre 100%), la vente se finalise sans dialog de paiement. Appelle `finalizeSaleWithoutPayment()`.

**Mobile :** Le `PaymentDialogFragment` s'ouvre toujours avec le montant. **Pas de shortcut si montant = 0.**

**Actions :**
- [ ] Dans `finalizeSale()`, si `amountToBePaid <= 0` pour une vente VO, finaliser directement sans payment dialog
- [ ] Afficher un message de confirmation : "Vente entierement couverte par l'assurance. Finaliser ?"
- [ ] Appeler `viewModel.finalizeSale(payments = emptyList(), montantVerse = 0, montantRendu = 0)`

---

## Tableau recapitulatif

| # | Action | Priorite | Type | Effort estime |
|---|--------|----------|------|---------------|
| 1.1 | Gestion ayants droit | P1 | Assurance | 2-3 jours |
| 1.2 | Ajout tiers payant complementaire | P1 | Assurance | 1-2 jours |
| 2.1 | Detail couverture dans UI | P2 | Les deux | 1 jour |
| 2.2 | Remise pour ventes Comptant/Carnet | P2 | Comptant + Carnet | 1 jour |
| 2.3 | Validation montant verse (bloquer si insuffisant) | P2 | Tous types | 0.5 jour |
| 3.1 | Type prescription configurable | P3 | Assurance | 0.5 jour |
| 3.2 | Creation client carnet | P3 | Carnet | 1-2 jours |
| 3.3 | Creation client assure | P3 | Assurance | 1-2 jours |
| 3.4 | Paiement sans montant (100% couvert) | P3 | Assurance | 0.5 jour |

**Total estime : 7-13 jours de developpement**

---

## Ordre d'implementation recommande

1. **1.1** Ayants droit → debloquer les ventes assurance avec beneficiaires
2. **1.2** Ajout tiers payant → permettre les assurances complementaires
3. **3.4** Paiement sans montant → corriger le flow assurance 100% couverte
4. **2.1** Detail couverture → ameliorer la lisibilite
5. **2.3** Validation montant verse → bloquer si insuffisant (tous types)
6. **2.2** Remise VO → fonctionnalite manquante
7. **3.1 - 3.3** Enhancements restants
