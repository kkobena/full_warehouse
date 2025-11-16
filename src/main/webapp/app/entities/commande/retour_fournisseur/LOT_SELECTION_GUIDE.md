# Lot Selection Implementation Guide

## Overview

This implementation provides **two flexible workflows** for selecting lot numbers and quantities when processing supplier returns (`retour fournisseur`). The system automatically detects when order items have associated lots and presents appropriate selection interfaces.

## Features

### 🎯 Dual Mode Support

1. **Dialog Mode (Modal)**: Traditional popup dialog for lot selection
2. **Inline Mode**: Expandable card-based selection directly in the page

### 🔄 Easy Mode Switching

A toggle button in the header allows instant switching between modes:

- **Dialog Mode**: 🪟 Shows "Mode: Dialog" with window icon
- **Inline Mode**: 📋 Shows "Mode: Inline" with list icon

### ✨ Key Features

- **Automatic Lot Detection**: System detects when `AbstractOrderItem.lots[]` is populated
- **FEFO Auto-Distribution**: First Expired First Out automatic quantity allocation
- **Real-time Validation**: Ensures total selected quantity matches requested quantity
- **Visual Feedback**: Clear indicators for lots, quantities, and selection status
- **Lot Tracking**: Each return item is linked to specific lot ID and lot number
- **Multi-Lot Support**: Can return from multiple lots in a single operation

---

## Architecture

### Component Files

```
retour_fournisseur/
├── supplier-returns.component.ts          # Main component with both modes
├── supplier-returns.component.html         # Template with toggle & both UIs
├── supplier-returns.component.scss         # Styling for both modes
├── lot-selection-dialog.component.ts       # Modal dialog component (NgbModal)
├── inline-lot-selection.component.ts       # Inline expandable component
└── LOT_SELECTION_GUIDE.md                 # This documentation
```

### Data Flow

```
User selects order line with lots
         ↓
System stores temporary values (quantity, order line, motif)
         ↓
     ┌───────────────┐
     │  Mode Check   │
     └───────┬───────┘
             │
     ┌───────┴────────┐
     │                │
Dialog Mode      Inline Mode
     │                │
Open NgbModal    Show Inline Card
     │                │
     └────────┬───────┘
              │
    User selects lots & quantities
              │
    onLotsConfirmed(selections)
              │
    Create return items (one per lot)
              │
    Display in return lines table
```

---

## Mode 1: Dialog Mode (Modal)

### User Experience

1. Select order line with lots
2. Enter quantity to return
3. Select return reason (motif)
4. Click "Add" button
5. **Modal dialog appears** with:
   - Product information header
   - Table of available lots
   - Quantity inputs for each lot
   - Real-time total validation
   - Color-coded feedback (red/green)

### Technical Implementation

**Component**: `lot-selection-dialog.component.ts`

```typescript
// Opens via NgbModal service
const modalRef = this.modalService.open(LotSelectionDialogComponent, {
  size: 'lg',
  backdrop: 'static',
  centered: true,
});

// Pass data to modal
modalRef.componentInstance.lots = orderLine.lots;
modalRef.componentInstance.requestedQuantity = quantity;
modalRef.componentInstance.productLabel = orderLine.produitLibelle;

// Handle result
modalRef.result.then(
  (selectedLots: LotSelection[]) => this.onLotsConfirmed(selectedLots),
  () => this.onLotSelectionCancelled(),
);
```

### Features

- ✅ **Auto-fill**: Uses FEFO algorithm to pre-fill quantities
- ✅ **Validation**: Cannot confirm unless total = requested quantity
- ✅ **Clean UI**: Focused modal without distractions
- ✅ **Keyboard Support**: Tab navigation, Enter to confirm

### When to Use

- Simple, quick lot selection
- Users prefer focused, distraction-free interface
- Mobile/tablet devices (modal works better on small screens)
- Users are familiar with modal dialogs

---

## Mode 2: Inline Mode

### User Experience

1. Select order line with lots
2. Enter quantity to return
3. Select return reason (motif)
4. Click "Add" button
5. **Expandable card appears below** with:
   - Collapsible header showing lot count
   - Grid of lot cards
   - Visual lot information (expiry date, available quantity)
   - Increment/decrement buttons
   - "Auto-Distribute (FEFO)" button
   - Confirm/Cancel buttons

### Technical Implementation

**Component**: `inline-lot-selection.component.ts`

```typescript
// Show inline selection
this.showInlineLotSelection.set(true);

// Template renders component
<jhi-inline-lot-selection
  [lots]="tempSelectedOrderLine()?.lots || []"
  [requestedQuantity]="tempReturnQuantity()"
  [productLabel]="tempSelectedOrderLine()?.produitLibelle || ''"
  [expanded]="true"
  (lotsConfirmed)="onLotsConfirmed($event)"
  (cancelled)="onLotSelectionCancelled()"
/>
```

