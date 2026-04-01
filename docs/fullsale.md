# Plan d'Implémentation - Module Mobile FullSale Android
## En tant que Architecte Expert & Développeur Mobile Kotlin

## Vue d'ensemble

Ce plan détaille l'implémentation complète du module mobile **FullSale** pour l'application Android Pharma-Smart, basé sur l'analyse approfondie du module web de ventes et du module sales-android existant.

**Objectif**: Créer un module Android natif offrant toutes les fonctionnalités de vente (comptant, assurance, carnet) avec gestion des préventes, optimisé pour mobile et suivant l'architecture Clean + MVVM.

---

## Modules de Référence Analysés

1. **Module Web** (`src/main/webapp/app/entities/sales/`)
   - sales-home, comptant-home, selling-home
   - 54+ fichiers TypeScript avec toutes les fonctionnalités
   - Patterns de gestion d'état avec Angular Signals
   - Endpoints API complets

2. **Module sales-android** (`C:\Users\k.kobena\Documents\full_warehouse\sales-android`)
   - Architecture MVVM complète et fonctionnelle
   - Vente comptant simple déjà implémentée
   - Composants réutilisables: BaseActivity, TokenManager, ApiClient
   - Intégration imprimante thermique Sunmi

3. **Base commune pharma-mobile-report** (`C:\Users\k.kobena\Documents\full_warehouse\pharma-mobile-report`)
   - Utilities partagées: NumberFormatUtils, NetworkManager, OfflineManager
   - Authentification JWT avec EncryptedSharedPreferences
   - Support offline avec Room et WorkManager
   - Thème et ressources UI communes

4. **Backend Services** (`src/main/java/com/kobe/warehouse/service/sale/`)
   - SimplifiedSaleService: ventes simples (utilisé par sales-android)
   - SaleService: ventes complètes (comptant, assurance, carnet)
   - ThirdPartySaleService: gestion tiers payant
   - Endpoints REST complets

---

## Définition du MVP (Minimum Viable Product)

Le MVP correspond à la **Phase 1** et permet de:
- Lister les ventes en cours et préventes
- Créer une nouvelle vente comptant (simple)
- Scanner des produits et gérer le panier
- Finaliser la vente avec paiement
- Imprimer le reçu thermique
- Mettre une vente en attente (prévente)

**Critères de succès MVP**:
- ✅ Parcours complet de vente comptant fonctionnel
- ✅ Gestion des préventes (création, reprise)
- ✅ Interface utilisateur fluide et réactive
- ✅ Intégration imprimante thermique
- ✅ Gestion hors ligne basique (cache des ventes en cours)
- ✅ Tests unitaires sur ViewModels clés

---

## Architecture Technique

### Stack Technologique
- **Langage**: Kotlin 2.0.21
- **Architecture**: Clean Architecture + MVVM
- **UI**: XML Layouts avec ViewBinding (préparation Jetpack Compose)
- **DI**: Manual Factory Pattern (recommandation: migrer vers Hilt en Phase 3)
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0 + Gson 2.10.1
- **Async**: Kotlin Coroutines 1.7.3 + Flow
- **State Management**: LiveData (Phase 1-2), StateFlow (Phase 3+)
- **Local Storage**: EncryptedSharedPreferences + Room 2.6.1
- **Printer**: Sunmi Printer Library 1.0.18

### Layers Clean Architecture

```
📱 Presentation Layer (ui/)
   ├── activity/        # Activities (Views)
   ├── fragment/        # Fragments pour tabs
   ├── adapter/         # RecyclerView adapters
   ├── dialog/          # DialogFragments
   └── viewmodel/       # ViewModels (business logic UI)

🔄 Domain Layer (domain/)
   ├── model/           # Business entities (Sale, SaleLine, Customer, etc.)
   ├── usecase/         # Use cases métier
   └── repository/      # Repository interfaces

💾 Data Layer (data/)
   ├── api/             # Retrofit API services
   ├── model/           # DTOs pour API
   ├── repository/      # Repository implementations
   ├── local/           # Room database, DAOs
   └── mapper/          # Mappers DTO ↔ Domain

🛠️ Infrastructure (utils/, printer/, service/)
   ├── TokenManager     # JWT token management
   ├── NetworkManager   # Connectivity monitoring
   └── UnifiedPrinterService # Thermal printing
```

---

## PHASE 1 - MVP: Vente Comptant Simple + Préventes (Priorité CRITIQUE)

**Durée estimée**: Sprint 1 (2 semaines)

### Objectifs
Implémenter le parcours complet de vente comptant avec gestion des préventes, réutilisant au maximum les composants de sales-android.

### 1.1 Écran Principal - FullSaleHomeActivity

**Description**: Écran d'accueil avec deux modes selon configuration

**Fonctionnalités**:
- Mode Simple (`useSimpleSale = true`):
  - Liste des ventes en cours uniquement
  - Bouton "Nouvelle vente" → ComptantSaleActivity
  - Barre de recherche avec filtre local

- Mode Complet (`useSimpleSale = false`):
  - TabLayout avec 2 onglets: "Ventes en cours" et "Préventes"
  - Chaque onglet = Fragment dédié
  - Bouton "Nouvelle vente" contextuel selon l'onglet actif

**UI Components**:
- `FullSaleHomeActivity.kt` (nouveau)
- `activity_fullsale_home.xml` avec TabLayout
- `VenteEnCoursFragment.kt` (nouveau)
- `PreventeFragment.kt` (nouveau)

