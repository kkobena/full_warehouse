# Sunmi Printer Simulation Guide

## Overview

This project supports both **real Sunmi printers** (on Sunmi devices) and **mock printers** (for development/testing on non-Sunmi devices).

## How It Works

The app automatically detects the device type:
- **On Sunmi devices** (V2, T2, etc.): Uses real `SunmiPrinterService` with hardware printer
- **On other devices** (emulators, regular phones): Uses `MockSunmiPrinterService` which logs to console

## Mock Printer Features

The mock printer simulates all printing operations and:

1. **Logs to Android Logcat** - All print commands are logged with tag `MockSunmiPrinter`
2. **Shows Toast notifications** - Confirms connection and print completion
3. **Builds receipt in memory** - Complete receipt text is available for debugging
4. **Simulates all printer functions**:
   - Text printing (with font sizes, bold, alignment)
   - Line separators
   - QR codes
   - Images/bitmaps
   - Paper feed and cut

## Testing on Emulator/Non-Sunmi Device

### 1. Install and Run the App

```bash
./gradlew installDebug
```

### 2. Perform a Sale

1. Open the app and login
2. Add products to cart
3. Click "Checkout" (Payer)
4. Select payment mode
5. Click "Valider"
6. When asked "Souhaitez-vous imprimer le reçu?", click **Oui**

### 3. View the Mock Receipt

**Option A: Android Studio Logcat**
1. Open Logcat in Android Studio
2. Filter by tag: `MockSunmiPrinter`
3. You'll see the complete receipt with all formatting:

```
========== MOCK RECEIPT START ==========
[CENTER] [BOLD] [XLARGE] Pharma Smart

[CENTER] Adresse de la pharmacie
[CENTER] TEL: +XXX XXX XXX

[CENTER] Bienvenue dans notre pharmacie

------------------------------------------------
TICKET:              VNO-2024-001
CLIENT:              Client comptant
CAISSIER:            John Doe
DATE:                15/01/2024 14:30:00
------------------------------------------------
Product Name         Qty    Total
------------------------------------------------
Paracétamol 500mg    2      1 000 FCFA
Amoxicilline 1g      1      2 500 FCFA
------------------------------------------------
MONTANT TTC:                3 500 FCFA
REMISE:                     0 FCFA
TOTAL A PAYER:              3 500 FCFA

REGLEMENT(S):
Espèces:                    3 500 FCFA
MONNAIE RENDUE:             0 FCFA
------------------------------------------------
Merci pour votre visite!
15/01/2024 14:30:00
========== MOCK RECEIPT END ==========
[CUT PAPER]
```

**Option B: ADB Command Line**
```bash
adb logcat -s MockSunmiPrinter:I
```

**Option C: Toast Messages**
- You'll see toast notifications:
  - "Mock Printer: Connected" when printing starts
  - "Mock Print: Receipt logged to console" when done

## Device Detection

To check what printer service is being used:

```bash
# View device detection
adb logcat -s UnifiedPrinter:D
```

Output will show:
```
D/UnifiedPrinter: Using: Mock Printer
```

Or on a Sunmi device:
```
D/UnifiedPrinter: Using: Real Sunmi Printer
```

## Manual Device Info

You can check device compatibility in your code:

```kotlin
import com.kobe.warehouse.sales.printer.UnifiedPrinterService

// Check if Sunmi device
val isSunmi = UnifiedPrinterService.isSunmiDevice()
Log.d("MyApp", "Is Sunmi device: $isSunmi")

// Get full device info
val printerService = UnifiedPrinterService(context)
val deviceInfo = printerService.getDeviceInfo()
Log.d("MyApp", deviceInfo)
```

## Testing on Real Sunmi Device

### Requirements
1. Sunmi V2, T2, or compatible device
2. Sunmi Inner Printer Service installed (pre-installed on Sunmi devices)
3. Thermal paper loaded in the printer

### Steps
1. Install app: `./gradlew installDebug`
2. Perform a sale as described above
3. Receipt will print on thermal printer

## Troubleshooting

### Mock Printer Not Logging
- Check Logcat filter is set correctly: `MockSunmiPrinter`
- Ensure log level is set to **Debug** or **Verbose**
- Try filtering by package: `com.kobe.warehouse.sales`

### No Toast Messages
- Check if app has toast permission (usually automatic)
- Look in Logcat instead

### Want to Force Real Printer on Non-Sunmi Device
Not recommended, but if you have a USB/Bluetooth Sunmi printer:
1. Modify `UnifiedPrinterService.isSunmiDevice()` to return `true`
2. Rebuild and install app
3. Ensure Sunmi printer drivers are installed

### Want to Force Mock Printer on Sunmi Device
For testing the mock implementation on real Sunmi hardware:
1. In `UnifiedPrinterService.kt`, modify the `useMock` initialization to `val useMock = true`
2. Rebuild and install app

## Code Example

The ReceiptPrinter uses UnifiedPrinterService directly:

```kotlin
// In ReceiptPrinter.kt
class ReceiptPrinter(context: Context) {
    private val printerService = UnifiedPrinterService(context)

    // UnifiedPrinterService automatically detects device type
    // No need to check - it handles both Mock and Real printers
}
```

## Advanced: Custom Mock Behavior

You can customize the mock printer in `MockSunmiPrinterService.kt`:

```kotlin
// Example: Save receipt to file instead of just logging
fun cutPaper() {
    printLog.appendLine("[CUT PAPER]")

    // Save to file
    val file = File(context.filesDir, "last_receipt.txt")
    file.writeText(printLog.toString())

    // Show in a dialog (optional)
    showReceiptDialog(printLog.toString())
}
```

## Comparison: Real vs Mock

| Feature | Real Sunmi Printer | Mock Printer |
|---------|-------------------|--------------|
| Device | Sunmi V2, T2, etc. | Any Android device |
| Output | Thermal paper | Logcat console |
| Speed | ~2-3 seconds | Instant |
| Cost | Thermal paper | Free |
| Visual | Physical receipt | Text in logs |
| QR Codes | Printed | Logged as text |
| Images | Printed | Logged as dimensions |

## Best Practices

1. **Development**: Use mock printer on emulator/regular phone
2. **Testing**: Use mock printer first, then test on real Sunmi device
3. **Production**: Deploy to Sunmi devices with real printers
4. **Debugging**: Check logs even on Sunmi devices for errors

## Next Steps

Once you've verified the mock printer works:
1. Test on actual Sunmi device
2. Adjust receipt formatting if needed
3. Configure paper size in settings (58mm vs 80mm)
4. Test with real customer data
