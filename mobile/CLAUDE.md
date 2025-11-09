# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **mobile Flutter application** for Pharma-Smart, a pharmacy warehouse management system. It provides mobile access to dashboard analytics, inventory management, stock viewing, cash register recaps, and VAT reporting. The app connects to the Spring Boot backend API (documented in the parent directory's CLAUDE.md) and supports role-based UI (admin vs user roles).

The mobile app is part of a larger monorepo that includes:
- **Backend**: Spring Boot Java application (parent directory: `../src/main/java/`)
- **Web Frontend**: Angular 20 application (parent directory: `../src/main/webapp/`)
- **Desktop**: Tauri wrapper (parent directory: `../src-tauri/`)
- **Mobile**: This Flutter application

## Technology Stack

**Flutter & Dart:**
- **Dart SDK**: ^3.8.1
- **Flutter**: Latest stable version (check with `flutter --version`)
- **State Management**: Provider 6.1.2 (ChangeNotifier pattern)
- **Local Storage**: Hive 2.2.3 with Hive Flutter 1.1.0
- **Networking**: http 1.4.0 package
- **Code Generation**: json_serializable 6.9.0, hive_generator 2.0.0, build_runner 2.5.3
- **Charts**: fl_chart 1.0.0 for data visualization
- **Internationalization**: intl 0.20.2 (French locale: fr_FR)

**Platforms Supported:**
- Android (primary target)
- iOS
- Web
- Windows
- Linux
- macOS

## Build & Development Commands

### Setup & Installation

```bash
# Install Flutter dependencies
flutter pub get

# Generate code for models (*.g.dart files from json_serializable & hive_generator)
flutter pub run build_runner build

# Generate code with watch mode (auto-regenerates on file changes)
flutter pub run build_runner watch

# Clean generated files and regenerate
flutter pub run build_runner build --delete-conflicting-outputs
```

### Running the App

```bash
# Run on connected device/emulator (debug mode)
flutter run

# Run on specific device
flutter devices                    # List available devices
flutter run -d <device_id>         # Run on specific device
flutter run -d chrome              # Run in Chrome browser
flutter run -d windows             # Run on Windows desktop

# Run with hot reload enabled (default in debug)
flutter run

# Run in release mode (optimized, no debug features)
flutter run --release

# Run in profile mode (for performance profiling)
flutter run --profile
```

### Building

```bash
# Build APK for Android (debug)
flutter build apk

# Build APK for Android (release)
flutter build apk --release

# Build App Bundle for Android (for Google Play)
flutter build appbundle --release

# Build for iOS (requires macOS with Xcode)
flutter build ios --release

# Build for web
flutter build web

# Build for Windows desktop
flutter build windows

# Build for Linux desktop
flutter build linux

# Build for macOS desktop
flutter build macos
```

### Testing & Analysis

```bash
# Run tests
flutter test

# Run tests with coverage
flutter test --coverage

# Analyze code for issues
flutter analyze

# Format code
flutter format .
flutter format lib/
```

### Cleaning

```bash
# Clean build artifacts
flutter clean

# After clean, reinstall dependencies
flutter pub get
```

## Project Architecture

### Directory Structure

