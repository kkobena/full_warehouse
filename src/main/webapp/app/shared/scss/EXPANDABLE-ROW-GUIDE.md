# Expandable Row Sub-Table - Quick Guide

This guide shows how to create consistent expandable rows with sub-tables using the common pharma styles.

## Quick Start

### Step 1: Import Common Styles

In your component SCSS:
```scss
@import 'app/shared/scss/table-common';
```

### Step 2: Use the Template

```html
<p-table [value]="items" dataKey="id" class="pharma-table">
  <!-- Main table header -->
  <ng-template #header>
    <tr class="pharma-table-head">
      <th style="width: 50px"></th>
      <th>Column 1</th>
      <th>Column 2</th>
    </tr>
  </ng-template>

  <!-- Main table body -->
  <ng-template #body let-item let-expanded="expanded">
    <tr>
      <td>
        <p-button
          [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
          [pRowToggler]="item"
          [rounded]="true"
          [text]="true"
        />
      </td>
      <td>{{ item.name }}</td>
      <td>{{ item.value }}</td>
    </tr>
  </ng-template>

  <!-- Expandable row content -->
  <ng-template #expandedrow let-item>
    <tr class="pharma-expanded-row">
      <td colspan="3">
        <div class="pharma-sub-table-card">
          <div class="card-header bg-info text-white">
            <i class="pi pi-list"></i>
            SUB-TABLE TITLE
          </div>
          <div class="card-body">
            <p-table [value]="item.details">
              <ng-template #header>
                <tr>
                  <th>#</th>
                  <th>Detail Column</th>
                </tr>
              </ng-template>
              <ng-template #body let-detail let-i="rowIndex">
                <tr>
                  <td>
                    <span class="pharma-row-number">{{ i + 1 }}</span>
                  </td>
                  <td>{{ detail.name }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        </div>
      </td>
    </tr>
  </ng-template>
</p-table>
```

## Two Styling Methods

### Method 1: pharma-sub-table-card (Recommended)

**Best for:** Bootstrap-style card appearance with colored headers

```html
<tr class="pharma-expanded-row">
  <td colspan="X">
    <div class="pharma-sub-table-card">
      <div class="card-header bg-info text-white">
        <i class="pi pi-list"></i>
        HEADER TEXT
      </div>
      <div class="card-body">
        <p-table><!-- Sub-table --></p-table>
      </div>
    </div>
  </td>
</tr>
```

**Features:**
- Card-based design with shadow
- Colored header with gradient
- Icon support
- Works with `.bg-info`, `.bg-primary`, `.bg-success`

---

### Method 2: pharma-sub-table-container (Alternative)

**Best for:** More minimalist design with custom header

```html
<tr class="pharma-expanded-row">
  <td colspan="X">
    <div class="pharma-sub-table-container">
      <div class="pharma-sub-table-header">
        <i class="pi pi-shopping-cart"></i>
        <span>HEADER TEXT</span>
      </div>
      <div class="pharma-sub-table-body">
        <p-table><!-- Sub-table --></p-table>
      </div>
    </div>
  </td>
</tr>
```

**Features:**
- Container-based design
- Info-colored header (teal/cyan)
- Flexbox layout for icon + text
- Margin around the container

---

## Important Notes

### 1. Colspan Value

**Always match the colspan to your main table's column count!**

```html
<!-- If main table has 6 columns -->
<td colspan="6">

<!-- If main table has 4 columns -->
<td colspan="4">
```

### 2. Required Classes

| Element | Required Class | Purpose |
|---------|---------------|---------|
| Expanded `<tr>` | `.pharma-expanded-row` | Removes padding, sets background |
| Container `<div>` | `.pharma-sub-table-card` or `.pharma-sub-table-container` | Applies card/container styling |
| Main table | `.pharma-table` | Applies main table styling |

### 3. Header Colors

```html
<!-- Teal/Cyan (Info) - Default -->
<div class="card-header bg-info text-white">

<!-- Blue (Primary) -->
<div class="card-header bg-primary text-white">

<!-- Green (Success) -->
<div class="card-header bg-success text-white">
```

### 4. Row Highlighting in Sub-tables

Use these classes on sub-table `<tr>` elements:

```html
<!-- Warning (Yellow background) -->
<tr class="pharma-sub-row-warning">

<!-- Success (Green background) -->
<tr class="pharma-sub-row-success">

<!-- Danger (Red background) -->
<tr class="pharma-sub-row-danger">

<!-- Legacy support -->
<tr class="table-danger">  <!-- Same as pharma-sub-row-warning -->
```

**Dynamic Example:**
```html
<tr [ngClass]="{
  'pharma-sub-row-warning': item.quantity < item.minQuantity,
  'pharma-sub-row-danger': item.quantity === 0,
  'pharma-sub-row-success': item.quantity >= item.optimalQuantity
}">
```

## Complete Real-World Example