**ViewModel**:
- `FullSaleHomeViewModel.kt` (nouveau)
  - `ongoingSales: LiveData<List<Sale>>`
  - `preventes: LiveData<List<Sale>>`
  - `searchQuery: MutableLiveData<String>`
  - `loadOngoingSales(search: String?)`
  - `loadPreventes(search: String?)`
  - `deleteSale(saleId: SaleId)`

**Repository**:
- Réutiliser `SalesRepository` existant de sales-android
- Ajouter méthode: `getPreventes(search: String?): Result<List<Sale>>`

**API Endpoints** (à ajouter si manquants):
- `GET /api/sales/prevente?search={query}` (existe déjà dans web)
- `GET /api/sales/simplified?search={query}` (existe déjà)

**Adapter**:
- Réutiliser `SalesAdapter` de sales-android
- Modifier pour afficher:
  - `numberTransaction`
  - `updatedAt` formaté (ex: "Il y a 2h")
  - `customer.firstName + lastName` si présent
  - Statut visuel (PENDING badge orange)

**Navigation**:
- Clic sur vente → `ComptantSaleActivity` (réutiliser existant)
  - Passer `saleId` et `saleDate` en intent extras
  - Charger la vente existante dans le ViewModel

**Fichiers à créer/modifier**:
- ✨ `ui/activity/FullSaleHomeActivity.kt`
- ✨ `ui/fragment/VenteEnCoursFragment.kt`
- ✨ `ui/fragment/PreventeFragment.kt`
- ✨ `ui/viewmodel/FullSaleHomeViewModel.kt`
- ✨ `ui/viewmodel/FullSaleHomeViewModelFactory.kt`
- ✨ `res/layout/activity_fullsale_home.xml`
- ✨ `res/layout/fragment_vente_en_cours.xml`
- ✨ `res/layout/fragment_prevente.xml`
- 📝 Modifier `data/repository/SalesRepository.kt` (ajouter getPreventes)
- 📝 Modifier `data/api/SalesApiService.kt` (ajouter endpoint prevente)

### 1.2 Gestion des Préventes (Put on Hold)

**Description**: Permettre de mettre une vente en attente et de la reprendre

**Fonctionnalités dans ComptantSaleActivity**:
- Bouton "Mettre en attente" dans le menu
- Confirmation dialog avant mise en attente
- Sauvegarde de l'état complet de la vente (panier, client, etc.)
- Retour à FullSaleHomeActivity après sauvegarde

**ViewModel Update** (ComptantSaleViewModel):
- `putOnHold(): LiveData<Result<ResponseDTO>>`
  - Appel API: `PUT /api/sales/comptant/put-on-hold`
  - Broadcast event: "Sale put on hold"

**API Endpoint** (existe déjà):
- `PUT /api/sales/comptant/put-on-hold`
  - Body: `CashSaleDTO` avec tous les items du panier

**Fichiers à modifier**:
- 📝 `ui/activity/ComptantSaleActivity.kt`
  - Ajouter menu item "Mettre en attente"
  - Ajouter dialog de confirmation
  - Observer `putOnHoldResult` du ViewModel
- 📝 `ui/viewmodel/ComptantSaleViewModel.kt`
  - Ajouter `putOnHold()` method
- 📝 `data/repository/SalesRepository.kt`
  - Ajouter `putCashSaleOnHold(sale: Sale): Result<ResponseDTO>`
- 📝 `data/api/SalesApiService.kt`
  - Ajouter endpoint si manquant

### 1.3 Reprise d'une Vente/Prévente

**Description**: Charger une vente existante dans ComptantSaleActivity

**Fonctionnalités**:
- Détecter intent extra `saleId` et `saleDate`
- Si présents → charger la vente depuis API
- Remplir le panier avec les `salesLines` existantes
- Restaurer le client si présent
- Mode édition: désactiver certaines actions (ex: change sale type)

**ViewModel Update** (ComptantSaleViewModel):
- `loadExistingSale(saleId: Long, saleDate: String)`
  - Appel API: `GET /api/sales/{id}/{date}`
  - Mapper response → état local (cart, customer, totals)
  - Set `isEditMode = true`

**API Endpoint** (existe déjà):
- `GET /api/sales/{id}/{saleDate}`
  - Response: `Sale` complet avec `salesLines`, `customer`, `payments`

**Fichiers à modifier**:
- 📝 `ui/activity/ComptantSaleActivity.kt`
  - Extraire intent extras au `onCreate`
  - Appeler `viewModel.loadExistingSale()` si saleId présent
- 📝 `ui/viewmodel/ComptantSaleViewModel.kt`
  - Ajouter `loadExistingSale()`
  - Ajouter `isEditMode: LiveData<Boolean>`
- 📝 `data/repository/SalesRepository.kt`
  - Ajouter `getSaleById(id: Long, date: String): Result<Sale>`
- 📝 `data/api/SalesApiService.kt`
  - Ajouter endpoint si manquant

### 1.4 Tests Unitaires MVP

**ViewModels à tester**:
- `FullSaleHomeViewModelTest.kt`
  - Test chargement ventes en cours
  - Test chargement préventes
  - Test recherche/filtre
  - Test suppression vente

- `ComptantSaleViewModelTest.kt`
  - Test création nouvelle vente
  - Test ajout produit au panier
  - Test mise en attente (putOnHold)
  - Test chargement vente existante
  - Test finalisation vente

**Framework**: JUnit 4 + Mockito + Coroutines Test

**Fichiers à créer**:
- ✨ `test/java/com/kobe/warehouse/sales/viewmodel/FullSaleHomeViewModelTest.kt`
- ✨ `test/java/com/kobe/warehouse/sales/viewmodel/ComptantSaleViewModelTest.kt`

