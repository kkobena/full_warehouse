# Pharma Mobile Report

## Project Overview

Pharma Mobile Report is an Android application designed for pharmacists to access and analyze key data from their pharmacy management system. The app provides a mobile-first experience for viewing reports, tracking performance, and making informed decisions on the go.

The application is built using modern Android development practices and relies on a Spring Boot backend for data processing and retrieval.

## Technologies

*   **Programming Language:** [Kotlin](https://kotlinlang.org/)
*   **Architecture:** MVVM (Model-View-ViewModel) with Android Jetpack
*   **Core Libraries:**
    *   **UI:** Material Components, ViewBinding, DataBinding
    *   **Asynchronous Programming:** Kotlin Coroutines
    *   **Dependency Injection:** Koin (implied, common with this stack)
    *   **Navigation:** Android Jetpack Navigation Component
    *   **Networking:** Retrofit, OkHttp
    *   **Database:** Room for offline caching
    *   **Background Processing:** WorkManager
    *   **Charts:** MPAndroidChart
    *   **Image Loading:** Coil
    *   **Push Notifications:** Firebase Cloud Messaging (FCM)
    *   **Analytics:** Firebase Analytics
*   **Build Tool:** Gradle

## Building and Running

### Prerequisites

*   Android Studio
*   Java 17
*   Access to the backend server

### Build Instructions

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    ```
2.  **Open the project in Android Studio.**
3.  **Configure `local.properties`:**
    Create a `local.properties` file in the root of the project and add the following line:
    ```properties
    sdk.dir=<path-to-your-android-sdk>
    ```
4.  **Configure `gradle.properties` for release builds:**
    For release builds, you need to provide the keystore information in the `gradle.properties` file:
    ```properties
    KEYSTORE_PASSWORD=<your-keystore-password>
    KEY_ALIAS=<your-key-alias>
    KEY_PASSWORD=<your-key-password>
    ```
5.  **Build the project:**
    ```bash
    ./gradlew build
    ```

### Running the Application

1.  **Select the `debug` build variant** in Android Studio.
2.  **Run the application** on an emulator or a physical device.

The application will start with the login screen. After successful authentication, you will be redirected to the main dashboard.

## Development Conventions

*   **Branching Strategy:** The project follows the GitFlow branching model.
*   **Code Style:** The project adheres to the official Kotlin coding conventions.
*   **Testing:** Unit tests are written using JUnit, and UI tests are written using Espresso.
*   **Dependency Management:** Dependencies are managed using the `libs.versions.toml` file.

## Backend Integration

The mobile application communicates with a Spring Boot backend via a REST API. The backend provides the necessary endpoints for retrieving data for the reports and other features. The backend services are already implemented, and the mobile app needs to consume these services.

The base URL for the API is defined in the `build.gradle.kts` file for each build type.

## Key Features

The application provides the following key features:

*   **Dashboard:** A summary of the key performance indicators (KPIs) of the pharmacy.
*   **Reports:** A comprehensive set of reports, including:
    *   Sales analysis
    *   Stock valuation
    *   Profitability analysis
    *   Supplier performance
    *   Third-party receivables
*   **Forecasting:** Sales forecasting based on historical data (currently disabled).
*   **Custom Reports:** A tool for building custom reports (to be implemented).
*   **Alerts:** Notifications for important events, such as low stock levels.
*   **Product Search:** A feature for searching products and viewing their details.
*   **Barcode Scanning:** A feature for scanning barcodes to quickly access product information.
*   **Offline Support:** The application uses a local database to cache data for offline access.
*   **Push Notifications:** The application uses Firebase Cloud Messaging to send push notifications to users.
*   **Home Screen Widget:** A widget for displaying key information on the home screen.