```
lib/
├── main.dart                    # App entry point, Provider setup, routing
├── src/
    ├── data/                    # Data layer (services, repositories, API clients)
    │   ├── auth/               # Authentication logic
    │   │   ├── model/          # User model
    │   │   └── servie/         # AuthService (typo in folder name)
    │   └── services/           # Business logic services
    │       ├── balance/        # Balance/sales service
    │       ├── dahboard/       # Dashboard service (typo in folder name)
    │       ├── inventaire/     # Inventory services
    │       ├── produit/        # Product service
    │       ├── recap_caisse/   # Cash register recap service
    │       ├── tva/            # VAT/Tax service
    │       └── utils/          # API client (HTTP + auth)
    ├── models/                 # Data models (JSON serializable)
    │   ├── caisse/            # Cash register models
    │   ├── inventaire/        # Inventory models (Hive + JSON)
    │   ├── produit/           # Product models
    │   └── *.dart             # Shared models (Balance, Dashboard, TVA)
    ├── ui/                    # Presentation layer (pages & widgets)
    │   ├── auth/             # Login/authentication UI
    │   ├── balance/          # Sales balance page
    │   ├── caisse/           # Cash register recap page
    │   ├── commande/         # Order management page
    │   ├── home/             # Dashboard page
    │   ├── inventory/        # Inventory management page
    │   ├── stock/            # Stock viewing & search pages
    │   └── tvas/             # VAT report page
    └── utils/                # Utilities, shared widgets, theme
        ├── api_client.dart   # Singleton for API URL & credentials (Hive)
        ├── app_drawer.dart   # Navigation drawer (admin only)
        ├── app_theme.dart    # Theme definitions
        ├── constant.dart     # App constants (strings, roles)
        ├── custom_app_bar.dart
        ├── date_range_state.dart  # Date range provider
        ├── profile_page_router.dart  # Routing based on auth state
        ├── service/          # Shared service utilities
        └── theme_provider.dart  # Theme switching provider

assets/
└── images/                  # Image assets

test/
└── widget_test.dart         # Sample widget test
```

### Key Architectural Patterns

**State Management (Provider Pattern):**
- All services extend `ChangeNotifier` from the Provider package
- Services are registered in `main.dart` using `MultiProvider`
- UI widgets use `Provider.of<T>(context)` or `context.watch<T>()` to access services
- Services call `notifyListeners()` to trigger UI updates

**Data Layer Architecture:**
- `ApiClient` (singleton): Manages base URLs, credentials, HTTP headers, Basic Auth
- Services: Fetch data from backend API, manage loading/error states, notify listeners
- `SharedService`: Generic HTTP service used by other services for common operations
- Models use `json_serializable` for JSON parsing (`*.g.dart` files)

**Local Storage (Hive):**
- Used for settings persistence: API URL, username, password, theme, rememberMe flag
- Inventory models (`Rayon`, `Inventaire`, `InventaireItem`) use Hive adapters for offline storage
- Settings box opened in `main.dart`: `await Hive.openBox('settings')`

**Code Generation:**
- Models use `@JsonSerializable()` annotation → generates `fromJson`/`toJson` methods
- Hive models use `@HiveType()` and `@HiveField()` → generates TypeAdapters
- Run `flutter pub run build_runner build` after modifying models

**Role-Based UI:**
- Two roles: `Constant.profilAdmin` (admin) and `Constant.profilUser` (user)
- Admin role: Access to Dashboard, TVA, Balance, Recap Caisse pages + drawer navigation
- User role: Access only to Inventory page
- UI configured dynamically in `MyHomePage._setupRoleBasedUI()` based on `AuthService.currentUserRole`

**Authentication Flow:**
- **JWT (JSON Web Token) authentication** with Bearer tokens
- Login process:
  1. POST credentials to `/api/auth/login`
  2. Receive JWT tokens (accessToken, refreshToken) and expiration time
  3. Tokens stored in Hive by `ApiClient`
  4. Fetch user details from `/api/account` using Bearer token
  5. User data and credentials saved for auto-login
- `AuthService` manages login, logout, auto-login, and user state
- `ProfilePageRouter`: Routes user to login or home based on `AuthService.isAuthenticated`
- Auto-login checks token validity first, re-authenticates if expired
- Token expiration tracked in milliseconds since epoch

**Routing:**
- Named routes defined in `main.dart` routes map
- `ProfilePageRouter` is the initial route (`/`) that redirects based on auth state
- Pages expose static `routeName` constant (e.g., `MyHomePage.routeName`)