### 1.5 UI/UX Optimisations Mobile

**Améliorations par rapport au web**:
- **Pull-to-refresh** sur listes de ventes
- **Swipe-to-delete** sur items de vente (avec confirmation)
- **Empty states** avec icônes et messages encourageants
- **Loading states** avec shimmer effect (utiliser library Shimmer)
- **Error handling** avec Snackbar + bouton retry
- **Timestamps relatifs** ("Il y a 5 min", "Aujourd'hui 14:30")

**Composants UI à créer**:
- `EmptyStateView.kt` - Composant réutilisable pour états vides
- `LoadingStateView.kt` - Shimmer loading
- `ErrorStateView.kt` - État d'erreur avec retry

**Fichiers à créer**:
- ✨ `ui/components/EmptyStateView.kt`
- ✨ `ui/components/LoadingStateView.kt`
- ✨ `ui/components/ErrorStateView.kt`
- ✨ `res/layout/view_empty_state.xml`
- ✨ `res/layout/view_loading_state.xml`
- ✨ `res/layout/view_error_state.xml`

---

## PHASE 2 - Ventes Avancées: Assurance & Carnet (Priorité HAUTE)

**Durée estimée**: Sprint 2-3 (3 semaines)

### Objectifs
Implémenter les types de vente avancés (VO - Vente Obligataire) avec gestion assurance et carnet, en s'appuyant sur les services backend existants.

### 2.1 Architecture pour Multi-Types de Vente

**Pattern**: Sealed Class pour typage sûr

```kotlin
sealed class SaleType {
    object Comptant : SaleType()
    data class Assurance(val tiersPayants: List<TiersPayant>) : SaleType()
    data class Carnet(val carnetId: Long) : SaleType()
}
```

**ViewModel Unifié**:
- `UnifiedSaleViewModel.kt` (nouveau)
  - Gère les 3 types de vente avec logique commune
  - Délègue calculs spécifiques selon le type
  - State: `currentSaleType: LiveData<SaleType>`

**Avantages**:
- Code partagé pour gestion panier, produits, paiement
- Logique spécifique isolée par type
- Type safety avec Kotlin sealed classes

### 2.2 Écran de Vente Unifié - UnifiedSaleActivity

**Description**: Remplace ComptantSaleActivity pour gérer tous types de vente

**UI Components**:
- Top Bar avec sélecteur de type de vente (Chips horizontal)
- Zone client (obligatoire pour Assurance/Carnet, optionnel pour Comptant)
- Zone assurance (visible seulement si type = Assurance)
- Zone panier (commune à tous types)
- Zone paiement (adaptée selon le type)

**Layout Structure**:
```xml
<LinearLayout orientation="vertical">
  <!-- Sale Type Selector (HorizontalScrollView + ChipGroup) -->
  <include layout="@layout/include_sale_type_selector" />

  <!-- Customer Zone (expandable) -->
  <include layout="@layout/include_customer_zone" />

  <!-- Insurance Data Zone (visible if Assurance) -->
  <include layout="@layout/include_insurance_data" />

  <!-- Product Search & Cart -->
  <include layout="@layout/include_product_cart" />

  <!-- Payment Zone -->
  <include layout="@layout/include_payment_zone" />
</LinearLayout>
```

**Fichiers à créer**:
- ✨ `ui/activity/UnifiedSaleActivity.kt`
- ✨ `ui/viewmodel/UnifiedSaleViewModel.kt`
- ✨ `domain/model/SaleType.kt` (sealed class)
- ✨ `res/layout/activity_unified_sale.xml`
- ✨ `res/layout/include_sale_type_selector.xml`
- ✨ `res/layout/include_customer_zone.xml`
- ✨ `res/layout/include_insurance_data.xml`

### 2.3 Gestion Client Obligatoire (Assurance/Carnet)

**Fonctionnalités**:
- Client obligatoire pour VO (validation avant ajout produit)
- Recherche client avec autocomplete
- Affichage info client: plafond conso, encours, tiers payants
- Sélection ayant-droit (bénéficiaire) pour assurance
- Avertissement si plafond atteint

**UI Components**:
- `CustomerSearchView.kt` - Autocomplete avec debounce
- `CustomerInfoCard.kt` - Card affichant les infos client
- `AyantDroitDialog.kt` - Dialog de sélection bénéficiaire

**ViewModel**:
- `CustomerSelectionViewModel.kt` (nouveau)
  - `searchCustomers(query: String): LiveData<List<Customer>>`
  - `selectCustomer(customer: Customer)`
  - `selectedCustomer: LiveData<Customer?>`
  - `ayantDroits: LiveData<List<Customer>>`
  - `plafondReached: LiveData<Boolean>`

**API Endpoints** (existent déjà):
- `GET /api/customers/search?q={query}`
- `GET /api/customers/{id}`
- `GET /api/customers/{id}/ayant-droits`

**Fichiers à créer**:
- ✨ `ui/components/CustomerSearchView.kt`
- ✨ `ui/components/CustomerInfoCard.kt`
- ✨ `ui/dialog/AyantDroitDialog.kt`
- ✨ `ui/viewmodel/CustomerSelectionViewModel.kt`
- ✨ `data/repository/CustomerRepository.kt` (étendre existant)
- ✨ `res/layout/view_customer_search.xml`
- ✨ `res/layout/card_customer_info.xml`
- ✨ `res/layout/dialog_ayant_droit.xml`

### 2.4 Gestion Assurance (Tiers Payant)

