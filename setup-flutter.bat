@echo off
REM Flutter Mobile App Setup Script for Windows
REM This script initializes the Flutter mobile app for Pharma-Smart

echo =========================================
echo Pharma-Smart Flutter Mobile App Setup
echo =========================================
echo.

REM Check if Flutter is installed
where flutter >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo X Flutter is not installed!
    echo Please install Flutter from: https://docs.flutter.dev/get-started/install
    pause
    exit /b 1
)

echo + Flutter is installed
flutter --version
echo.

REM Create Flutter app
echo Creating Flutter mobile app...
flutter create mobile --org com.kobe.warehouse --project-name pharma_smart_mobile

REM Navigate to mobile directory
cd mobile

echo.
echo Installing dependencies...

REM Update pubspec.yaml
echo name: pharma_smart_mobile > pubspec.yaml
echo description: Pharma-Smart Warehouse Management Mobile App >> pubspec.yaml
echo publish_to: 'none' >> pubspec.yaml
echo version: 1.0.0+1 >> pubspec.yaml
echo. >> pubspec.yaml
echo environment: >> pubspec.yaml
echo   sdk: '>=3.0.0 ^<4.0.0' >> pubspec.yaml
echo. >> pubspec.yaml
echo dependencies: >> pubspec.yaml
echo   flutter: >> pubspec.yaml
echo     sdk: flutter >> pubspec.yaml
echo. >> pubspec.yaml
echo   # HTTP ^& Networking >> pubspec.yaml
echo   http: ^1.2.0 >> pubspec.yaml
echo   dio: ^5.4.0 >> pubspec.yaml
echo. >> pubspec.yaml
echo   # State Management >> pubspec.yaml
echo   flutter_bloc: ^8.1.3 >> pubspec.yaml
echo   provider: ^6.1.1 >> pubspec.yaml
echo. >> pubspec.yaml
echo   # Storage >> pubspec.yaml
echo   shared_preferences: ^2.2.2 >> pubspec.yaml
echo   hive: ^2.2.3 >> pubspec.yaml
echo   hive_flutter: ^1.1.0 >> pubspec.yaml
echo. >> pubspec.yaml
echo   # Security ^& Auth >> pubspec.yaml
echo   flutter_secure_storage: ^9.0.0 >> pubspec.yaml
echo   jwt_decoder: ^2.0.1 >> pubspec.yaml
echo. >> pubspec.yaml
echo   # UI Components >> pubspec.yaml
echo   cupertino_icons: ^1.0.6 >> pubspec.yaml
echo   intl: ^0.19.0 >> pubspec.yaml
echo. >> pubspec.yaml
echo   # Barcode Scanning >> pubspec.yaml
echo   mobile_scanner: ^3.5.5 >> pubspec.yaml
echo. >> pubspec.yaml
echo   # PDF Generation >> pubspec.yaml
echo   pdf: ^3.10.7 >> pubspec.yaml
echo   printing: ^5.11.1 >> pubspec.yaml
echo. >> pubspec.yaml
echo dev_dependencies: >> pubspec.yaml
echo   flutter_test: >> pubspec.yaml
echo     sdk: flutter >> pubspec.yaml
echo   flutter_lints: ^3.0.0 >> pubspec.yaml
echo   build_runner: ^2.4.7 >> pubspec.yaml
echo   hive_generator: ^2.0.1 >> pubspec.yaml
echo. >> pubspec.yaml
echo flutter: >> pubspec.yaml
echo   uses-material-design: true >> pubspec.yaml

REM Install dependencies
flutter pub get

echo.
echo Creating project structure...

REM Create directory structure
if not exist lib\core\config mkdir lib\core\config
if not exist lib\core\network mkdir lib\core\network
if not exist lib\core\auth mkdir lib\core\auth
if not exist lib\models mkdir lib\models
if not exist lib\services mkdir lib\services
if not exist lib\screens mkdir lib\screens
if not exist lib\widgets mkdir lib\widgets
if not exist lib\utils mkdir lib\utils

echo.
echo + Flutter mobile app setup complete!
echo.
echo Next steps:
echo 1. Review FLUTTER_INTEGRATION.md for detailed setup instructions
echo 2. cd mobile
echo 3. flutter run (to test on emulator/device)
echo 4. Start developing your mobile screens
echo.
echo Happy coding!
echo.
pause
