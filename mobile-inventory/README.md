# Pharma Smart Inventory - Mobile Android Application

## Project Overview

Pharma Smart Inventory is a native Android mobile application for warehouse inventory management built with Kotlin. It connects to the Pharma Smart Spring Boot backend to handle pharmaceutical inventory counting, barcode scanning, offline data storage, and server synchronization. The app implements JWT authentication matching the web application's security model.

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
- **Retrofit 2.9.0 + OkHttp 4.12.0** - REST API client
- **Gson 2.10.1** - JSON serialization
- **Room 2.6.1** - Local database for offline storage
- **AndroidX Lifecycle 2.7.0** - ViewModel, LiveData
- **Material Components 1.11.0** - Material Design UI
- **Security Crypto 1.1.0-alpha06** - Encrypted SharedPreferences
- **ZXing 3.5.3 + journeyapps 4.3.0** - Barcode scanning
- **WorkManager 2.9.0** - Background synchronization
- **Navigation Component 2.7.6** - Navigation framework

**Java Version:**
- **JDK 17** (configured in gradle.properties: `org.gradle.java.home=C:/Users/k.kobena/Documents/jdk17`)
- **IMPORTANT:** This project must use JDK 17, NOT Java 25 like the parent backend project

## Project Structure

```
mobile-inventory/
├── build.gradle                    # Module build configuration
├── gradle.properties               # Gradle properties (JDK 17 path)
├── proguard-rules.pro             # ProGuard rules for release builds
├── src/main/
│   ├── AndroidManifest.xml        # App manifest, permissions, activities
│   ├── java/com/kobe/warehouse/inventory/
│   │   ├── data/                  # Data layer (MVVM Model)
│   │   │   ├── api/               # Retrofit API service interfaces
│   │   │   │   ├── AuthApiService.kt
│   │   │   │   └── InventoryApiService.kt
│   │   │   ├── model/             # Data models (DTOs)
│   │   │   │   ├── auth/          # Authentication models
│   │   │   │   ├── StoreInventory.kt
│   │   │   │   ├── StoreInventoryLine.kt
│   │   │   │   ├── Rayon.kt
│   │   │   │   ├── Product.kt
│   │   │   │   ├── InventoryCategory.kt
│   │   │   │   └── InventoryStatut.kt
│   │   │   ├── repository/        # Repository layer (data access)
│   │   │   │   ├── AuthRepository.kt
│   │   │   │   └── InventoryRepository.kt
│   │   │   └── database/          # Room database (offline storage)
│   │   │       ├── InventoryDatabase.kt
│   │   │       ├── dao/           # Data Access Objects
│   │   │       │   ├── InventoryDao.kt
│   │   │       │   └── InventoryLineDao.kt
│   │   │       └── entity/        # Room entities
│   │   │           ├── InventoryEntity.kt
│   │   │           └── InventoryLineEntity.kt
│   │   ├── ui/                    # UI layer (MVVM View)
│   │   │   ├── activity/          # Activities (to be implemented)
│   │   │   │   ├── SplashActivity.kt
│   │   │   │   ├── LoginActivity.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── InventoryListActivity.kt
│   │   │   │   └── InventoryDetailActivity.kt
│   │   │   ├── adapter/           # RecyclerView adapters (to be implemented)
│   │   │   ├── dialog/            # Dialog fragments (to be implemented)
│   │   │   └── viewmodel/         # ViewModels (to be implemented)
│   │   ├── scanner/               # Barcode scanning
│   │   │   └── BarcodeScanner.kt
│   │   ├── sync/                  # Synchronization service (to be implemented)
│   │   └── utils/                 # Utilities
│   │       ├── ApiClient.kt       # Retrofit client factory
│   │       ├── TokenManager.kt    # JWT token storage (encrypted)
│   │       └── SessionManager.kt  # Session management
│   └── res/
│       ├── layout/                # XML layouts (to be implemented)
│       ├── values/
│       │   ├── strings.xml        # (to be created)
│       │   ├── colors.xml         # (to be created)
│       │   └── themes.xml         # (to be created)
│       └── drawable/              # Icons, graphics (to be added)
└── README.md                      # This file
```

## Key Features Implemented

### ✅ Complete

1. **Project Structure & Configuration**
   - Gradle build configuration with all required dependencies
   - AndroidManifest with proper permissions (Camera, Internet, Storage)
   - ProGuard rules for release builds
   - Gradle wrapper scripts

2. **Authentication Module**
   - JWT token management with EncryptedSharedPreferences
   - Session management with broadcast events
   - Auth API service and repository
   - Server configuration support

3. **Inventory Data Models**
   - StoreInventory, StoreInventoryLine, Rayon, Product models
   - Enum classes (InventoryCategory, InventoryStatut)
   - Complete data classes matching backend DTOs