**Fonctionnalités**:
- Saisie données assurance:
  - Tiers payant principal (sélection depuis liste client)
  - Tiers payants complémentaires (optionnel)
  - Type de prescription (Ordonnance, Bon de prise en charge, etc.)
  - Numéro de bon (obligatoire selon config)
  - Taux de couverture (% part assurance)
- Calcul automatique part client / part assurance
- Validation règles métier (ex: prescription obligatoire pour certains produits)

**UI Components**:
- `InsuranceDataForm.kt` - Formulaire données assurance
- `TiersPayantSelector.kt` - Sélection tiers payant avec autocomplete
- `PrescriptionTypeSelector.kt` - Dropdown type prescription

**ViewModel**:
- `InsuranceDataViewModel.kt` (nouveau)
  - `tiersPayants: LiveData<List<TiersPayant>>`
  - `prescriptionType: MutableLiveData<PrescriptionType>`
  - `numeroBon: MutableLiveData<String>`
  - `tauxCouverture: MutableLiveData<Int>`
  - `validateInsuranceData(): Boolean`

**Business Logic**:
- `TiersPayantCalculationService` (backend existe déjà)
  - Utiliser endpoint pour calcul part client/assurance
- `AssuranceValidator.kt` (nouveau côté mobile)
  - Validation formulaire assurance
  - Règles métier spécifiques

**API Endpoints** (existent déjà):
- `GET /api/tiers-payants/search?q={query}`
- `POST /api/sales/assurance` - Créer vente assurance
- `POST /api/sales/add-item/assurance` - Ajouter item
- `PUT /api/sales/assurance/save` - Finaliser vente assurance

**Fichiers à créer**:
- ✨ `ui/components/InsuranceDataForm.kt`
- ✨ `ui/components/TiersPayantSelector.kt`
- ✨ `ui/components/PrescriptionTypeSelector.kt`
- ✨ `ui/viewmodel/InsuranceDataViewModel.kt`
- ✨ `domain/model/TiersPayant.kt`
- ✨ `domain/model/PrescriptionType.kt` (enum)
- ✨ `domain/validator/AssuranceValidator.kt`
- ✨ `data/repository/TiersPayantRepository.kt`
- ✨ `data/api/TiersPayantApiService.kt`
- ✨ `res/layout/form_insurance_data.xml`

### 2.5 Gestion Vente Carnet

**Fonctionnalités**:
- Sélection client carnet (type CARNET)
- Vérification encours client (limite crédit)
- Affichage historique achats carnet client
- Validation limite crédit avant finalisation
- Mode paiement: crédit uniquement (pas de cash/carte)

**UI Components**:
- `CarnetClientInfo.kt` - Affichage info carnet client
- `CarnetHistoryDialog.kt` - Historique achats carnet

**ViewModel**:
- `CarnetSaleViewModel.kt` (nouveau)
  - `carnetClient: LiveData<Customer>`
  - `encours: LiveData<Int>`
  - `canFinalizeSale: LiveData<Boolean>` (encours + sale <= limite)

**API Endpoints**:


**Fichiers à créer**:
- ✨ `ui/components/CarnetClientInfo.kt`
- ✨ `ui/dialog/CarnetHistoryDialog.kt`
- ✨ `ui/viewmodel/CarnetSaleViewModel.kt`
- ✨ `res/layout/card_carnet_client_info.xml`
- ✨ `res/layout/dialog_carnet_history.xml`

### 2.6 Transformation de Vente (Comptant → Assurance/Carnet)

**Fonctionnalités**:
- Bouton "Transformer en..." dans menu UnifiedSaleActivity
- Dialog de sélection du nouveau type
- Confirmation avec aperçu des changements (recalcul prix)
- Appel API transformation
- Rechargement vente transformée

**UI Components**:
- `SaleTransformationDialog.kt` - Dialog de transformation

**ViewModel Update** (UnifiedSaleViewModel):
- `transformSale(newType: SaleType): LiveData<Result<Sale>>`
  - Appel API avec saleId et nouveau type
  - Rechargement vente après transformation

**API Endpoint** (existe déjà):
- `GET /api/sales/assurance/transform?natureVente={ASSURANCE|CARNET}&saleId={id}`

**Fichiers à créer/modifier**:
- ✨ `ui/dialog/SaleTransformationDialog.kt`
- 📝 `ui/viewmodel/UnifiedSaleViewModel.kt` (ajouter transformSale)
- 📝 `data/repository/SalesRepository.kt` (ajouter transformSale)
- ✨ `res/layout/dialog_sale_transformation.xml`

### 2.7 Tests Unitaires Phase 2

**ViewModels à tester**:
- `UnifiedSaleViewModelTest.kt`
  - Test changement type vente
  - Test validation client obligatoire
  - Test transformation vente

- `InsuranceDataViewModelTest.kt`
  - Test validation données assurance
  - Test calcul part client/assurance

- `CarnetSaleViewModelTest.kt`
  - Test vérification limite crédit
  - Test validation encours

**Fichiers à créer**:
- ✨ `test/java/com/kobe/warehouse/sales/viewmodel/UnifiedSaleViewModelTest.kt`
- ✨ `test/java/com/kobe/warehouse/sales/viewmodel/InsuranceDataViewModelTest.kt`
- ✨ `test/java/com/kobe/warehouse/sales/viewmodel/CarnetSaleViewModelTest.kt`

---

## PHASE 3 - Support Offline & Optimisations (Priorité MOYENNE)

**Durée estimée**: Sprint 4 (2 semaines)