### Features

- ✅ **Visual Lot Cards**: Each lot displayed as a card with full details
- ✅ **Expiry Date Display**: See expiry dates at a glance
- ✅ **Quick Controls**: +/- buttons for easy quantity adjustment
- ✅ **One-Click Fill**: "Select All" button per lot
- ✅ **Auto-Distribute**: FEFO button for automatic quantity allocation
- ✅ **Collapsible**: Can minimize when not actively selecting

### Card Layout

Each lot card shows:

- 🏷️ Lot number (badge)
- 📅 Expiry date
- 📦 Available quantity
- ➕➖ Increment/decrement buttons
- Input field for manual entry
- "Select All" button
- "Clear" button

### When to Use

- Complex scenarios with many lots
- Users need to see all lot details at once
- Desktop environments with larger screens
- Users prefer context without losing page view
- Reviewing multiple lots before confirming

---

## Usage Workflow

### Scenario: Return with Lots

**Example**: Return 100 units of "DOLIPRANE 1000MG" from order #CMD-2024-001

#### Step 1: Search & Select Order

```
1. Search for order: "CMD-2024-001"
2. Select from dropdown
3. Order meta information displays
```

#### Step 2: Select Product Line

```
1. Search for product: "DOLIPRANE"
2. Select from order lines dropdown
3. System shows:
   - CIP code
   - Available quantity
   - 🏷️ "3 lot(s) disponible(s)" (if lots exist)
```

#### Step 3: Specify Return Details

```
1. Enter quantity: 100
2. Select motif: "Produit périmé"
```

#### Step 4: Lot Selection (Auto-triggered)

##### Dialog Mode:

```
1. Modal opens automatically
2. Shows 3 lots:
   - LOT-2024-001: Exp 12/2024, Available: 50
   - LOT-2024-002: Exp 01/2025, Available: 75
   - LOT-2024-003: Exp 03/2025, Available: 100

3. System auto-fills (FEFO):
   - LOT-2024-001: 50 (oldest expiry)
   - LOT-2024-002: 50
   - LOT-2024-003: 0

4. User can adjust as needed
5. Click "Confirmer"
```

##### Inline Mode:

```
1. Expandable card appears below
2. Shows grid of 3 lot cards
3. User clicks "Auto-répartir (FEFO)"
4. Same auto-fill logic applies
5. User can use +/- buttons or type manually
6. Click "Confirmer la sélection"
```

#### Step 5: Review Return Lines

Table shows **2 rows** (one per lot):

```
CIP         | Produit              | N° Lot       | Qty | Motif
------------|----------------------|--------------|-----|------------------
3400123456  | DOLIPRANE 1000MG     | LOT-2024-001 | 50  | Produit périmé
3400123456  | DOLIPRANE 1000MG     | LOT-2024-002 | 50  | Produit périmé
```

---

## Data Model

### IRetourBonItem (Enhanced)

```typescript
export interface IRetourBonItem {
  id?: number;
  // ... existing fields ...
  lotId?: number; // ← Added: References lot.id
  lotNumero?: string; // ← Added: Lot number for display
  qtyMvt?: number; // Quantity being returned from this lot
  // ...
}
```

### AbstractOrderItem

```typescript
export interface AbstractOrderItem {
  id?: number;
  lots?: ILot[]; // ← Array of available lots
  quantityReceived?: number;
  // ...
}
```

### ILot

```typescript
export interface ILot {
  id?: number;
  numLot?: string; // Lot number
  quantity?: number; // Available quantity
  expiryDate?: string; // Expiration date
  freeQuantity?: number; // Free/available quantity
  // ...
}
```

---

## FEFO Algorithm (First Expired First Out)

Both modes use the same auto-distribution logic:

```typescript
// Sort lots by expiry date (oldest first)
const sortedLots = lots.sort((a, b) => new Date(a.expiryDate) - new Date(b.expiryDate));

// Distribute quantity starting with oldest lot
let remaining = requestedQuantity;
for (const lot of sortedLots) {
  if (remaining <= 0) break;

  const assign = Math.min(remaining, lot.maxQuantity);
  lot.selectedQuantity = assign;
  remaining -= assign;
}
```

This ensures that products nearing expiry are returned first, following pharmacy best practices.

---

## Backend Integration

### Request Payload

```json
{
  "commandeId": 123,
  "commandeOrderDate": "2024-11-12",
  "commentaire": "Retour pour péremption",
  "retourBonItems": [
    {
      "orderLineId": 456,
      "orderLineOrderDate": "2024-11-12",
      "produitId": 789,
      "qtyMvt": 50,
      "motifRetourId": 1,
      "lotId": 101, // ← Lot tracking
      "lotNumero": "LOT-2024-001" // ← Lot number
    },
    {
      "orderLineId": 456,
      "orderLineOrderDate": "2024-11-12",
      "produitId": 789,
      "qtyMvt": 50,
      "motifRetourId": 1,
      "lotId": 102,
      "lotNumero": "LOT-2024-002"
    }
  ]
}
```

