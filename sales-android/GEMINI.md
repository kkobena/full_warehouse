# Pharma Smart - Sales Android Module

## Project Overview

This is a native Android application that serves as the sales module for the Pharma Smart pharmacy management system. It's built with Kotlin and follows the MVVM (Model-View-ViewModel) architecture. The app uses JWT for authentication and communicates with a Spring Boot backend via a REST API.

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Core Libraries**:
  - AndroidX Lifecycle (ViewModel, LiveData)
  - Coroutines for asynchronous operations
  - Retrofit for networking
  - Gson for JSON serialization
  - Room for local database (optional)
  - Paging 3 for pagination
  - Navigation Component for in-app navigation
- **UI**:
  - Material Components for UI elements
  - ViewBinding and DataBinding
  - ViewPager2 and CardView
- **Security**:
  - EncryptedSharedPreferences for secure data storage
- **Hardware Integration**:
  - Sunmi Printer Library for thermal receipt printing
  - ZXing for QR code scanning

## Building and Running

### Prerequisites

- Android Studio
- JDK 8 or higher
- A running instance of the Pharma Smart Spring Boot backend

### Configuration

The backend URL is configured in the `build.gradle` file. For development with an emulator, the default URL `http://10.0.2.2:9080/` should work. For a physical device, you need to change this to your computer's IP address.

```gradle
// in build.gradle
buildConfigField "String", "BASE_URL", "\"http://YOUR_SERVER_IP:9080/\""
```

### Build and Run Commands

You can build and run the application from Android Studio or using the following Gradle commands:

- **Build debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Install on a connected device:**
  ```bash
  ./gradlew installDebug
  ```
- **Run unit tests:**
  ```bash
  ./gradlew test
  ```
- **Run instrumented tests:**
  ```bash
  ./gradlew connectedAndroidTest
  ```

## Development Conventions

- **Architecture**: Strictly follow the MVVM pattern.
- **Asynchronous Code**: Use Kotlin Coroutines for all asynchronous tasks.
- **UI State**: Use `LiveData` or `StateFlow` to expose UI state from `ViewModel`s.
- **Dependency Injection**: While not explicitly defined, consider using a DI framework like Hilt or Koin for managing dependencies.
- **Code Style**: Adhere to the standard Kotlin coding conventions.
- **Error Handling**: Implement comprehensive error handling for API calls and display appropriate feedback to the user.
- **Security**: Store all sensitive information, like API keys and user credentials, in `EncryptedSharedPreferences`.