### Objectifs
Améliorer la robustesse de l'application avec support offline complet, synchronisation intelligente et optimisations de performance.
## NB:  on ne fait pas de vente offline, donc supprimer la partie vente offline
### 3.1 Offline-First Architecture

**Stratégie**:
- Cache local Room pour ventes en cours/préventes
- Queue d'actions offline (add item, finalize, put on hold)
- Synchronisation automatique au retour online
- Indicateur UI état online

**Room Database Enhancement**:
- Entités:
  - `SaleEntity` - Ventes en cache
  - `SaleLineEntity` - Lignes de vente
  - `PendingSaleActionEntity` - Actions en attente sync
  - `ProductCacheEntity` - Cache produits récents
  - `CustomerCacheEntity` - Cache clients récents

**DAOs**:
- `SaleDao` - CRUD ventes locales
- `PendingSaleActionDao` - Queue actions offline
- `ProductCacheDao` - Cache produits
- `CustomerCacheDao` - Cache clients

**Fichiers à créer**:
- ✨ `data/local/AppDatabase.kt` (étendre existant)
- ✨ `data/local/dao/SaleDao.kt`
- ✨ `data/local/dao/PendingSaleActionDao.kt`
- ✨ `data/local/dao/ProductCacheDao.kt`
- ✨ `data/local/dao/CustomerCacheDao.kt`
- ✨ `data/local/entity/SaleEntity.kt`
- ✨ `data/local/entity/SaleLineEntity.kt`
- ✨ `data/local/entity/PendingSaleActionEntity.kt`
- ✨ `data/local/entity/ProductCacheEntity.kt`
- ✨ `data/local/entity/CustomerCacheEntity.kt`

### 3.2 Synchronization Manager

**Fonctionnalités**:
- Détection retour online
- Synchronisation auto des actions pending
- Gestion conflits (version serveur vs locale)
- Retry mechanism avec backoff exponentiel
- Notification utilisateur si sync échoue

**Components**:
- `SyncManager.kt` - Orchestration sync
- `ConflictResolver.kt` - Résolution conflits
- `SyncWorker.kt` - WorkManager worker pour sync background

**Fichiers à créer**:
- ✨ `data/sync/SyncManager.kt`
- ✨ `data/sync/ConflictResolver.kt`
- ✨ `data/sync/SyncWorker.kt`
- ✨ `data/sync/SyncStrategy.kt`

### 3.3 Migration LiveData → StateFlow

**Objectif**: Moderniser state management avec StateFlow + Compose-ready

**Avantages**:
- Meilleure performance (backpressure handling)
- Composition de flows (combine, zip, etc.)
- Préparation migration Jetpack Compose
- API plus moderne et concise

**ViewModels à migrer**:
- `FullSaleHomeViewModel`
- `UnifiedSaleViewModel`
- `CustomerSelectionViewModel`

**Pattern de migration**:
```kotlin
// Avant (LiveData)
private val _sales = MutableLiveData<List<Sale>>()
val sales: LiveData<List<Sale>> = _sales

// Après (StateFlow)
private val _sales = MutableStateFlow<List<Sale>>(emptyList())
val sales: StateFlow<List<Sale>> = _sales.asStateFlow()
```

**Fichiers à modifier**:
- 📝 Tous les ViewModels créés en Phase 1 et 2

### 3.4 Performance Optimizations

**Optimisations**:
1. **Pagination** pour listes de ventes (Paging 3)
2. **Image caching** pour photos produits (Coil)
3. **ProGuard rules** pour réduction taille APK
4. **R8 optimizations** pour shrinking code
5. **Lazy loading** pour onglets fragments
6. **RecyclerView ViewHolder recycling** optimisé

**Pagination Implementation**:
```kotlin
val sales: Flow<PagingData<Sale>> = Pager(
    config = PagingConfig(pageSize = 20, prefetchDistance = 5),
    pagingSourceFactory = { SalesPagingSource(api, search) }
).flow.cachedIn(viewModelScope)
```

**Fichiers à créer/modifier**:
- ✨ `data/paging/SalesPagingSource.kt`
- 📝 `ui/viewmodel/FullSaleHomeViewModel.kt` (ajouter Paging)
- 📝 `proguard-rules.pro` (ajouter règles)

### 3.5 Dependency Injection avec Hilt

**Objectif**: Éliminer le boilerplate des factories manuelles

**Setup**:
```gradle
// build.gradle
plugins {
    id 'com.google.dagger.hilt.android' version '2.48'
}

dependencies {
    implementation "com.google.dagger:hilt-android:2.48"
    kapt "com.google.dagger:hilt-compiler:2.48"
}
```

**Modules Hilt**:
- `NetworkModule` - Retrofit, OkHttp, ApiServices
- `RepositoryModule` - All repositories
- `DatabaseModule` - Room database, DAOs
- `UtilsModule` - TokenManager, NetworkManager, etc.

**Fichiers à créer**:
- ✨ `di/NetworkModule.kt`
- ✨ `di/RepositoryModule.kt`
- ✨ `di/DatabaseModule.kt`
- ✨ `di/UtilsModule.kt`
- 📝 `PharmaSmartApplication.kt` (ajouter @HiltAndroidApp)
- 📝 Toutes les Activities (ajouter @AndroidEntryPoint)

### 3.6 Tests Phase 3

**Tests à ajouter**:
- Repository tests avec Room in-memory database
- SyncManager tests avec fake API
- Paging tests avec Paging 3 test utils
- Integration tests pour sync flow complet

