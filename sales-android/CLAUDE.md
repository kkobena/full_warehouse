# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pharma Smart Sales Android is a native Android mobile application for pharmacy sales (POS/Point of Sale) built with Kotlin. It connects to the Pharma Smart Spring Boot backend (parent directory) to handle pharmaceutical sales, inventory checking, payment processing, and thermal receipt printing on Sunmi devices. The app implements JWT authentication matching the web application's security model.

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
- LiveData for reactive UI updates
- ViewBinding and DataBinding

**Key Libraries:**
- Retrofit 2.9.0 + OkHttp 4.12.0 - REST API client
- Gson 2.10.1 - JSON serialization
- AndroidX Lifecycle 2.7.0 - ViewModel, LiveData
- Material Components 1.11.0 - Material Design UI
- Security Crypto 1.1.0-alpha06 - Encrypted SharedPreferences for token storage
- Room 2.6.1 - Local database for offline caching
- Sunmi PrinterLibrary 1.0.18 - Thermal receipt printing
- ZXing 3.5.3 + journeyapps 4.3.0 - Barcode scanning and QR code generation
- Coil 2.5.0 - Image loading
- Paging 3.2.1 - Pagination support
- Navigation Component 2.7.6 - Navigation framework

**Java Version:**
- JDK 17 (configured in gradle.properties: `org.gradle.java.home=C:/Users/k.kobena/Documents/jdk17`)
- IMPORTANT: This project must use JDK 17, NOT Java 25 like the parent backend project

## Build & Development Commands

### Gradle Commands

**Note:** On Windows, use `gradlew.bat` or `.\gradlew.bat` in PowerShell. On Linux/macOS, use `./gradlew`.

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

# View dependencies
./gradlew dependencies

# View tasks
./gradlew tasks
```

### Development Workflow

```bash
# Clean build and install on device
./gradlew clean assembleDebug installDebug

# Build release APK (output: build/outputs/apk/release/)
./gradlew clean assembleRelease

