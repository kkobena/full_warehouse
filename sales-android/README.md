# Pharma Smart - Sales Android Module

Android Kotlin module for the Pharma Smart pharmacy management system. This module provides a native Android interface for sales management with JWT authentication.

## Overview

This is a standalone Android module built with Kotlin that connects to the Pharma Smart Spring Boot backend. It implements the same authentication logic as the web application, using JWT tokens for secure API access.

## Technology Stack

- **Language**: Kotlin
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Libraries**:
  - Retrofit 2.9.0 - REST API client
  - OkHttp 4.12.0 - HTTP client
  - Gson 2.10.1 - JSON serialization
  - AndroidX Lifecycle 2.7.0 - ViewModel and LiveData
  - Material Components 1.11.0 - Material Design UI
  - Security Crypto 1.1.0 - Encrypted SharedPreferences
  - Coroutines 1.7.3 - Asynchronous programming

## Project Structure

```
sales-android/
├── build.gradle                          # Module build configuration
├── src/main/
│   ├── AndroidManifest.xml              # App manifest
│   ├── java/com/kobe/warehouse/sales/
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   └── AuthApiService.kt    # Retrofit API interface
│   │   │   ├── model/
│   │   │   │   └── auth/
│   │   │   │       ├── LoginRequest.kt       # Login request model
│   │   │   │       ├── JwtTokenResponse.kt   # JWT token response
│   │   │   │       └── Account.kt            # User account model
│   │   │   └── repository/
│   │   │       └── AuthRepository.kt    # Authentication repository
│   │   ├── ui/
│   │   │   ├── activity/
│   │   │   │   ├── LoginActivity.kt     # Login screen
│   │   │   │   ├── SplashActivity.kt    # Splash screen
│   │   │   │   └── MainActivity.kt      # Main app screen
│   │   │   └── viewmodel/
│   │   │       └── LoginViewModel.kt    # Login view model
│   │   └── utils/
│   │       ├── ApiClient.kt             # Retrofit client factory
│   │       └── TokenManager.kt          # JWT token storage
│   └── res/
│       ├── layout/
│       │   └── activity_login.xml       # Login screen layout
│       ├── values/
│       │   ├── strings.xml              # String resources
│       │   ├── colors.xml               # Color palette
│       │   └── themes.xml               # Material theme
│       └── drawable/                     # Vector icons
└── README.md
```

## Architecture

### MVVM Pattern

The app follows the **Model-View-ViewModel (MVVM)** architectural pattern:

- **Model**: Data models and repositories (`data/` package)
- **View**: Activities and XML layouts (`ui/` package)
- **ViewModel**: UI state management and business logic (`ui/viewmodel/` package)

### Authentication Flow

The authentication flow mirrors the web application:

1. **User Input**: User enters username, password, and optionally checks "Remember Me"
2. **Login Request**: POST to `/api/auth/login` with credentials
3. **Token Response**: Backend returns JWT tokens (accessToken, refreshToken, tokenType, expiresIn)
4. **Token Storage**: Tokens stored securely using EncryptedSharedPreferences
5. **Account Fetch**: GET `/api/account` with Bearer token to fetch user details
6. **Navigation**: Navigate to MainActivity on successful authentication

### JWT Token Management

- **Storage**: EncryptedSharedPreferences for secure local storage
- **Expiration**: Tokens expire after 8 hours (28800 seconds)
- **Auto-Login**: Saved credentials used for auto-login if "Remember Me" was checked
- **Authorization Header**: `Authorization: Bearer {accessToken}` added to all API requests

## Setup & Configuration

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 8 or later
- Android SDK 21-34
- Running Pharma Smart Spring Boot backend

### Backend Configuration

Update the base URL in `build.gradle`:

```gradle
buildConfigField "String", "BASE_URL", "\"http://YOUR_SERVER_IP:8080/\""
```

**Important**:
- Use your machine's local IP address (not `localhost`) for device testing
- Ensure backend is accessible from the Android device/emulator
- Backend must be running on the specified port (default: 8080)

### Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install and run on connected device
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

## Features

### Authentication

- ✅ Username/Password login
- ✅ JWT token authentication
- ✅ Remember me functionality
- ✅ Auto-fill saved credentials
- ✅ Auto-login for authenticated users
- ✅ Secure token storage (encrypted)
- ✅ Error handling with user feedback

### Security

- **Encrypted Storage**: All credentials and tokens stored using EncryptedSharedPreferences
- **TLS Support**: HTTPS connections supported
- **Token Expiration**: Automatic token expiration handling
- **Secure Headers**: Authorization header automatically added to API requests

## API Endpoints

### Authentication

- **POST** `/api/auth/login` - Login with username/password
  - Request: `{ "username": "...", "password": "..." }`
  - Response: `{ "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 28800 }`

- **GET** `/api/account` - Get current user account details
  - Headers: `Authorization: Bearer {accessToken}`
  - Response: `{ "id": 1, "login": "...", "firstName": "...", "lastName": "...", "authorities": [...] }`

- **POST** `/api/auth/refresh` - Refresh access token
  - Request: `{ "refreshToken": "..." }`
  - Response: `{ "accessToken": "...", ... }`

- **POST** `/api/logout` - Logout (optional, JWT is stateless)

## Development

### Adding New Features

1. **Create Model**: Add data class in `data/model/`
2. **Create API Service**: Add Retrofit interface in `data/api/`
3. **Create Repository**: Add repository in `data/repository/`
4. **Create ViewModel**: Add ViewModel in `ui/viewmodel/`
5. **Create UI**: Add Activity/Fragment in `ui/activity/` or `ui/fragment/`
6. **Create Layout**: Add XML layout in `res/layout/`

### Code Style

- Use Kotlin coding conventions
- Follow MVVM architecture pattern
- Use LiveData for UI state management
- Use Coroutines for asynchronous operations
- Use ViewBinding for type-safe view access

### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Check backend is running
   - Verify BASE_URL in build.gradle
   - Use local IP, not localhost for device testing

2. **401 Unauthorized**
   - Token may be expired
   - Try logging out and logging in again
   - Check token storage in TokenManager

3. **Build Errors**
   - Run `./gradlew clean`
   - Sync Gradle files
   - Invalidate caches and restart Android Studio

### Debugging

- Enable logging in `ApiClient` (already enabled in debug builds)
- Check Logcat for API requests/responses
- Use Android Studio debugger with breakpoints
- Check backend logs for authentication errors

## License

Proprietary - Pharma Smart Warehouse Management System

## Support

For issues or questions, contact the development team or refer to the main project documentation in the parent directory.