**Fichiers à créer**:
- ✨ `test/java/com/kobe/warehouse/sales/repository/SalesRepositoryTest.kt`
- ✨ `test/java/com/kobe/warehouse/sales/sync/SyncManagerTest.kt`
- ✨ `test/java/com/kobe/warehouse/sales/paging/SalesPagingSourceTest.kt`

---

## PHASE 4 - Features Avancées & UX (Priorité BASSE)

**Durée estimée**: Sprint 5-6 (3 semaines)

### 4.1 Gestion Remises (Discounts)

**Fonctionnalités**:
- Application remise sur vente complète (% ou montant fixe)
- Application remise sur ligne de vente spécifique
- Validation autorisation utilisateur (authority `PR_REMISE`)
- Historique remises appliquées
- Suppression remise

**UI Components**:
- `DiscountDialog.kt` - Dialog application remise
- `DiscountAuthDialog.kt` - Dialog autorisation remise

**API Endpoints** (existent déjà):
- `PUT /api/sales/comptant/add-remise`
- `DELETE /api/sales/comptant/remove-remise`

**Fichiers à créer**:
- ✨ `ui/dialog/DiscountDialog.kt`
- ✨ `ui/dialog/DiscountAuthDialog.kt`
- ✨ `domain/model/Discount.kt`
- ✨ `res/layout/dialog_discount.xml`

### 4.2 Déconditionnement

**Fonctionnalités**:
- Détection produit déconditionnable
- Dialog confirmation déconditionnement
- Création automatique ligne pour produit parent (boîte)
- Ajout ligne pour produit enfant (unité)

**UI Components**:
- `DeconditionnementDialog.kt` - Dialog confirmation

**Business Logic**:
- `DeconditionnementService.kt` - Logique métier déconditionnement

**API Endpoint**:
- Intégré dans `POST /api/sales/add-item/comptant` avec flag `deconditionner=true`

**Fichiers à créer**:
- ✨ `ui/dialog/DeconditionnementDialog.kt`
- ✨ `domain/service/DeconditionnementService.kt`
- ✨ `res/layout/dialog_deconditionnement.xml`

### 4.3 Force Stock (Override Stock)

**Fonctionnalités**:
- Détection stock insuffisant
- Dialog validation autorité (PR_FORCE_STOCK)
- Confirmation utilisateur avec raison
- Ajout produit malgré stock insuffisant

**UI Components**:
- `ForceStockDialog.kt` - Dialog confirmation force stock

**Validation**:
- Vérifier authority utilisateur
- Logger action pour audit

**Fichiers à créer**:
- ✨ `ui/dialog/ForceStockDialog.kt`
- ✨ `domain/validator/StockValidator.kt` (réutiliser logique existante)
- ✨ `res/layout/dialog_force_stock.xml`

### 4.4 Raccourcis Clavier (pour Tablettes avec Clavier)

**Fonctionnalités**:
- Ctrl+N: Nouvelle vente
- Ctrl+S: Sauvegarder/Finaliser
- Ctrl+H: Mettre en attente
- Ctrl+F: Focus recherche produit
- Ctrl+C: Focus recherche client
- F2: Incrémenter quantité
- F3: Décrémenter quantité
- F4: Appliquer remise
- Esc: Annuler/Retour

**Implementation**:
```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (event?.isCtrlPressed == true) {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> { createNewSale(); return true }
            KeyEvent.KEYCODE_S -> { finalizeSale(); return true }
            // ...
        }
    }
    return super.onKeyDown(keyCode, event)
}
```

**Fichiers à modifier**:
- 📝 `ui/activity/UnifiedSaleActivity.kt` (override onKeyDown)
- 📝 `ui/activity/FullSaleHomeActivity.kt` (override onKeyDown)

### 4.5 Affichage Client (Customer Display - Tauri Integration)

**Fonctionnalités**:
- Envoi info vente à affichage client externe (écran secondaire)
- WebSocket ou HTTP API vers Tauri backend
- Affichage: produit ajouté, total, montant à payer

**Components**:
- `CustomerDisplayService.kt` - Service communication affichage

**API**:
- Custom endpoint Tauri ou WebSocket

**Fichiers à créer**:
- ✨ `service/CustomerDisplayService.kt`
- ✨ `data/api/CustomerDisplayApiService.kt`

### 4.6 Statistiques Vente en Temps Réel

**Fonctionnalités**:
- Dashboard vendeur: CA du jour, nombre ventes, panier moyen
- Graphiques: évolution CA journée, top produits vendus
- Comparaison avec objectifs

**UI Components**:
- `SalesDashboardActivity.kt` - Écran dashboard vendeur

**API Endpoints**:
- Réutiliser endpoints dashboard existants (`/api/mobile/dashboard`)

**Fichiers à créer**:
- ✨ `ui/activity/SalesDashboardActivity.kt`
- ✨ `ui/viewmodel/SalesDashboardViewModel.kt`
- ✨ `res/layout/activity_sales_dashboard.xml`

### 4.7 Tests Phase 4

**Tests à ajouter**:
- Tests UI avec Espresso pour flows complets
- Tests déconditionnement
- Tests force stock
- Tests remises

**Fichiers à créer**:
- ✨ `androidTest/java/com/kobe/warehouse/sales/ui/FullSaleFlowTest.kt`
- ✨ `androidTest/java/com/kobe/warehouse/sales/ui/DeconditionnementTest.kt`

---

## PHASE 5 - Jetpack Compose Migration (Priorité FUTURE)

**Durée estimée**: Sprint 7-9 (4-6 semaines)

### Objectifs
Migration progressive vers Jetpack Compose pour UI moderne et déclarative.

