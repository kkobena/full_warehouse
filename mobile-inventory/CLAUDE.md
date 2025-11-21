# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pharma Smart Inventory is a native Android mobile application for pharmacy inventory management built with Kotlin. It connects to the Pharma Smart Spring Boot backend (parent directory) to handle pharmaceutical inventory counting, barcode scanning, offline data storage, and server synchronization. The app implements JWT authentication matching the web application's security model.

## Technology Stack

**Android/Kotlin:**
- Kotlin 2.0.21
- Min SDK 30 (Android 11)
- Target SDK 36 (Android 14+)
- Android Gradle Plugin 8.13.1

**Architecture:**
- MVVM (Model-View-ViewModel) pattern
- Repository pattern for data layer
- Coroutines for asynchronous operations
- Flow/LiveData for reactive UI updates
- ViewBinding and DataBinding

**Key Libraries:**
- Retrofit 2.9.0 + OkHttp 4.12.0 - REST API client
- Gson 2.10.1 - JSON serialization
- Room 2.6.1 - Local database for offline caching
- AndroidX Lifecycle 2.7.0 - ViewModel, LiveData
- Material Components 1.11.0 - Material Design UI
- Security Crypto 1.1.0-alpha06 - Encrypted SharedPreferences for token storage
- ZXing 3.5.3 + journeyapps 4.3.0 - Barcode scanning
- WorkManager 2.9.0 - Background synchronization
- Navigation Component 2.7.6 - Navigation framework

**Java Version:**
- JDK 17 (configured in gradle.properties: `org.gradle.java.home=C:/Users/k.kobena/Documents/jdk17`)
- IMPORTANT: This project must use JDK 17, NOT Java 25 like the parent backend project

## Build & Development Commands

### Gradle Commands

**Note:** On Windows, use `gradlew.bat` or `.\gradlew.bat` in PowerShell.

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (with ProGuard)
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Project Architecture

### MVVM Pattern

The app strictly follows MVVM pattern:

**Model (data/):**
- Data models in `data/model/` (Kotlin data classes)
- API interfaces in `data/api/` (Retrofit service definitions)
- Repository classes in `data/repository/` (business logic, API calls)
- Database entities in `data/database/entity/` (Room entities)
- DAOs in `data/database/dao/` (Room data access objects)

**View (ui/):**
- Activities in `ui/activity/` (UI controllers)
- XML layouts in `res/layout/` (UI structure)
- ViewBinding for type-safe view access
- Observers for LiveData/Flow from ViewModels

**ViewModel (ui/viewmodel/):**
- Manages UI state with LiveData/Flow
- No Android framework dependencies (except AndroidX)
- Uses Coroutines for async operations via `viewModelScope`
- Repository dependency injection via ViewModelFactory

### Backend Integration

**API Base URL:**
- Build-time: `BuildConfig.BASE_URL`
- Runtime: Configurable via ServerConfigDialog
- Stored in TokenManager

**Key Backend Endpoints:**

**Authentication:**
- POST `/api/auth/login` - Login
- GET `/api/account` - Get user account

**Inventory:**
- GET `/api/store-inventories/actif` - List active inventories
- GET `/api/store-inventories/{id}` - Get inventory
- GET `/api/store-inventories/{id}/rayons` - Get sections
- GET `/api/store-inventories/{inventoryId}/rayons/{rayonId}/items` - Get items
- PUT `/api/store-inventories/lines` - Sync inventory lines (batch update)
- POST `/api/store-inventories/close/{id}` - Close inventory
- GET `/api/products/code/{barcode}` - Search product by barcode

## Implementation Status

### ✅ Implemented Components

1. **Project Configuration:**
   - build.gradle with all dependencies
   - AndroidManifest.xml with permissions
   - ProGuard rules
   - Gradle wrapper

2. **Authentication Module:**
   - TokenManager (EncryptedSharedPreferences)
   - SessionManager (session state management)
   - AuthApiService
   - AuthRepository
   - Auth models (JwtTokenResponse, LoginRequest, Account)

3. **Inventory Data Layer:**
   - Data models (StoreInventory, StoreInventoryLine, Rayon, Product)
   - Enums (InventoryCategory, InventoryStatut)
   - InventoryApiService (Retrofit interface)
   - InventoryRepository