4. **API Services (Retrofit)**
   - AuthApiService - Login, account management
   - InventoryApiService - Inventory CRUD, sync, search

5. **Repository Layer**
   - AuthRepository - Authentication operations
   - InventoryRepository - Inventory operations with error handling
   - Proper coroutine usage and Result type returns

6. **Offline Storage (Room Database)**
   - InventoryEntity and InventoryLineEntity
   - InventoryDao and InventoryLineDao
   - Database with proper foreign keys and indexes
   - Sync status tracking

7. **Barcode Scanner**
   - BarcodeScanner utility using ZXing
   - Support for all barcode types
   - ScanResult sealed class for result handling

8. **Utilities**
   - ApiClient - Retrofit client factory with interceptors
   - TokenManager - Secure token storage
   - SessionManager - Session state management

### ⏳ To Be Implemented

The following components need to be implemented to complete the application:

1. **ViewModels**
   - LoginViewModel
   - InventoryListViewModel
   - InventoryDetailViewModel
   - Use AndroidX ViewModel with LiveData/Flow

2. **Activities & Fragments**
   - SplashActivity - Initial screen with auto-login
   - LoginActivity - User authentication
   - MainActivity - Home screen
   - InventoryListActivity - List active inventories
   - InventoryDetailActivity - Count products by scanning

3. **UI Layouts (XML)**
   - activity_splash.xml
   - activity_login.xml
   - activity_main.xml
   - activity_inventory_list.xml
   - activity_inventory_detail.xml
   - item_inventory.xml (RecyclerView item)
   - item_inventory_line.xml (RecyclerView item)
   - dialog_*.xml (Various dialogs)

4. **RecyclerView Adapters**
   - InventoryAdapter - Display inventories
   - InventoryLineAdapter - Display inventory lines
   - RayonAdapter - Display sections/aisles

5. **Synchronization Service**
   - WorkManager worker for background sync
   - Sync pending inventory lines when online
   - Conflict resolution strategy

6. **Resources**
   - strings.xml - French translations
   - colors.xml - App colors
   - themes.xml - Material Design theme
   - Drawable resources (icons, backgrounds)

7. **Error Handling & Logging**
   - Centralized error handler
   - Timber logging integration
   - User-friendly error messages

8. **Testing**
   - Unit tests for repositories
   - Integration tests for database
   - UI tests with Espresso

## API Endpoints

The app connects to these backend endpoints:

### Authentication
- **POST** `/api/auth/login` - Login with username/password
- **GET** `/api/account` - Get current user details
- **POST** `/api/auth/refresh` - Refresh access token

### Inventory
- **GET** `/api/store-inventories/actif` - Get active inventories
- **GET** `/api/store-inventories/{id}` - Get inventory by ID
- **GET** `/api/store-inventories/{id}/rayons` - Get inventory sections
- **GET** `/api/store-inventories/{inventoryId}/rayons/{rayonId}/items` - Get items by section
- **PUT** `/api/store-inventories/lines/{id}` - Update single line
- **PUT** `/api/store-inventories/lines` - Batch update lines (sync)
- **POST** `/api/store-inventories/close/{id}` - Close inventory
- **GET** `/api/products/code/{barcode}` - Search product by barcode

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

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Generate lint report
./gradlew lint
```

### Development Workflow

```bash
# Clean build and install on device
./gradlew clean assembleDebug installDebug

# Build release APK (output: build/outputs/apk/release/)
./gradlew clean assembleRelease

# View logs
adb logcat | grep "PharmaSmartInventory"
```

## Configuration

### Backend URL Configuration

**Build-time Configuration (build.gradle:27):**
```gradle
buildConfigField "String", "BASE_URL", "\"http://192.168.1.100:8080/\""
```

**Runtime Configuration:**
- Users can change server URL via settings/configuration dialog
- URL saved in TokenManager
- **IMPORTANT:** Must use local network IP (not localhost) for device testing

### JDK Configuration

**gradle.properties:**
```properties
org.gradle.java.home=C:/Users/k.kobena/Documents/jdk17
```

## Offline Functionality

The app uses Room database for offline storage:

1. **Data Synchronization:**
   - Inventory data is cached locally
   - Inventory lines can be counted offline
   - Pending changes are marked with `syncStatus = "PENDING"`
   - WorkManager syncs data when connection is restored

2. **Sync Status:**
   - `PENDING` - Not yet synced to server
   - `SYNCED` - Successfully synced
   - `ERROR` - Sync failed (requires retry)

3. **Conflict Resolution:**
   - Server data takes precedence
   - Local changes are merged on sync
   - User is notified of conflicts

## Barcode Scanning

The app uses ZXing for barcode scanning:

```kotlin
// In Activity
val barcodeScanner = BarcodeScanner(this)