### 5.1 Setup Jetpack Compose

**Dependencies**:
```gradle
dependencies {
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'

    debugImplementation 'androidx.compose.ui:ui-tooling'
}
```

### 5.2 Migration Stratégie

**Approche**: Incrémentale, composant par composant

**Ordre de migration**:
1. Composants simples réutilisables (EmptyStateView, LoadingStateView)
2. Dialogs (DiscountDialog, ForceStockDialog)
3. Cards et items de liste (SaleItemCard, ProductItemCard)
4. Fragments (VenteEnCoursFragment, PreventeFragment)
5. Activities complètes (FullSaleHomeActivity → FullSaleHomeScreen)

**Pattern Composable**:
```kotlin
@Composable
fun SaleItemCard(
    sale: Sale,
    onClick: (Sale) -> Unit,
    onDelete: (Sale) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick(sale) }
    ) {
        // Compose UI...
    }
}
```

### 5.3 State Management avec Compose

**Pattern**:
- ViewModel expose StateFlow
- Composable collecte avec `collectAsState()`
- UI pure, pas de logique métier

**Example**:
```kotlin
@Composable
fun FullSaleHomeScreen(
    viewModel: FullSaleHomeViewModel = hiltViewModel()
) {
    val sales by viewModel.sales.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    FullSaleHomeContent(
        sales = sales,
        isLoading = isLoading,
        onSaleClick = viewModel::onSaleClick,
        onRefresh = viewModel::refresh
    )
}
```

### 5.4 Migration Checklist

**Phase 5A - Composants de base**:
- [ ] EmptyStateView → EmptyState composable
- [ ] LoadingStateView → LoadingState composable
- [ ] ErrorStateView → ErrorState composable
- [ ] SaleItemCard → Composable

**Phase 5B - Dialogs**:
- [ ] DiscountDialog → DiscountDialog composable
- [ ] ForceStockDialog → ForceStockDialog composable
- [ ] DeconditionnementDialog → Composable

**Phase 5C - Écrans**:
- [ ] VenteEnCoursFragment → VenteEnCoursScreen composable
- [ ] PreventeFragment → PreventeScreen composable
- [ ] FullSaleHomeActivity → FullSaleHomeScreen composable

**Phase 5D - Écran vente complet**:
- [ ] UnifiedSaleActivity → UnifiedSaleScreen composable (complexe)

---

## Récapitulatif des Priorités

### 🔴 PRIORITÉ CRITIQUE (MVP - Phase 1)
- ✅ Écran principal FullSaleHome avec liste ventes/préventes
- ✅ Création vente comptant (réutiliser sales-android)
- ✅ Gestion préventes (put on hold, reprise)
- ✅ Tests unitaires ViewModels
- **Durée**: 2 semaines
- **Livrables**: MVP fonctionnel pour ventes comptant

### 🟠 PRIORITÉ HAUTE (Phase 2)
- ✅ Ventes assurance (tiers payant)
- ✅ Ventes carnet (crédit)
- ✅ Gestion client obligatoire
- ✅ Transformation vente
- **Durée**: 3 semaines
- **Livrables**: Support complet tous types de vente

### 🟡 PRIORITÉ MOYENNE (Phase 3)
- ✅ Support offline complet
- ✅ Synchronisation intelligente
- ✅ Migration StateFlow
- ✅ Optimisations performance (Paging, Hilt)
- **Durée**: 2 semaines
- **Livrables**: Application robuste et performante

### 🟢 PRIORITÉ BASSE (Phase 4)
- ✅ Features avancées (remises, déconditionnement, force stock)
- ✅ Raccourcis clavier
- ✅ Dashboard vendeur
- ✅ Affichage client
- **Durée**: 3 semaines
- **Livrables**: Features "nice to have"

### 🔵 PRIORITÉ FUTURE (Phase 5)
- ✅ Migration Jetpack Compose
- **Durée**: 4-6 semaines
- **Livrables**: UI moderne et déclarative

---

## Fichiers Critiques à Créer/Modifier

### Phase 1 (MVP)
**Nouveaux fichiers (20+)**:
- `ui/activity/FullSaleHomeActivity.kt`
- `ui/fragment/VenteEnCoursFragment.kt`
- `ui/fragment/PreventeFragment.kt`
- `ui/viewmodel/FullSaleHomeViewModel.kt`
- `ui/components/EmptyStateView.kt`
- `ui/components/LoadingStateView.kt`
- `ui/components/ErrorStateView.kt`
- Layouts XML correspondants (7 fichiers)
- Tests unitaires (2 fichiers)

**Fichiers à modifier (5)**:
- `data/repository/SalesRepository.kt`
- `data/api/SalesApiService.kt`
- `ui/activity/ComptantSaleActivity.kt`
- `ui/viewmodel/ComptantSaleViewModel.kt`
- `AndroidManifest.xml`

### Phase 2 (Ventes Avancées)
**Nouveaux fichiers (30+)**:
- `ui/activity/UnifiedSaleActivity.kt`
- `ui/viewmodel/UnifiedSaleViewModel.kt`
- `ui/viewmodel/CustomerSelectionViewModel.kt`
- `ui/viewmodel/InsuranceDataViewModel.kt`
- `ui/viewmodel/CarnetSaleViewModel.kt`
- Composants UI (10+ fichiers)
- Domain models (5 fichiers)
- Repositories (3 fichiers)
- Tests (3 fichiers)