4. **Offline Storage:**
   - InventoryDatabase (Room)
   - InventoryEntity, InventoryLineEntity
   - InventoryDao, InventoryLineDao
   - Sync status tracking

5. **Utilities:**
   - ApiClient (Retrofit factory with interceptors)
   - BarcodeScanner (ZXing wrapper)
   - SessionManager

### ⏳ To Be Implemented

1. **ViewModels:**
   - LoginViewModel
   - InventoryListViewModel
   - InventoryDetailViewModel

2. **Activities:**
   - SplashActivity
   - LoginActivity
   - MainActivity
   - InventoryListActivity
   - InventoryDetailActivity

3. **UI Layouts:**
   - All XML layouts
   - String resources
   - Color/theme resources
   - Drawable resources

4. **RecyclerView Adapters:**
   - InventoryAdapter
   - InventoryLineAdapter
   - RayonAdapter

5. **Synchronization:**
   - WorkManager worker for background sync
   - Sync service implementation
   - Conflict resolution

6. **Testing:**
   - Unit tests
   - Integration tests
   - UI tests

## Development Guidelines

### Adding a New Activity

1. Create Activity class in `ui/activity/`
2. Create corresponding ViewModel in `ui/viewmodel/`
3. Create XML layout in `res/layout/`
4. Add activity to AndroidManifest.xml
5. Implement ViewBinding
6. Observe LiveData/Flow from ViewModel

**Example Pattern:**
```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyBinding
    private val viewModel: MyViewModel by viewModels { MyViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.dataState.observe(this) { state ->
            when (state) {
                is DataState.Loading -> showLoading()
                is DataState.Success -> showData(state.data)
                is DataState.Error -> showError(state.message)
            }
        }
    }
}
```

### Adding a New ViewModel

1. Create ViewModel class in `ui/viewmodel/`
2. Inject Repository via constructor
3. Use `viewModelScope` for coroutines
4. Expose LiveData/Flow for UI state
5. Create ViewModelFactory for dependency injection

**Example Pattern:**
```kotlin
class MyViewModel(private val repository: MyRepository) : ViewModel() {

    private val _dataState = MutableLiveData<DataState<MyData>>()
    val dataState: LiveData<DataState<MyData>> = _dataState

    fun loadData() {
        viewModelScope.launch {
            _dataState.value = DataState.Loading
            repository.getData().fold(
                onSuccess = { data ->
                    _dataState.value = DataState.Success(data)
                },
                onFailure = { error ->
                    _dataState.value = DataState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
}

// ViewModelFactory
class MyViewModelFactory(
    private val repository: MyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### Offline-First Pattern

1. Always read from Room database first
2. Show cached data immediately
3. Fetch fresh data from API in background
4. Update Room database with fresh data
5. Observe database for reactive UI updates

**Example:**
```kotlin
// In Repository
suspend fun getInventories(): Flow<List<StoreInventory>> = flow {
    // Emit cached data first
    inventoryDao.getActiveInventories()
        .collect { entities ->
            emit(entities.map { it.toModel() })
        }

    // Fetch fresh data
    try {
        val response = apiService.getActiveInventories()
        if (response.isSuccessful && response.body() != null) {
            val inventories = response.body()!!
            // Update cache
            inventoryDao.insertInventories(inventories.map { InventoryEntity.fromModel(it) })
        }
    } catch (e: Exception) {
        // Fail silently - user sees cached data
        Log.e(TAG, "Failed to refresh inventories", e)
    }
}
```

### Barcode Scanning Integration

```kotlin
// In Activity
private val barcodeScanner = BarcodeScanner(this)

// Start scan (button click)
binding.btnScan.setOnClickListener {
    barcodeScanner.startScan()
}