# Run tests with coverage
./gradlew testDebugUnitTest
```

## Project Structure

```
sales-android/
├── build.gradle                    # Module build configuration
├── gradle.properties               # Gradle properties (JDK 17 path)
├── proguard-rules.pro             # ProGuard rules for release builds
├── src/main/
│   ├── AndroidManifest.xml        # App manifest, permissions, activities
│   ├── java/com/kobe/warehouse/sales/
│   │   ├── data/                  # Data layer (MVVM Model)
│   │   │   ├── api/               # Retrofit API service interfaces
│   │   │   │   ├── AuthApiService.kt
│   │   │   │   ├── SalesApiService.kt
│   │   │   │   ├── ProductApiService.kt
│   │   │   │   ├── CustomerApiService.kt
│   │   │   │   └── PaymentApiService.kt
│   │   │   ├── model/             # Data models (DTOs)
│   │   │   │   ├── auth/          # Authentication models
│   │   │   │   ├── Sale.kt
│   │   │   │   ├── SaleLine.kt
│   │   │   │   ├── Product.kt
│   │   │   │   ├── Customer.kt
│   │   │   │   └── PaymentMode.kt
│   │   │   └── repository/        # Repository layer (data access)
│   │   │       ├── AuthRepository.kt
│   │   │       ├── SalesRepository.kt
│   │   │       ├── ProductRepository.kt
│   │   │       ├── CustomerRepository.kt
│   │   │       └── PaymentRepository.kt
│   │   ├── ui/                    # UI layer (MVVM View)
│   │   │   ├── activity/          # Activities
│   │   │   │   ├── SplashActivity.kt
│   │   │   │   ├── LoginActivity.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── SalesHomeActivity.kt
│   │   │   │   └── ComptantSaleActivity.kt
│   │   │   ├── adapter/           # RecyclerView adapters
│   │   │   │   ├── SalesAdapter.kt
│   │   │   │   ├── ProductAdapter.kt
│   │   │   │   ├── CartAdapter.kt
│   │   │   │   └── PaymentModeAdapter.kt
│   │   │   ├── dialog/            # Dialog fragments
│   │   │   │   ├── ServerConfigDialog.kt
│   │   │   │   └── PaymentDialogFragment.kt
│   │   │   └── viewmodel/         # ViewModels (MVVM ViewModel)
│   │   │       ├── LoginViewModel.kt
│   │   │       ├── SalesHomeViewModel.kt
│   │   │       ├── ComptantSaleViewModel.kt
│   │   │       └── ViewModelFactory.kt
│   │   ├── printer/               # Thermal printer integration
│   │   │   ├── SunmiPrinterService.kt
│   │   │   └── ReceiptPrinter.kt
│   │   └── utils/                 # Utilities
│   │       ├── ApiClient.kt       # Retrofit client factory
│   │       └── TokenManager.kt    # JWT token storage (encrypted)
│   └── res/
│       ├── layout/                # XML layouts
│       │   ├── activity_login.xml
│       │   ├── activity_sales_home.xml
│       │   ├── activity_comptant_sale.xml
│       │   ├── item_sale.xml
│       │   ├── item_product_list.xml
│       │   ├── item_product_grid.xml
│       │   ├── item_cart.xml
│       │   ├── item_payment_mode.xml
│       │   └── dialog_*.xml
│       ├── values/
│       │   ├── strings.xml
│       │   ├── colors.xml
│       │   └── themes.xml
│       └── drawable/              # Icons, graphics
├── comptantsale.md               # UI implementation specs for POS screen
├── fullsale.md                   # UI implementation specs for full sales
└── README.md                     # Project documentation
```

## Key Design Patterns & Conventions

### MVVM Architecture

The app strictly follows MVVM pattern:

**Model (data/):**
- Data models in `data/model/` (Kotlin data classes)
- API interfaces in `data/api/` (Retrofit service definitions)
- Repository classes in `data/repository/` (business logic, API calls)

**View (ui/):**
- Activities in `ui/activity/` (UI controllers)
- XML layouts in `res/layout/` (UI structure)
- ViewBinding for type-safe view access
- Observers for LiveData from ViewModels

**ViewModel (ui/viewmodel/):**
- Manages UI state with LiveData
- No Android framework dependencies (except AndroidX)
- Uses Coroutines for async operations via `viewModelScope`
- Repository dependency injection via ViewModelFactory

### Authentication & Security

**JWT Token Flow:**
1. User logs in via `LoginActivity` → `LoginViewModel` → `AuthRepository`
2. Backend returns JWT tokens (accessToken, refreshToken, tokenType, expiresIn)
3. Tokens stored securely in `TokenManager` using EncryptedSharedPreferences
4. `ApiClient` adds `Authorization: Bearer {token}` header to all API requests
5. Custom header `x-PHARMA-SMART-ANDROID: true` identifies Android client
6. Token expiration: 8 hours (28800 seconds)

**Security Features:**
- EncryptedSharedPreferences for token/credential storage
- HTTPS support (usesCleartextTraffic=true for dev/testing only)
- Auto-login with saved credentials (optional "Remember Me")
- Server URL configuration via `ServerConfigDialog` (stored in TokenManager)

### API Client Pattern

**ApiClient.kt:**
- Singleton object creating Retrofit instances
- Base URL priority: 1) parameter, 2) TokenManager config, 3) BuildConfig.BASE_URL
- OkHttp client with 30s timeouts
- Logging interceptor (enabled in debug builds only)
- Auth interceptor adds JWT token header

**Repository Pattern:**
- All API calls in repository classes (not in ViewModels)
- Return `Result<T>` for success/failure handling
- Use `withContext(Dispatchers.IO)` for background execution
- Fold pattern: `.fold(onSuccess = {}, onFailure = {})`

### Printer Integration

**Sunmi Thermal Printer:**
- Sunmi V2 devices with built-in thermal printer
- `SunmiPrinterService.kt` wraps Sunmi SDK with Kotlin coroutines
- Paper widths: 80mm (48 chars), 58mm (32 chars)
- Font sizes: NORMAL (24f), LARGE (28f), XLARGE (32f)
- Alignment: LEFT (0), CENTER (1), RIGHT (2)

**Receipt Printing:**
- `ReceiptPrinter.kt` handles receipt formatting and printing
- Receipt data from backend: `GET /api/sales/{id}/{date}/receipt`
- QR codes printed from Base64 strings (payment modes with QR support)
- Confirmation dialog before printing: "Souhaitez-vous imprimer le reçu?" (Yes/No)

**Printer Operations:**
```kotlin
// Connect to printer
val connected = printerService.connect()