// Start scan
barcodeScanner.startScan()

// Handle result in onActivityResult
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    when (val result = barcodeScanner.parseScanResult(requestCode, resultCode, data)) {
        is ScanResult.Success -> {
            // Handle scanned barcode
            searchProduct(result.barcode)
        }
        is ScanResult.Cancelled -> {
            // User cancelled scan
        }
        is ScanResult.Error -> {
            // Show error message
        }
    }
}
```

## Authentication Flow

1. **Splash Screen:**
   - Check if user is authenticated (valid token)
   - Auto-login if remember me is enabled
   - Navigate to MainActivity or LoginActivity

2. **Login:**
   - User enters username/password
   - App calls `/api/auth/login`
   - JWT token stored in EncryptedSharedPreferences
   - Navigate to MainActivity

3. **Token Management:**
   - Access token valid for 8 hours (28800 seconds)
   - Auto-logout on token expiration (401 error)
   - SessionManager broadcasts logout events

## Inventory Counting Workflow

1. **Select Inventory:**
   - List active inventories (status = OPEN)
   - Show inventory category (MAGASIN, RAYON, STORAGE, FAMILLY)

2. **Select Section (if RAYON inventory):**
   - List rayons/sections for the inventory
   - Navigate to inventory detail screen

3. **Count Products:**
   - Load inventory lines for the section
   - Scan barcode to find product
   - Enter quantity on hand
   - Mark line as counted (updated = true)
   - Save locally (Room database)

4. **Synchronize:**
   - Batch update counted lines to server
   - Server validates and updates stock
   - Clear local pending changes

5. **Close Inventory:**
   - All lines must be counted (updated = true)
   - Server calculates gaps and amounts
   - Inventory status changed to CLOSED

## Next Steps for Implementation

### Priority 1: Core UI (Week 1)

1. Create string resources (strings.xml, colors.xml, themes.xml)
2. Implement SplashActivity with auto-login logic
3. Implement LoginActivity with authentication flow
4. Implement MainActivity as home screen
5. Create InventoryListActivity to display active inventories

### Priority 2: Inventory Counting (Week 2)

1. Create InventoryDetailActivity for counting products
2. Implement barcode scanning integration
3. Create RecyclerView adapters (InventoryAdapter, InventoryLineAdapter)
4. Implement ViewModels for each screen
5. Add quantity input dialog

### Priority 3: Offline & Sync (Week 3)

1. Implement local caching with Room database
2. Create synchronization service with WorkManager
3. Handle network state changes
4. Implement conflict resolution
5. Add sync status indicators

### Priority 4: Polish & Testing (Week 4)

1. Add error handling and logging
2. Create user-friendly error messages
3. Implement loading indicators
4. Add unit tests and integration tests
5. Test on warehouse devices (Sunmi, Honeywell, Zebra)

## Device Compatibility

**Supported Devices:**
- Android tablets (landscape orientation)
- Warehouse-specific devices:
  - Sunmi devices (with integrated scanner)
  - Honeywell devices
  - Zebra devices
- Standard Android smartphones (with camera)

**Scanner Types:**
- Camera-based scanning (ZXing)
- Integrated hardware scanners (device-specific)
- External Bluetooth scanners

## Testing

### Device Testing

```bash
# Connect device
adb devices

# Install app
adb install build/outputs/apk/debug/mobile-inventory-debug.apk

# View logs
adb logcat | grep "InventoryRepository"
```

### Backend Configuration

Ensure backend is running and accessible:
- Backend URL: `http://<YOUR_IP>:8080/`
- Update build.gradle or use ServerConfigDialog in app

## Troubleshooting

**Common Issues:**

1. **Connection Refused:**
   - Ensure backend is running
   - Use local network IP (192.168.x.x), not localhost
   - Check firewall settings

2. **Build Errors:**
   - Ensure JDK 17 is configured in gradle.properties
   - Run `./gradlew clean` before building
   - Sync Gradle files in Android Studio

3. **Scanner Not Working:**
   - Grant camera permission in app settings
   - Check if device has camera
   - For warehouse devices, check manufacturer documentation

4. **Sync Issues:**
   - Check network connectivity
   - View pending sync items in database
   - Check WorkManager status

## Resources & References

- [Android Developer Guide](https://developer.android.com/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)
- [ZXing Barcode Scanner](https://github.com/journeyapps/zxing-android-embedded)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

## License

Proprietary - Pharma Smart © 2025

## Contact

For questions or issues, contact the development team.

---

**Status:** Foundation complete, UI implementation in progress
**Version:** 1.0.0
**Last Updated:** November 2025