// Handle result
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    when (val result = barcodeScanner.parseScanResult(requestCode, resultCode, data)) {
        is ScanResult.Success -> {
            viewModel.searchProduct(result.barcode)
        }
        is ScanResult.Cancelled -> {
            Toast.makeText(this, "Scan annulé", Toast.LENGTH_SHORT).show()
        }
        is ScanResult.Error -> {
            Toast.makeText(this, "Erreur: ${result.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

### Synchronization Pattern

Use WorkManager for background sync:

```kotlin
// Create Worker
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = InventoryRepository(TokenManager(applicationContext))

            // Get pending lines
            val pendingLines = inventoryLineDao.getPendingSyncLines()

            if (pendingLines.isNotEmpty()) {
                // Sync to server
                repository.synchronizeInventoryLines(pendingLines.map { it.toModel() })
                    .fold(
                        onSuccess = {
                            // Mark as synced
                            pendingLines.forEach {
                                it.syncStatus = "SYNCED"
                                it.locallyModified = false
                                inventoryLineDao.updateLine(it)
                            }
                            Result.success()
                        },
                        onFailure = {
                            Result.retry()
                        }
                    )
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule periodic sync
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "inventory_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
)
```

## Common Pitfalls

- **JDK Version**: Must use JDK 17 (not Java 25 like backend)
- **Network Configuration**: Use local IP address (192.168.x.x), NOT localhost for device testing
- **Coroutines**: Always use `viewModelScope` in ViewModels, NOT `GlobalScope`
- **LiveData Observers**: Remove observers in `onDestroy()` or use `viewLifecycleOwner` in Fragments
- **ViewBinding**: Enable in build.gradle (`viewBinding = true`)
- **ProGuard**: Test release builds thoroughly
- **Retrofit Responses**: Always check `response.isSuccessful` and `response.body() != null`
- **Token Expiration**: Handle 401 Unauthorized errors (token expired)
- **Room Queries**: Use Flow for reactive queries, suspend functions for one-time operations
- **Background Work**: Use WorkManager, NOT Services or Alarms

## Code Style

**Kotlin Conventions:**
- Official Kotlin code style
- Use data classes for models
- Prefer `val` over `var`
- Use named parameters for clarity
- Extension functions for reusable logic

**MVVM Best Practices:**
- ViewModels should NOT reference Views/Activities
- Use LiveData/Flow for observable data
- Single responsibility: one ViewModel per screen
- Repository pattern for all data access
- Avoid logic in Activities (delegate to ViewModels)

**Naming Conventions:**
- Activities: `*Activity.kt` (e.g., `LoginActivity.kt`)
- ViewModels: `*ViewModel.kt` (e.g., `LoginViewModel.kt`)
- Repositories: `*Repository.kt` (e.g., `AuthRepository.kt`)
- API Services: `*ApiService.kt` (e.g., `InventoryApiService.kt`)
- Layouts: `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`
- IDs in XML: snake_case (e.g., `btn_login`, `tv_total_price`)

## Next Development Steps

### Priority 1: Core UI Implementation

1. **Create Resource Files:**
   - strings.xml (French translations)
   - colors.xml (Material Design colors)
   - themes.xml (App theme)

2. **Splash & Login:**
   - Implement SplashActivity with auto-login logic
   - Implement LoginActivity with authentication flow
   - Create ViewModels and layouts

3. **Main Screen:**
   - Implement MainActivity as home/dashboard
   - Show user info, quick stats
   - Navigate to inventory list

### Priority 2: Inventory List & Detail

1. **Inventory List:**
   - Implement InventoryListActivity
   - Create InventoryAdapter for RecyclerView
   - Show active inventories with status

2. **Inventory Detail:**
   - Implement InventoryDetailActivity
   - Integrate barcode scanning
   - Allow quantity input
   - Save to local database

3. **ViewModels:**
   - InventoryListViewModel
   - InventoryDetailViewModel
   - Use Flow for reactive updates

### Priority 3: Synchronization

1. **WorkManager Integration:**
   - Create SyncWorker
   - Schedule periodic sync
   - Handle conflicts

2. **Network Monitoring:**
   - Detect online/offline status
   - Trigger sync when online
   - Show sync indicators

### Priority 4: Polish & Testing

1. **Error Handling:**
   - Centralized error handler
   - User-friendly messages
   - Retry mechanisms

2. **Testing:**
   - Unit tests for repositories
   - Integration tests for database
   - UI tests with Espresso

3. **Device Testing:**
   - Test on tablets
   - Test on warehouse devices
   - Test barcode scanning

## Resources

- [Parent Backend Project](../src/main/java/com/kobe/warehouse/service/stock/impl/InventaireServiceImpl.java)
- [Angular Reference](../src/main/webapp/app/entities/store-inventory/)
- [Sales Android Reference](../sales-android/)
- [Android Developer Guide](https://developer.android.com/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## Contact

For questions or issues, contact the development team.