**Theming:**
- `ThemeProvider` extends `ChangeNotifier` to manage app theme
- Multiple themes available (defined in `AppThemes` enum)
- Theme preference saved to Hive via `ApiClient.updateTheme()`
- French locale hardcoded: `locale: Locale('fr')`

### Backend API Integration

**Base URL Configuration:**
- API URL: `http://{IP}:{PORT}/api` (for authentication and account endpoints)
- Java Client URL: `http://{IP}:{PORT}/java-client` (for mobile data endpoints)
- Auth URL: `http://{IP}:{PORT}/api/auth/login` (JWT authentication endpoint)
- Configured via `ApiClient.saveApiUrl(String apiUrl)` and stored in Hive

**Authentication:**
- **Uses JWT (JSON Web Token) authentication with Bearer tokens**
- Login endpoint: `POST /api/auth/login`
  - Request body: `{"username": "...", "password": "..."}`
  - Response: `{"accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 28800}`
- User details endpoint: `GET /api/account` (requires Bearer token)
- Access token included in all requests via `Authorization: Bearer {token}` header
- Token expiration: 8 hours (28800 seconds) by default
- Tokens stored securely in Hive local storage
- `ApiClient.headers` automatically includes Bearer token when available
- Legacy Basic Auth marked as deprecated but kept for backward compatibility

**API Endpoint Pattern:**
- Auth endpoints: `/api/auth/*` (JWT authentication)
- Account endpoints: `/api/account` (user details)
- Mobile data endpoints: `/java-client/mobile/{resource}/{action}`
- Example: `/java-client/mobile/dashboard/data?fromDate=...&toDate=...`
- Services use `SharedService.fetchData<T>()` which automatically selects correct base URL

**Backend Code Location:**
- Backend Java code: `../src/main/java/com/kobe/warehouse/`
- Mobile-specific REST endpoints: `../src/main/java/com/kobe/warehouse/web/rest/` (look for mobile controllers)
- See parent directory's `CLAUDE.md` for full backend architecture

## Development Workflows

### Adding a New Feature/Page

1. **Create Model** (if needed):
   - Add model class in `lib/src/models/` with `@JsonSerializable()` annotation
   - Include `part 'model_name.g.dart';` directive
   - Implement `fromJson` factory and `toJson` method (delegates to generated code)
   - Run `flutter pub run build_runner build` to generate `*.g.dart` file

2. **Create Service**:
   - Add service in `lib/src/data/services/{feature}/`
   - Extend `ChangeNotifier`
   - Use `SharedService.fetchData<T>()` or implement custom HTTP logic
   - Expose getters for state (`isLoading`, `errorMessage`, data)
   - Call `notifyListeners()` after state changes

3. **Register Provider**:
   - Add service to `MultiProvider` in `main.dart`
   - Example: `ChangeNotifierProvider(create: (_) => NewFeatureService())`

4. **Create UI**:
   - Add page widget in `lib/src/ui/{feature}/`
   - Use `Provider.of<T>(context)` or `context.watch<T>()` to access service
   - Implement loading, error, and data states
   - Add `static const String routeName = '/feature'` to page widget

5. **Add Route**:
   - Register route in `main.dart` routes map
   - Example: `'/feature': (context) => FeaturePage()`

6. **Update Navigation** (if admin feature):
   - Add navigation option to `AppDrawer` widget (`lib/src/utils/app_drawer.dart`)

### Working with Code Generation

**When to Regenerate:**
- After adding/modifying models with `@JsonSerializable()` or `@HiveType()`
- After adding/removing fields in existing models
- When encountering "undefined_identifier" errors for generated code

**Commands:**
```bash
# One-time generation
flutter pub run build_runner build

# Watch mode (auto-regenerates on save)
flutter pub run build_runner watch

# Force regeneration (resolves conflicts)
flutter pub run build_runner build --delete-conflicting-outputs
```

**Generated Files:**
- `*.g.dart` files are git-ignored (should be in `.gitignore`)
- Do NOT manually edit `*.g.dart` files
- Always regenerate after pulling changes that modify models