// Print receipt sections
printerService.printLine("Header", ALIGN_CENTER, FONT_SIZE_LARGE, isBold = true)
printerService.printSeparator()
printerService.printLabelValue("Total:", "1000 FCFA")
printerService.printQrCode(base64String, ALIGN_CENTER)
printerService.feedPaper(3)
printerService.cutPaper()
```

### Responsive Layouts

**Screen Size Adaptation:**
- Small screens (smartphones): List view for products, vertical cart layout
- Large screens (tablets): Grid view for products, side-by-side cart/product layout
- Use `ViewPager2` for swipeable layouts
- Material Design responsive patterns

**Layout Strategy:**
- `item_product_list.xml` - List item for small screens
- `item_product_grid.xml` - Grid item for tablets
- Determine layout at runtime based on screen size/orientation

## Backend Integration

### API Base URL Configuration

**Build-time Configuration (build.gradle:22):**
```gradle
buildConfigField "String", "BASE_URL", "\"http://192.168.1.100:8080/\""
```

**Runtime Configuration:**
- Users can change server URL via settings/configuration dialog
- URL saved in `TokenManager.saveBaseUrl(url)`
- IMPORTANT: Must use local network IP (not localhost) for device testing
- Backend must be accessible from device on same network

### Key API Endpoints

**Authentication:**
- POST `/api/auth/login` - Login with username/password
- GET `/api/account` - Get current user details
- POST `/api/auth/refresh` - Refresh access token

**Sales:**
- GET `/api/sales/cash-sales/ongoing` - Get ongoing sales list
- GET `/api/sales/{id}/{date}` - Get sale by ID and date
- POST `/api/sales/cash-sales` - Create new cash sale
- PUT `/api/sales/{id}/{date}` - Update sale
- DELETE `/api/sales/ongoing/{id}` - Delete ongoing sale
- POST `/api/sales/{id}/{date}/finalize` - Finalize sale with payments
- GET `/api/sales/{id}/{date}/receipt` - Get receipt data for printing

**Products:**
- GET `/api/products/search?q={query}` - Search products by name/code
- GET `/api/products/code/{code}` - Get product by barcode

**Payments:**
- GET `/api/payment-modes` - Get available payment modes

**Customers:**
- GET `/api/customers/default` - Get default customer (comptant)
- GET `/api/customers/search?q={query}` - Search customers

### Data Models Alignment

**Sale Model:**
- Maps to backend `com.kobe.warehouse.domain.Sales` entity
- Key fields: `id`, `numberTransaction`, `salesAmount`, `customer`, `salesLines`, `saleId` (composite key)
- Composite key pattern: `SaleId(id: Long, saleDate: String)`

**Product Model:**
- Maps to backend `com.kobe.warehouse.domain.Produit` entity
- Key fields: `id`, `productCode`, `productName`, `regularUnitPrice`, `currentStockQuantity`

**PaymentMode Model:**
- Maps to backend `com.kobe.warehouse.domain.PaymentMode` entity
- Key fields: `id`, `libelle`, `code`, `enable`, `qrCode` (Base64 QR code for mobile money)

## Development Workflows

### Adding a New Feature

1. **Create Data Model** in `data/model/` (Kotlin data class matching backend DTO)
2. **Add API Endpoint** in `data/api/` (Retrofit interface method)
3. **Implement Repository** in `data/repository/` (suspend functions returning `Result<T>`)
4. **Create ViewModel** in `ui/viewmodel/` (LiveData for UI state, call repository methods)
5. **Design Layout** in `res/layout/` (XML with ViewBinding/DataBinding)
6. **Implement Activity/Fragment** in `ui/activity/` or `ui/fragment/` (observe LiveData, update UI)
7. **Update AndroidManifest.xml** if adding new Activity

### Testing on Device

**Prerequisites:**
- Backend must be running and accessible on network
- Update BASE_URL in build.gradle with your machine's local IP
- Enable USB debugging on Android device OR use emulator

**Steps:**
```bash
# 1. Connect device/start emulator
adb devices

# 2. Build and install
./gradlew installDebug

# 3. View logs
adb logcat | grep "PharmaSmart"

