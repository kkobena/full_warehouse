#!/bin/bash

# Flutter Mobile App Setup Script
# This script initializes the Flutter mobile app for Pharma-Smart

echo "========================================="
echo "Pharma-Smart Flutter Mobile App Setup"
echo "========================================="
echo ""

# Check if Flutter is installed
if ! command -v flutter &> /dev/null
then
    echo "❌ Flutter is not installed!"
    echo "Please install Flutter from: https://docs.flutter.dev/get-started/install"
    exit 1
fi

echo "✅ Flutter is installed"
flutter --version
echo ""

# Create Flutter app
echo "📱 Creating Flutter mobile app..."
flutter create mobile --org com.kobe.warehouse --project-name pharma_smart_mobile

# Navigate to mobile directory
cd mobile || exit

echo ""
echo "📦 Installing dependencies..."

# Update pubspec.yaml with required dependencies
cat > pubspec.yaml << 'EOF'
name: pharma_smart_mobile
description: Pharma-Smart Warehouse Management Mobile App
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter

  # HTTP & Networking
  http: ^1.2.0
  dio: ^5.4.0

  # State Management
  flutter_bloc: ^8.1.3
  provider: ^6.1.1

  # Storage
  shared_preferences: ^2.2.2
  hive: ^2.2.3
  hive_flutter: ^1.1.0

  # Security & Auth
  flutter_secure_storage: ^9.0.0
  jwt_decoder: ^2.0.1

  # UI Components
  cupertino_icons: ^1.0.6
  intl: ^0.19.0

  # Barcode Scanning
  mobile_scanner: ^3.5.5

  # PDF Generation
  pdf: ^3.10.7
  printing: ^5.11.1

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0
  build_runner: ^2.4.7
  hive_generator: ^2.0.1

flutter:
  uses-material-design: true
EOF

# Install dependencies
flutter pub get

echo ""
echo "📁 Creating project structure..."

# Create directory structure
mkdir -p lib/core/config
mkdir -p lib/core/network
mkdir -p lib/core/auth
mkdir -p lib/models
mkdir -p lib/services
mkdir -p lib/screens
mkdir -p lib/widgets
mkdir -p lib/utils

echo ""
echo "✅ Flutter mobile app setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Review FLUTTER_INTEGRATION.md for detailed setup instructions"
echo "2. cd mobile"
echo "3. flutter run (to test on emulator/device)"
echo "4. Start developing your mobile screens"
echo ""
echo "🚀 Happy coding!"