```html
<!-- Component: prevente-modal.component.html -->
<div class="pharma-table-wrapper">
  <p-table
    [value]="sales"
    [paginator]="true"
    [rows]="10"
    dataKey="id"
    class="pharma-table">

    <!-- Main table header -->
    <ng-template #header>
      <tr class="pharma-table-head">
        <th style="width: 50px"></th>
        <th>Référence</th>
        <th>Articles</th>
        <th>Montant</th>
        <th>Client</th>
      </tr>
    </ng-template>

    <!-- Main table rows -->
    <ng-template #body let-sale let-expanded="expanded">
      <tr (click)="onSelect(sale)" [ngClass]="{ active: selectedSale?.id === sale.id }">
        <td>
          <p-button
            [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
            [pRowToggler]="sale"
            [rounded]="true"
            [text]="true"
          />
        </td>
        <td>
          <span class="pharma-code">{{ sale.reference }}</span>
        </td>
        <td class="text-right">
          <span class="pharma-qty-value">{{ sale.itemCount | number }}</span>
        </td>
        <td class="text-right">
          <span class="pharma-total">{{ sale.amount | number }}</span>
        </td>
        <td>
          <div class="pharma-entity-name">{{ sale.customerName }}</div>
        </td>
      </tr>
    </ng-template>

    <!-- Expandable row with product details -->
    <ng-template #expandedrow let-sale>
      <tr class="pharma-expanded-row">
        <td colspan="5">
          <div class="pharma-sub-table-card">
            <div class="card-header bg-info text-white">
              <i class="pi pi-list"></i>
              LISTE DES PRODUITS
            </div>
            <div class="card-body">
              <p-table
                [value]="sale.items"
                [paginator]="true"
                [rows]="10">

                <ng-template #header>
                  <tr>
                    <th style="width: 5%">#</th>
                    <th style="width: 10%">CODE</th>
                    <th>LIBELLÉ</th>
                    <th style="width: 10%">QTÉ.D</th>
                    <th style="width: 10%">QTÉ.S</th>
                    <th style="width: 10%">PU</th>
                    <th style="width: 12%">TOTAL</th>
                  </tr>
                </ng-template>

                <ng-template #body let-item let-i="rowIndex">
                  <tr [ngClass]="{ 'pharma-sub-row-warning': item.qtySold < item.qtyRequested }">
                    <td style="text-align: center">
                      <span class="pharma-row-number">{{ i + 1 }}</span>
                    </td>
                    <td>
                      <span class="pharma-code">{{ item.code }}</span>
                    </td>
                    <td>
                      <div class="pharma-product-name">{{ item.name }}</div>
                    </td>
                    <td class="text-right">
                      <span class="pharma-qty-value">{{ item.qtyRequested | number }}</span>
                    </td>
                    <td class="text-right">
                      <span class="pharma-qty-value">{{ item.qtySold | number }}</span>
                    </td>
                    <td class="text-right">
                      <span class="pharma-price">{{ item.unitPrice | number }}</span>
                    </td>
                    <td class="text-right">
                      <span class="pharma-total">{{ item.total | number }}</span>
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          </div>
        </td>
      </tr>
    </ng-template>
  </p-table>
</div>
```

## Troubleshooting

### Issue: Sub-table header not styled

**Solution:** Ensure you're using the correct class combination:
```html
<div class="pharma-sub-table-card">
  <div class="card-header bg-info text-white">
```

### Issue: Sub-table rows not alternating colors

**Solution:** The common styles handle this automatically. Make sure the parent component has `ViewEncapsulation.None` or the import is correct.

```typescript
@Component({
  encapsulation: ViewEncapsulation.None,
  // ...
})
```

### Issue: Colspan not spanning correctly

**Solution:** Count all columns in your main table (including the toggle column) and use that number:
```html
<!-- Main table: toggle + 4 columns = 5 total -->
<td colspan="5">
```

### Issue: Styles not applying

**Solution:** Check the import path:
```scss
// Correct
@import 'app/shared/scss/table-common';

// Incorrect
@import '../shared/scss/table-common';
```

## Visual Design

The expandable row design follows this hierarchy:

```
Main Table (Blue gradient header)
└─ Expandable Row (Light gray background)
   └─ Sub-Table Card (White with shadow)
      ├─ Colored Header (Info/Primary/Success gradient)
      └─ Sub-Table (Blue gradient header, same as main)
```

**Color Scheme:**
- **Main table header:** Blue gradient (#6b9ab8 → #5b89a6)
- **Expanded row background:** Light gray (#f8f9fa)
- **Card header (Info):** Teal gradient (#5bc0de → #46a8c4)
- **Card header (Primary):** Blue gradient (#5b89a6 → #4a7189)
- **Card header (Success):** Green gradient (#5cb85c → #4cae4c)
- **Sub-table header:** Blue gradient (same as main table)

## Best Practices

1. **Always use `pharma-expanded-row` class** on the expanded `<tr>`
2. **Match colspan** to the total column count
3. **Add icons** to headers for better visual hierarchy
4. **Use semantic row classes** (warning, success, danger) to highlight issues
5. **Apply pharma cell classes** (pharma-code, pharma-total, etc.) consistently
6. **Enable pagination** on sub-tables if they can have many items
7. **Keep header text uppercase** for consistency

## Quick Checklist

- [ ] Import `app/shared/scss/table-common` in component SCSS
- [ ] Add `.pharma-table` class to main `<p-table>`
- [ ] Add `.pharma-table-head` class to header `<tr>`
- [ ] Add `.pharma-expanded-row` class to expandedrow `<tr>`
- [ ] Add `.pharma-sub-table-card` wrapper inside expanded `<td>`
- [ ] Set correct `colspan` value
- [ ] Add header with icon and title
- [ ] Use pharma cell classes (pharma-code, pharma-total, etc.)
- [ ] Apply row highlighting classes as needed
- [ ] Test expansion/collapse functionality

---

For more examples and complete documentation, see `TABLE-COMMON-USAGE.md`.