# 4. Test with backend
# Ensure backend is running on http://<YOUR_IP>:8080
# Use ServerConfigDialog in app to change URL if needed
```

### Working with Sunmi Printer

**Device Requirements:**
- Sunmi V2 series device with built-in thermal printer
- Sunmi Inner Printer Service must be installed

**Testing Printer:**
1. Connect to printer: `printerService.connect()`
2. Check status: `printerService.getPrinterStatus()`
3. Print test receipt: `printerService.printLine("Test")`

**Common Printer Issues:**
- Printer not connected: Call `connect()` first
- Out of paper: Check `getPrinterStatus()` returns `OUT_OF_PAPER`
- Overheated: Wait for printer to cool down

### ProGuard Configuration

**Release Build:**
- ProGuard enabled for release builds (minifyEnabled=true)
- Rules in `proguard-rules.pro`
- Keep Retrofit, Gson, Room, and model classes
- Test release builds on device before production

## UI Implementation Guidelines

### Sales Home Screen (SalesHomeActivity)

**If `useSimpleSale = true`:**
- Show search field and "Nouvelle vente" button
- List ongoing sales with: `numberTransaction`, `updatedAt`, `customer.firstName`, `customer.lastName`
- Click sale → open `ComptantSaleActivity`

**If `useSimpleSale = false`:**
- Tab layout: "Vente en cours" and "Prévente" tabs
- Support different sale types: Comptant, Carnet, Assurance

### POS Screen (ComptantSaleActivity)

**Layout Requirements (from comptantsale.md):**
- Responsive layout: list view (smartphones) vs grid view (tablets)
- Product search by name or code
- Cart with quantity controls (+/-)
- Total price display
- Payment dialog with multiple payment methods
- QR code scanning for mobile money payments
- Print receipt after successful sale (with confirmation dialog)

**Key Features:**
- Real-time cart updates using LiveData
- Barcode scanning for quick product lookup
- Multi-payment support (cash, card, mobile money)
- Receipt printing with Sunmi printer
- Error handling with user-friendly messages

## Common Pitfalls

- **JDK Version**: Must use JDK 17 (not Java 25 like backend). Configured in gradle.properties.
- **Network Configuration**: Use local IP address (192.168.x.x), NOT localhost or 127.0.0.1 for device testing.
- **EncryptedSharedPreferences**: Requires min SDK 23+, this project uses SDK 30+ so no issues.
- **Sunmi Printer**: Only works on Sunmi devices. Use emulator/regular devices for non-printer testing.
- **Coroutines**: Always use `viewModelScope` in ViewModels, NOT `GlobalScope`.
- **LiveData Observers**: Remove observers in `onDestroy()` or use `viewLifecycleOwner` in Fragments.
- **ViewBinding**: Enable in build.gradle (`viewBinding = true`). Access views via `binding.viewId`.
- **ProGuard**: Test release builds thoroughly. Add keep rules for reflection-based libraries.
- **Retrofit Responses**: Always check `response.isSuccessful` and `response.body() != null`.
- **Token Expiration**: Handle 401 Unauthorized errors (token expired, need re-login).

## Debugging

**Enable Logging:**
- HTTP logging enabled automatically in debug builds (ApiClient.kt)
- View all logs: `adb logcat | grep "PharmaSmart"`
- View specific tag: `adb logcat -s SunmiPrinterService`

**Common Debug Commands:**
```bash
# View app logs
adb logcat | grep "com.kobe.warehouse.sales"

# Clear app data (logout)
adb shell pm clear com.kobe.warehouse.sales

# Check network connectivity
adb shell ping <backend-ip>

# View database (if using Room)
adb shell run-as com.kobe.warehouse.sales
cd databases
```

**Android Studio Debugging:**
- Set breakpoints in ViewModels and Repositories
- Use "Debug" instead of "Run" configuration
- Inspect LiveData values in debugger
- Use Profiler for performance analysis

## Code Style

**Kotlin Conventions:**
- Official Kotlin code style (configured in gradle.properties)
- Use data classes for models
- Prefer `val` over `var`
- Use named parameters for clarity
- Extension functions for reusable logic

**MVVM Best Practices:**
- ViewModels should NOT reference Views/Activities
- Use LiveData for observable data
- Single responsibility: one ViewModel per screen
- Repository pattern for all data access
- Avoid logic in Activities (delegate to ViewModels)

**Naming Conventions:**
- Activities: `*Activity.kt` (e.g., `LoginActivity.kt`)
- ViewModels: `*ViewModel.kt` (e.g., `LoginViewModel.kt`)
- Repositories: `*Repository.kt` (e.g., `AuthRepository.kt`)
- API Services: `*ApiService.kt` (e.g., `SalesApiService.kt`)
- Layouts: `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`
- IDs in XML: snake_case (e.g., `btn_login`, `tv_total_price`)
- not use produitId = product.produitId,