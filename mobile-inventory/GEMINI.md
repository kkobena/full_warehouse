# Gemini Project: Pharma Smart Inventory - Android

## Project Overview

This is a native Android mobile application for warehouse inventory management built with Kotlin. It's designed to work with the Pharma Smart Spring Boot backend, providing features like inventory counting, barcode scanning, offline data storage, and synchronization with the server. The application uses JWT for authentication, consistent with the main web application.

## Technology Stack

- **Platform:** Android
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel) with Repository pattern
- **Core Libraries:**
    - **AndroidX:** Core components, Lifecycle (ViewModel, LiveData), Room
    - **Networking:** Retrofit, OkHttp, Gson
    - **Asynchronous:** Coroutines, Flow
    - **UI:** Material Components, ViewBinding, DataBinding
    - **Database:** Room for offline storage
    - **Barcode Scanning:** ZXing
    - **Background Jobs:** WorkManager
    - **Security:** EncryptedSharedPreferences for token storage

## Building and Running

### Prerequisites

- **JDK 17:** The project is configured to use JDK 17. The path is specified in `gradle.properties`.
- **Android SDK:** `minSdk` is 30 (Android 11) and `targetSdk` is 36.
- **Backend Server:** A running instance of the Pharma Smart Spring Boot backend is required. The base URL is configured in `build.gradle` and can be changed at runtime.

### Build Commands

Use the Gradle wrapper (`gradlew.bat` for Windows, `./gradlew` for Linux/macOS) to build and run the project.

- **Build debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```

- **Build release APK:**
  ```bash
  ./gradlew assembleRelease
  ```

- **Install on a connected device/emulator:**
  ```bash
  ./gradlew installDebug
  ```

- **Clean the build:**
  ```bash
  ./gradlew clean
  ```

### Running Tests

- **Run unit tests:**
  ```bash
  ./gradlew test
  ```

- **Run instrumented tests (requires a connected device/emulator):**
  ```bash
  ./gradlew connectedAndroidTest
  ```

## Development Conventions

- **Code Style:** The project follows the official Kotlin code style.
- **Asynchronous Operations:** Coroutines and Flow are used for managing background threads and asynchronous tasks.
- **Data Layer:** The repository pattern is used to abstract data sources (network and local database).
- **Offline First:** The application is designed to be "offline-first," using a Room database to cache data and a `WorkManager` to synchronize data with the server.
- **Modularity:** The code is organized into `data`, `ui`, `scanner`, `sync`, and `utils` packages, following the MVVM pattern.

## Key Implementation Points

- **Authentication:** JWT tokens are securely stored using `EncryptedSharedPreferences`. The `AuthRepository` handles login, logout, and account management.
- **Offline Storage:** `Room` is used to store inventory data locally. `InventoryDao` and `InventoryLineDao` provide interfaces for database operations.
- **API Communication:** `Retrofit` is used for making network requests to the backend API. `AuthApiService` and `InventoryApiService` define the API endpoints.
- **Barcode Scanning:** The `ZXing` library is integrated for barcode scanning functionality.
- **Background Sync:** `WorkManager` is used to handle background data synchronization, ensuring that offline changes are sent to the server when the device is online.
- **To-Be-Implemented:** The `README.md` file contains a detailed list of components that are yet to be implemented, including UI screens (Activities, Fragments, Layouts), ViewModels, and RecyclerView Adapters.

## Next Steps

Based on the `IMPLEMENTATION_GUIDE.md`, the following are the immediate next steps to continue the development of the application:

1.  **Create Resource Files:**
    *   `colors.xml`: Define the color palette for the application.
    *   `strings.xml`: Add all user-facing strings for internationalization (starting with French).
    *   `themes.xml`: Configure the application's theme and splash screen theme.

2.  **Implement Core UI:**
    *   **SplashActivity:** Create the initial splash screen with logic to navigate to the `MainActivity` if the user is authenticated, or to the `LoginActivity` otherwise.
    *   **LoginActivity:** Implement the login screen with username and password fields, a "remember me" checkbox, and a login button.
    *   **LoginViewModel:** Create the ViewModel to handle the login logic, including input validation and communication with the `AuthRepository`.

3.  **Implement Inventory List:**
    *   **InventoryListActivity:** Create the screen to display the list of active inventories.
    *   **InventoryAdapter:** Implement a `RecyclerView.Adapter` to display the inventory items.
    *   **InventoryListViewModel:** Create the ViewModel to fetch and manage the inventory list data.

4.  **Implement Inventory Detail and Scanning:**
    *   **InventoryDetailActivity:** Create the screen for counting products, including barcode scanning integration.
    *   **InventoryDetailViewModel:** Create the ViewModel to manage the state of the inventory detail screen.
    *   Implement the quantity input dialog.

5.  **Implement Background Synchronization:**
    *   **SyncWorker:** Create a `WorkManager` to handle background data synchronization.
    *   Schedule periodic synchronization and handle network state changes.