### Connecting to Backend

**Development Setup:**
1. Start the Spring Boot backend server (see parent directory's CLAUDE.md)
   - Backend runs on port 8080 by default
   - Use `mvnw.cmd` (Windows) or `./mvnw` (Linux/macOS)

2. Configure API URL in mobile app:
   - On first launch, app prompts for server IP/URL
   - Or modify `ApiClient` to set default URL during development
   - Example: `http://192.168.1.100:8080` (use machine's local IP, not localhost if testing on device)

3. Ensure backend `/api-user-account` endpoint is accessible (mobile auth endpoint)

**Testing API Changes:**
- Backend changes require backend restart
- Mobile app uses hot reload (no restart needed)
- Check `ApiClient.apiUrl` and `ApiClient.authUrl` for endpoint configuration

### Handling Locale/Internationalization

**Current Configuration:**
- App is French-only: `locale: Locale('fr')`
- Date formatting uses `fr_FR` locale (initialized in `main.dart`)
- All UI strings are hardcoded in `Constant` class (`lib/src/utils/constant.dart`)

**To Add Internationalization:**
1. Install `flutter_localizations` package (already included)
2. Use `intl` package for string externalization
3. Generate ARB files for translations
4. Update `Constant` strings to use translated strings

### Testing

**Widget Tests:**
- Sample test in `test/widget_test.dart`
- Use `flutter test` to run tests
- Mock providers using `Provider.value()` or test-specific providers

**Integration Testing:**
- Requires backend server running
- Test authentication flow, API calls, navigation

**Manual Testing:**
- Use Flutter DevTools for debugging: `flutter pub global activate devtools`
- Enable debug mode: `flutter run` (default)
- Use hot reload (press `r` in terminal) for quick UI changes
- Use hot restart (press `R` in terminal) for state reset

## Important Notes

### Backend Dependency

This mobile app is NOT standalone. It requires the Spring Boot backend to be running. The backend provides:
- JWT authentication via `/api/auth/login`
- User account details via `/api/account`
- All business logic and data via REST APIs under `/java-client/mobile/*`
- Database access (PostgreSQL)

See parent directory's `CLAUDE.md` for backend setup instructions.

### Configuration Storage

All configuration is stored in Hive box (`settings`):
- API URLs (`apiUrl`, `auth`, `javaClient`, `appIp`)
- User credentials (`username`, `password`) - used for auto-login
- JWT tokens (`accessToken`, `refreshToken`, `tokenExpiration`)
- Remember me flag (`rememberMe`)
- Current user data (`user`)
- Theme preference (`theme`)

**Security Note:**
- JWT tokens are stored in Hive (local storage) - this is secure on mobile devices
- Credentials are stored for auto-login convenience
- Token expiration is tracked and validated before use
- For enhanced security, consider:
  - Using Flutter Secure Storage for sensitive data
  - Implementing token refresh mechanism (backend endpoint exists but not yet implemented)
  - Clearing tokens on logout or when expired

### Known Folder Naming Issues

- `lib/src/data/auth/servie/` should be `service` (typo)
- `lib/src/data/services/dahboard/` should be `dashboard` (typo)

These are existing typos in the codebase. Be aware when navigating the directory structure.

### Linting Configuration

The project has relaxed some Flutter lints in `analysis_options.yaml`:
- `prefer_const_constructors: false`
- `prefer_final_fields: false`
- `use_key_in_widget_constructors: false`
- `prefer_const_literals_to_create_immutables: false`
- `prefer_const_constructors_in_immutables: false`
- `avoid_print: false`

When writing code, follow these relaxed conventions unless refactoring.

### Hot Reload Limitations

Hot reload does NOT work for:
- Changes to `main.dart` (especially Provider registration)
- Changes to generated `*.g.dart` files (requires regeneration + restart)
- Changes to Hive adapters
- Changes to app initialization logic

Use hot restart (`R`) or full app restart for these cases.

### Platform-Specific Considerations

**Android:**
- Minimum SDK version defined in `android/app/build.gradle`
- Permissions configured in `android/app/src/main/AndroidManifest.xml`

**iOS:**
- Requires macOS with Xcode for building
- Permissions configured in `ios/Runner/Info.plist`

**Web:**
- CORS must be configured on backend for web builds
- Backend runs on different origin (localhost:8080 vs localhost:4200 or localhost:8080 vs localhost:{flutter_web_port})

**Desktop (Windows/Linux/macOS):**
- Network access works without additional configuration
- Can connect to localhost backend directly

## Code Style & Conventions

**Dart Conventions:**
- Use `lowerCamelCase` for variables, methods, parameters
- Use `UpperCamelCase` for classes, enums, typedefs
- Use `lowercase_with_underscores` for file names
- Prefer `final` for local variables that won't change (though `prefer_final_fields` is disabled)
- Use trailing commas for multi-line parameter lists (helps with formatting)

**Provider Pattern:**
- Always call `notifyListeners()` after updating state in a service
- Use `listen: false` when accessing provider in callbacks or one-time reads
- Use `context.watch<T>()` for automatic rebuilds on state changes
- Use `Provider.of<T>(context, listen: false)` for reading without listening

**Model Conventions:**
- All models use nullable fields (e.g., `List<ListItem>?`)
- Models include both `fromJson` and `toJson` methods
- Use `part` directive for generated code: `part 'model_name.g.dart';`

**Service Conventions:**
- Services have private fields prefixed with `_` (e.g., `_isLoading`, `_dashboard`)
- Public getters expose private state
- Services use `SharedService` for common HTTP operations
- Error messages stored as strings, not exceptions

**UI Conventions:**
- Pages are StatefulWidget with `routeName` constant
- Use `IndexedStack` for bottom navigation (preserves state across tabs)
- Admin UI includes drawer, user UI does not
- Loading states show `CircularProgressIndicator`
- Empty states show centered text messages

## Debugging

**Common Issues:**

1. **"No such method: _$ModelNameFromJson"**
   - Missing code generation
   - Fix: Run `flutter pub run build_runner build`

2. **"Cannot open Hive box"**
   - Hive not initialized or box not opened
   - Fix: Check `main.dart` for `Hive.initFlutter()` and `Hive.openBox('settings')`

3. **"Connection refused" errors**
   - Backend not running or wrong API URL
   - Fix: Check `ApiClient.apiUrl` and ensure backend is accessible
   - For device testing: Use machine's local IP, not `localhost`

4. **Provider not found errors**
   - Service not registered in `MultiProvider`
   - Fix: Add service to providers list in `main.dart`

5. **Hot reload not working**
   - Changed `main.dart` or generated files
   - Fix: Use hot restart (`R`) or full restart

6. **401 Unauthorized errors after login**
   - JWT token not included in request or expired
   - Fix: Check `ApiClient.accessToken` is not null and `ApiClient.isTokenExpired` is false
   - Check `ApiClient.headers` includes `Authorization: Bearer {token}`
   - Try logging out and logging in again to get fresh token

7. **Auto-login not working**
   - Token expired or invalid
   - Fix: Check token expiration in Hive, credentials should trigger re-authentication
   - Clear app data to reset: Delete Hive database and restart app

**Debugging Tools:**
- `print()` statements (allowed by linter)
- Flutter DevTools: `flutter pub global activate devtools && flutter pub global run devtools`
- VS Code/Android Studio debugger with breakpoints
- `flutter logs` to view device logs

**Backend Debugging:**
- Check backend console for API errors
- Enable SQL logging in backend: `spring.jpa.show-sql=true`
- Use Postman/curl to test API endpoints directly
- See parent directory's `CLAUDE.md` for backend debugging
