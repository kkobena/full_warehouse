# Printer Configuration Guide

## Paper Roll Size Configuration

The Android POS application supports two thermal printer paper roll sizes:

### 58mm Paper Roll (Small) - **Default**
- **Width**: 32 characters per line
- **Best for**: Compact receipts, mobile printers
- **Product name**: Limited to 18 characters
- **Label abbreviations**: Used to fit content
  - "TOTAL A PAYER" → "TOTAL"
  - "MONNAIE RENDUE" → "MONNAIE"
  - "RESTE A PAYER" → "RESTE"

### 80mm Paper Roll (Standard)
- **Width**: 48 characters per line
- **Best for**: Standard POS printers, detailed receipts
- **Product name**: Up to 24 characters
- **Full labels**: Complete text displayed

## Configuration

### Option 1: Using SharedPreferences (Programmatic)

Set the paper roll size using SharedPreferences:

```kotlin
val prefs = getSharedPreferences("printer_settings", Context.MODE_PRIVATE)
prefs.edit().putString("paper_roll_size", "58").apply()  // For 58mm (default)
// OR
prefs.edit().putString("paper_roll_size", "80").apply()  // For 80mm
```

### Option 2: Settings Dialog (TODO)

A settings dialog can be added to the app to allow users to configure the paper roll size:

1. Add a "Printer Settings" menu item in MainActivity or Settings screen
2. Display a dialog with radio buttons for 58mm and 80mm options
3. Save selection to SharedPreferences
4. Receipts will automatically use the selected paper size

## Receipt Layout Comparison

### 80mm Receipt Example:
```
===============================================
         PHARMA SMART
    123 Rue de la Pharmacie
       Tél: +221 33 123 45 67
   Bienvenue dans notre pharmacie

-----------------------------------------------
TICKET: VNO-2025-001
CASSIER(RE): Jean Dupont
Client: Marie Martin

-----------------------------------------------
QTE  PRODUIT                PU     MONTANT
-----------------------------------------------
  2  Paracétamol 500mg     500      1 000
  1  Ibuprofène 400mg      750        750
-----------------------------------------------
MONTANT TTC:                         1 750

TOTAL A PAYER:                       1 750

          REGLEMENT(S)

Espèces:                             2 000
MONNAIE RENDUE:                        250
-----------------------------------------------
19/11/2025 12:30:45

     Merci pour votre visite!
```

### 58mm Receipt Example:
```
================================
      PHARMA SMART
 123 Rue de la Pharmacie
    Tél: +221 33 123 45 67
Bienvenue dans notre...

--------------------------------
TICKET: VNO-2025-001
CASSIER(RE): Jean Dupont
Client: Marie Martin

--------------------------------
QTE PRODUIT          PU  MONTANT
--------------------------------
  2 Paracétamol 500 500   1 000
  1 Ibuprofène 400  750     750
--------------------------------
MONTANT TTC:               1 750

TOTAL:                     1 750

        REGLEMENT(S)

Espèces:                   2 000
MONNAIE:                     250
--------------------------------
19/11/2025 12:30:45

   Merci pour votre visite!
```

## Technical Details

### Column Width Allocation

**80mm Paper (48 chars):**
- QTE: 3 chars
- PRODUIT: 24 chars
- PU: 8 chars
- MONTANT: 10 chars
- Spacing: 3 chars

**58mm Paper (32 chars):**
- QTE: 3 chars
- PRODUIT: 18 chars
- PU: 6 chars
- MONTANT: 6 chars
- Spacing: 3 chars (reduced)

### Code Implementation

The `ReceiptPrinter` class automatically adjusts formatting based on the paper roll size:

```kotlin
enum class PaperRoll(val width: Int, val productNameWidth: Int) {
    MM_58(32, 18),  // 58mm paper: 32 chars per line, 18 chars for product name
    MM_80(48, 24)   // 80mm paper: 48 chars per line, 24 chars for product name
}
```

## Default Behavior

- **Default**: 58mm paper roll
- If no preference is set, the system defaults to 58mm
- Setting is persistent across app restarts
- To use 80mm paper, set the preference to "80"

## Testing

To test different paper sizes without changing preferences:

```kotlin
// In ComptantSaleActivity.kt - printReceipt() method
// Temporarily force a paper size for testing:
val paperRoll = ReceiptPrinter.PaperRoll.MM_58  // Test 58mm (default)
// val paperRoll = ReceiptPrinter.PaperRoll.MM_80  // Test 80mm
```

## Supported Printers

This implementation is designed for **Sunmi thermal printers** with the following paper widths:
- 58mm (small format)
- 80mm (standard format)

Other thermal printers using ESC/POS commands may also work with appropriate configuration.

## Troubleshooting

**Receipt is too wide or wrapping incorrectly:**
- Check that the configured paper size matches your actual printer paper
- Use 58mm setting for narrow printers
- Use 80mm setting for standard POS printers

**Text is cut off:**
- Product names are automatically truncated to fit the paper width
- Long store names may wrap on 58mm paper
- Consider abbreviating store information for 58mm receipts

**Alignment issues:**
- The printer service handles alignment automatically
- ESC/POS commands ensure proper formatting
- Test with your specific printer model

---
**Last Updated:** 2025-11-19