### Backend Processing

The backend receives separate return items per lot, allowing:

- ✅ Accurate inventory tracking per lot
- ✅ Lot-specific stock adjustments
- ✅ Traceability for audits
- ✅ Compliance with pharmaceutical regulations

---

## Configuration

### Default Mode

In `supplier-returns.component.ts`:

```typescript
// Change default mode here
protected lotSelectionMode = signal<'dialog' | 'inline'>('dialog');
```

### Customization Options

#### Dialog Size

```typescript
const modalRef = this.modalService.open(LotSelectionDialogComponent, {
  size: 'lg', // Options: 'sm', 'lg', 'xl'
  backdrop: 'static',
  centered: true,
});
```

#### Inline Grid Layout

```scss
// In inline-lot-selection.component.ts styles
.lots-grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  // Adjust 280px to change card width
}
```

---

## Responsive Design

### Dialog Mode

- ✅ Mobile: Full-screen modal
- ✅ Tablet: 750px centered modal
- ✅ Desktop: 750px centered modal

### Inline Mode

- ✅ Mobile: Single column cards
- ✅ Tablet: 2 column grid
- ✅ Desktop: 3+ column grid (based on screen width)

---

## Accessibility

### Keyboard Navigation

**Dialog Mode:**

- `Tab`: Navigate between inputs
- `Enter`: Confirm selection
- `Esc`: Cancel/close modal

**Inline Mode:**

- `Tab`: Navigate between controls
- `Space`: Toggle expand/collapse
- `Enter`: Confirm selection

### Screen Reader Support

- Proper ARIA labels on all inputs
- Status announcements for validation
- Clear button labels and tooltips

---

## Best Practices

### When Products Have Lots

✅ **DO**:

- Use lot selection for all returns
- Track lot-specific quantities
- Apply FEFO when possible
- Validate total quantities

❌ **DON'T**:

- Skip lot selection
- Create returns without lot information
- Ignore expiry dates

### When Products Don't Have Lots

- System automatically creates single return item
- No lot selection interface shown
- Standard quantity validation applies

---

## Troubleshooting

### Issue: No lots showing in dialog/inline

**Check**:

1. Does `orderLine.lots` exist and have length > 0?
2. Are lot quantities > 0?
3. Console for any errors

**Solution**:

```typescript
// Add logging in supplier-returns.component.ts
if (orderLine.lots && orderLine.lots.length > 0) {
  console.log('Lots available:', orderLine.lots);
}
```

### Issue: Cannot confirm selection

**Check**:

1. Total selected quantity matches requested quantity
2. At least one lot has quantity > 0

**Validation**:

```typescript
protected isValid(): boolean {
  const total = this.getTotalSelected();
  return total === this.requestedQuantity && total > 0;
}
```

### Issue: Mode toggle not working

**Check**:

1. Button click handler is bound correctly
2. Signal updates properly

**Debug**:

```typescript
protected toggleLotSelectionMode(): void {
  console.log('Current mode:', this.lotSelectionMode());
  this.lotSelectionMode.update(mode => mode === 'dialog' ? 'inline' : 'dialog');
  console.log('New mode:', this.lotSelectionMode());
}
```

---

## Future Enhancements

### Potential Features

1. **Barcode Scanning**: Scan lot numbers to auto-select
2. **History**: Show previously returned lots for same product
3. **Warnings**: Alert if selecting expired lots
4. **Bulk Operations**: Select multiple products at once
5. **Export**: Export lot selection details to PDF/Excel
6. **Preferences**: Remember user's preferred mode

### Performance Optimizations

1. **Virtual Scrolling**: For orders with 100+ lots
2. **Lazy Loading**: Load lot details on demand
3. **Caching**: Cache lot information

---

## Summary

| Feature             | Dialog Mode     | Inline Mode       |
| ------------------- | --------------- | ----------------- |
| **UI Type**         | Modal popup     | Expandable card   |
| **Best For**        | Quick selection | Detailed review   |
| **Screen Size**     | Mobile-friendly | Desktop-optimized |
| **Visual Feedback** | Focused         | Contextual        |
| **Auto-Fill**       | ✅ FEFO         | ✅ FEFO           |
| **Multi-Lot**       | ✅ Yes          | ✅ Yes            |
| **Validation**      | ✅ Real-time    | ✅ Real-time      |
| **Expiry Display**  | ✅ Table view   | ✅ Card view      |

Both modes provide **full lot tracking functionality** with the flexibility to choose the workflow that best suits your needs.

---

## Support

For questions or issues:

1. Check this guide
2. Review component code comments
3. Test with sample data
4. Contact development team

**Last Updated**: November 2025
**Version**: 1.0.0