### Phase 3 (Offline & Optimizations)
**Nouveaux fichiers (20+)**:
- Room entities (5 fichiers)
- DAOs (4 fichiers)
- Sync components (4 fichiers)
- Hilt modules (4 fichiers)
- Paging sources (1 fichier)
- Tests (3 fichiers)

### Phase 4 (Features Avancées)
**Nouveaux fichiers (15+)**:
- Dialogs (5 fichiers)
- Services (2 fichiers)
- Dashboard (3 fichiers)
- Tests (2 fichiers)

**Total estimé: 100+ fichiers nouveaux/modifiés**

---

## Risques & Mitigation

### Risques Identifiés

1. **Complexité Tiers Payant** (Phase 2)
   - **Risque**: Calculs assurance complexes, multiples tiers payants
   - **Mitigation**: Réutiliser service backend `TiersPayantCalculationService`, tests unitaires exhaustifs

2. **Gestion Conflits Offline** (Phase 3)
   - **Risque**: Conflits sync vente modifiée offline et online
   - **Mitigation**: Stratégie "server wins", interface résolution manuelle si conflit

3. **Performance Listes Longues** (Phase 3)
   - **Risque**: Listes ventes/produits très longues (lag)
   - **Mitigation**: Paging 3, pagination backend, lazy loading

4. **Migration Compose** (Phase 5)
   - **Risque**: Bugs UI, incompatibilités
   - **Mitigation**: Migration incrémentale, tests UI Espresso conservés

5. **Compatibilité Imprimantes** (Toutes phases)
   - **Risque**: Sunmi SDK ne fonctionne pas sur tous devices
   - **Mitigation**: UnifiedPrinterService avec fallback (export PDF, share)

---

## Vérification & Tests End-to-End

### Scénario 1: Vente Comptant Simple (MVP)
1. ✅ Ouvrir FullSaleHomeActivity
2. ✅ Cliquer "Nouvelle vente"
3. ✅ Scanner un produit (code-barres)
4. ✅ Modifier quantité
5. ✅ Ajouter un autre produit
6. ✅ Vérifier total calculé correctement
7. ✅ Finaliser vente avec paiement cash
8. ✅ Imprimer reçu thermique
9. ✅ Vérifier vente disparue de liste "en cours"

### Scénario 2: Prévente (MVP)
1. ✅ Créer vente, ajouter produits
2. ✅ Menu → Mettre en attente
3. ✅ Confirmer dialog
4. ✅ Vérifier vente dans onglet "Préventes"
5. ✅ Cliquer sur prévente
6. ✅ Vérifier panier rechargé
7. ✅ Finaliser vente

### Scénario 3: Vente Assurance (Phase 2)
1. ✅ Nouvelle vente → Type "Assurance"
2. ✅ Rechercher et sélectionner client assuré
3. ✅ Sélectionner tiers payant
4. ✅ Remplir données assurance (prescription, numéro bon)
5. ✅ Ajouter produits
6. ✅ Vérifier calcul part client/assurance
7. ✅ Finaliser vente
8. ✅ Vérifier reçu avec détail assurance

### Scénario 4: Vente Offline puis Sync (Phase 3)
1. ✅ Activer mode avion
2. ✅ Créer vente, ajouter produits
3. ✅ Vérifier indicateur "offline" affiché
4. ✅ Finaliser vente (sauvegarde locale)
5. ✅ Désactiver mode avion
6. ✅ Vérifier sync automatique
7. ✅ Vérifier vente apparaît dans backend

### Scénario 5: Force Stock (Phase 4)
1. ✅ Ajouter produit avec stock insuffisant
2. ✅ Vérifier dialog "stock insuffisant"
3. ✅ Cliquer "Forcer stock"
4. ✅ Vérifier dialog autorisation
5. ✅ Valider autorité (PR_FORCE_STOCK)
6. ✅ Vérifier produit ajouté au panier

---

## Métriques de Succès

### MVP (Phase 1)
- ✅ Taux de réussite vente comptant: > 95%
- ✅ Temps moyen création vente: < 2 minutes
- ✅ Crash rate: < 1%
- ✅ Code coverage tests: > 70%

### Phase 2 (Ventes Avancées)
- ✅ Support 3 types de vente (comptant, assurance, carnet)
- ✅ Taux de réussite vente assurance: > 90%
- ✅ Code coverage tests: > 75%

### Phase 3 (Offline)
- ✅ Taux de succès sync offline: > 98%
- ✅ Temps de chargement listes: < 500ms (Paging)
- ✅ Taille APK: < 20 MB

### Phase 4 (Features Avancées)
- ✅ Adoption features avancées: > 50% utilisateurs
- ✅ Satisfaction utilisateur: > 4/5

---

## Conclusion

Ce plan d'implémentation fournit une feuille de route complète pour le développement du module mobile FullSale Android, structurée en 5 phases avec des priorités claires:

1. **Phase 1 (MVP)** - Vente comptant + préventes → Livrable en 2 semaines
2. **Phase 2** - Ventes avancées (assurance, carnet) → +3 semaines
3. **Phase 3** - Offline & optimisations → +2 semaines
4. **Phase 4** - Features avancées → +3 semaines
5. **Phase 5** - Migration Compose → +4-6 semaines (optionnel)

**Total estimation**: 10-12 semaines pour phases 1-4 (fonctionnalités complètes)

L'approche réutilise au maximum les composants existants de sales-android et pharma-mobile-report, suit l'architecture Clean + MVVM, et s'appuie sur les services backend déjà disponibles. Le plan prévoit une migration progressive vers les technologies modernes (StateFlow, Jetpack Compose) tout en garantissant la stabilité et la performance.
