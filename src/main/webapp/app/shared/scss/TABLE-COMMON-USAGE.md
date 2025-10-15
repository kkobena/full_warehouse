# Common Table Styles - Usage Guide

This guide explains how to use the common table styles (`_table-common.scss`) across the Pharma-Smart application to maintain consistent table design.

## Table of Contents
- [Quick Start](#quick-start)
- [Components](#components)
- [Examples](#examples)
- [CSS Classes Reference](#css-classes-reference)
- [Customization](#customization)

---

## Quick Start

### 1. Import the Styles

In your component's SCSS file:

```scss
@import 'app/shared/scss/table-common';
```

### 2. Apply the Classes

In your component's HTML:

```html
<div class="pharma-table-wrapper">
  <!-- Header with search -->
  <div class="pharma-table-header-compact">
    <div class="pharma-filter-search">
      <p-iconfield>
        <p-inputicon class="pi pi-search" />
        <input
          pInputText
          placeholder="Rechercher..."
          type="text"
          class="pharma-search-input"
        />
      </p-iconfield>
    </div>
  </div>

  <!-- Table -->
  <p-table class="pharma-table">
    <ng-template #header>
      <tr class="pharma-table-head">
        <th>#</th>
        <th>CODE</th>
        <th>LIBELLÉ</th>
        <th>MONTANT</th>
      </tr>
    </ng-template>
  </p-table>
</div>
```

---

## Components

### 1. Table Wrapper

Wraps the entire table component with consistent styling:

```html
<div class="pharma-table-wrapper">
  <!-- Table content here -->
</div>
```

**Features:**
- White background
- Rounded corners
- Subtle shadow
- Overflow handling

---

### 2. Compact Header

Modern header with search and optional actions:

```html
<div class="pharma-table-header-compact">
  <!-- Search section -->
  <div class="pharma-filter-search">
    <p-iconfield>
      <p-inputicon class="pi pi-search" />
      <input
        pInputText
        placeholder="Rechercher..."
        type="text"
        class="pharma-search-input"
      />
    </p-iconfield>
  </div>

  <!-- Optional divider -->
  <div class="pharma-divider"></div>

  <!-- Optional custom section -->
  <div>
    <!-- Custom buttons, filters, etc. -->
  </div>
</div>
```

---

### 3. Table Header

Styled table header with gradient background:

```html
<ng-template #header>
  <tr class="pharma-table-head">
    <th style="width: 20px; text-align: center">#</th>
    <th>CODE</th>
    <th>LIBELLÉ</th>
    <th style="width: 130px; text-align: right">MONTANT</th>
    <th style="width: 50px; text-align: center"></th>
  </tr>
</ng-template>
```

**Features:**
- Blue gradient background (`#6b9ab8` to `#5b89a6`)
- White text
- Uppercase
- Letter spacing
- Border separator between columns

---

### 4. Table Body

```html
<ng-template #body let-item let-rowIndex="rowIndex">
  <tr>
    <!-- Row number -->
    <td style="text-align: center">
      <span class="pharma-row-number">{{ rowIndex + 1 }}</span>
    </td>

    <!-- Code -->
    <td>
      <span class="pharma-code">{{ item.code }}</span>
    </td>

    <!-- Name/Label -->
    <td>
      <div class="pharma-entity-name">{{ item.name }}</div>
    </td>

    <!-- Amount -->
    <td style="text-align: right">
      <span class="pharma-total">{{ item.amount | number }}</span>
    </td>

    <!-- Actions -->
    <td style="text-align: center">
      <div class="pharma-action-buttons">
        <p-button icon="pi pi-pencil" [rounded]="true" [text]="true" />
        <p-button icon="pi pi-trash" [rounded]="true" [text]="true" severity="danger" />
      </div>
    </td>
  </tr>
</ng-template>
```

---

### 5. Table Footer

```html
<ng-template #footer>
  <tr class="pharma-table-footer">
    <td class="pharma-footer-label" colspan="3">
      <i class="pi pi-calculator"></i>
      <span>TOTAUX</span>
    </td>
    <td style="text-align: right">
      <span class="pharma-footer-total">{{ total | number }} F</span>
    </td>
    <td></td>
  </tr>
</ng-template>
```

---

### 6. Empty Message

```html
<ng-template #emptymessage>
  <tr>
    <td colspan="5" class="pharma-empty-message">
      <div class="pharma-empty-content">
        <i class="pi pi-inbox"></i>
        <p>Aucune donnée disponible</p>
        <small>Aucun élément trouvé</small>
      </div>
    </td>
  </tr>
</ng-template>
```

---

## Examples

### Example 1: Simple Table with Search

```html
<div class="pharma-table-wrapper">
  <div class="pharma-table-header-compact">
    <div class="pharma-filter-search">
      <p-iconfield>
        <p-inputicon class="pi pi-search" />
        <input
          #filterInput
          (input)="myTable.filterGlobal($event.target.value, 'contains')"
          pInputText
          placeholder="Rechercher un client..."
          type="text"
          class="pharma-search-input"
        />
      </p-iconfield>
    </div>
  </div>

  <p-table
    #myTable
    [value]="customers"
    [paginator]="true"
    [rows]="10"
    [globalFilterFields]="['name', 'code']"
    class="pharma-table">

    <ng-template #header>
      <tr class="pharma-table-head">
        <th style="width: 20px">#</th>
        <th>CODE</th>
        <th>NOM</th>
        <th>TÉLÉPHONE</th>
      </tr>
    </ng-template>

    <ng-template #body let-customer let-i="rowIndex">
      <tr>
        <td style="text-align: center">
          <span class="pharma-row-number">{{ i + 1 }}</span>
        </td>
        <td>
          <span class="pharma-code">{{ customer.code }}</span>
        </td>
        <td>
          <div class="pharma-entity-name">{{ customer.name }}</div>
        </td>
        <td>{{ customer.phone }}</td>
      </tr>
    </ng-template>
  </p-table>
</div>
```

---

### Example 2: Table with Status Badges

```html
<ng-template #body let-order>
  <tr>
    <td>{{ order.id }}</td>
    <td>
      <span class="pharma-code">{{ order.code }}</span>
    </td>
    <td>
      <span [ngClass]="{
        'pharma-badge pharma-badge-success': order.status === 'COMPLETED',
        'pharma-badge pharma-badge-warning': order.status === 'PENDING',
        'pharma-badge pharma-badge-danger': order.status === 'CANCELLED'
      }">
        {{ order.status }}
      </span>
    </td>
  </tr>
</ng-template>
```

---

### Example 3: Table with Conditional Row Highlighting

```html
<ng-template #body let-stock>
  <tr [ngClass]="{
    'pharma-row-warning': stock.quantity < stock.minQuantity,
    'pharma-row-danger': stock.quantity === 0,
    'pharma-row-success': stock.quantity >= stock.optimalQuantity
  }">
    <td>{{ stock.productName }}</td>
    <td style="text-align: right">
      <span class="pharma-qty-value">{{ stock.quantity | number }}</span>
    </td>
  </tr>
</ng-template>
```

---

### Example 4: Table with Editable Cells

```html
<ng-template #body let-item>
  <tr>
    <td pEditableColumn style="text-align: right">
      <p-cellEditor>
        <ng-template #input>
          <input
            pInputText
            type="number"
            (focus)="$event.target.select()"
            (keydown.enter)="updateQuantity(item, $event)"
            class="pharma-input-qty"
          />
        </ng-template>
        <ng-template #output>
          <span class="pharma-qty-value">{{ item.quantity | number }}</span>
        </ng-template>
      </p-cellEditor>
    </td>
  </tr>
</ng-template>
```

---

### Example 5: Table with Expandable Rows (Sub-table)

**Method 1: Using pharma-sub-table-card (Recommended)**

```html
<p-table [value]="sales" dataKey="id" class="pharma-table">
  <ng-template #header>
    <tr class="pharma-table-head">
      <th style="width: 50px"></th>
      <th>Référence</th>
      <th>Montant</th>
    </tr>
  </ng-template>

  <ng-template #body let-sale let-expanded="expanded">
    <tr>
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
        <span class="pharma-total">{{ sale.amount | number }}</span>
      </td>
    </tr>
  </ng-template>

  <!-- Expandable row with sub-table -->
  <ng-template #expandedrow let-sale>
    <tr class="pharma-expanded-row">
      <td colspan="3">
        <div class="pharma-sub-table-card">
          <div class="card-header bg-info text-white">
            <i class="pi pi-list"></i>
            DÉTAILS DES PRODUITS
          </div>
          <div class="card-body">
            <p-table
              [value]="sale.items"
              [paginator]="true"
              [rows]="10">
              <ng-template #header>
                <tr>
                  <th>#</th>
                  <th>CODE</th>
                  <th>LIBELLÉ</th>
                  <th>QUANTITÉ</th>
                  <th>MONTANT</th>
                </tr>
              </ng-template>
              <ng-template #body let-item let-i="rowIndex">
                <tr [ngClass]="{ 'pharma-sub-row-warning': item.hasIssue }">
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
                    <span class="pharma-qty-value">{{ item.quantity | number }}</span>
                  </td>
                  <td class="text-right">
                    <span class="pharma-total">{{ item.amount | number }}</span>
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
```

**Method 2: Using pharma-sub-table-container (Alternative)**

```html
<ng-template #expandedrow let-order>
  <tr class="pharma-expanded-row">
    <td colspan="5">
      <div class="pharma-sub-table-container">
        <div class="pharma-sub-table-header">
          <i class="pi pi-shopping-cart"></i>
          <span>ARTICLES COMMANDÉS</span>
        </div>
        <div class="pharma-sub-table-body">
          <p-table [value]="order.products">
            <!-- Sub-table content -->
          </p-table>
        </div>
      </div>
    </td>
  </tr>
</ng-template>
```

**Available Header Colors:**
```html
<!-- Info (Teal/Cyan) - Default -->
<div class="card-header bg-info">

<!-- Primary (Blue) -->
<div class="card-header bg-primary">

<!-- Success (Green) -->
<div class="card-header bg-success">
```

---

### Example 6: Table with Header Actions

```html
<div class="pharma-table-header-compact">
  <div class="pharma-filter-search">
    <p-iconfield>
      <p-inputicon class="pi pi-search" />
      <input
        pInputText
        placeholder="Rechercher..."
        type="text"
        class="pharma-search-input"
      />
    </p-iconfield>
  </div>

  <div class="pharma-divider"></div>

  <div style="display: flex; gap: 8px;">
    <p-button
      label="Nouveau"
      icon="pi pi-plus"
      (onClick)="addNew()"
    />
    <p-button
      label="Exporter"
      icon="pi pi-download"
      severity="secondary"
      (onClick)="export()"
    />
  </div>
</div>
```

---

## CSS Classes Reference

### Layout Classes

| Class | Description |
|-------|-------------|
| `.pharma-table-wrapper` | Main table container with shadow and border |
| `.pharma-table-header-compact` | Compact header with flexbox layout |
| `.pharma-filter-search` | Search input container |
| `.pharma-divider` | Vertical separator line |
| `.pharma-table` | Main table styling (apply to `<p-table>`) |

### Header Classes

| Class | Description |
|-------|-------------|
| `.pharma-table-head` | Styled table header with gradient |

### Row Classes

| Class | Description |
|-------|-------------|
| `.pharma-row-warning` | Yellow background for warnings |
| `.pharma-row-success` | Green background for success |
| `.pharma-row-danger` | Red background for errors |
| `.pharma-row-hover` | Hover effect (applied automatically) |

### Cell Content Classes

| Class | Description |
|-------|-------------|
| `.pharma-row-number` | Circular badge for row numbers |
| `.pharma-code` | Monospace code styling with blue background |
| `.pharma-entity-name` | Standard text for names/labels |
| `.pharma-product-name` | Alias for entity name |
| `.pharma-qty-value` | Bold blue text for quantities |
| `.pharma-price` | Bold text for prices |
| `.pharma-total` | Bold green text for totals |

### Badge Classes

| Class | Description |
|-------|-------------|
| `.pharma-badge` | Base badge styling |
| `.pharma-badge-success` | Green badge |
| `.pharma-badge-warning` | Yellow badge |
| `.pharma-badge-danger` | Red badge |
| `.pharma-badge-info` | Blue badge |
| `.pharma-badge-primary` | Primary blue badge |

### Input Classes

| Class | Description |
|-------|-------------|
| `.pharma-input-qty` | Styled number input for quantities |
| `.pharma-input-price` | Styled number input for prices |
| `.pharma-input-text` | Styled text input |

### Footer Classes

| Class | Description |
|-------|-------------|
| `.pharma-table-footer` | Footer row styling |
| `.pharma-footer-label` | Label with icon in footer |
| `.pharma-footer-value` | Value in footer |
| `.pharma-footer-total` | Large total value in footer |

### Expandable Row Classes

| Class | Description |
|-------|-------------|
| `.pharma-expanded-row` | Apply to expanded `<tr>` element |
| `.pharma-sub-table-card` | Card container for sub-table (Bootstrap-style) |
| `.pharma-sub-table-container` | Alternative container for sub-table |
| `.pharma-sub-table-header` | Header for sub-table container |
| `.pharma-sub-table-body` | Body wrapper for sub-table |
| `.pharma-sub-row-warning` | Warning style for sub-table rows |
| `.pharma-sub-row-success` | Success style for sub-table rows |
| `.pharma-sub-row-danger` | Danger style for sub-table rows |

### Utility Classes

| Class | Description |
|-------|-------------|
| `.text-center` | Center align text |
| `.text-right` | Right align text |
| `.text-left` | Left align text |
| `.font-weight-bold` | Bold font (700) |
| `.font-weight-semibold` | Semibold font (600) |
| `.font-weight-normal` | Normal font (400) |

---

## Customization

### Override Variables

You can override the default colors by defining variables before importing:

```scss
// In your component SCSS
$pharma-primary: #3498db;
$pharma-success: #27ae60;
$pharma-danger: #e74c3c;

@import 'app/shared/scss/table-common';
```

### Available Variables

- `$pharma-primary`: Primary color (default: `#5b89a6`)
- `$pharma-success`: Success color (default: `#5cb85c`)
- `$pharma-danger`: Danger color (default: `#d9534f`)
- `$pharma-warning`: Warning color (default: `#f0ad4e`)
- `$pharma-info`: Info color (default: `#5bc0de`)
- `$pharma-bg`: Background color (default: `#f5f7fa`)
- `$pharma-white`: White color (default: `#ffffff`)
- `$pharma-border`: Border color (default: `#dfe3e8`)
- `$pharma-text`: Text color (default: `#333333`)
- `$pharma-text-light`: Light text color (default: `#6c757d`)
- `$pharma-radius`: Border radius (default: `4px`)
- `$pharma-shadow`: Box shadow (default: `0 1px 3px rgba(0, 0, 0, 0.1)`)

---

## Best Practices

1. **Always use the wrapper**: Wrap tables in `.pharma-table-wrapper` for consistent styling
2. **Use semantic classes**: Use `.pharma-code` for codes, `.pharma-total` for totals, etc.
3. **Colspan attention**: When using colspan, ensure it matches the total number of columns
4. **Responsive design**: The styles are responsive, but test on different screen sizes
5. **Print styles**: Print styles hide pagination and buttons automatically
6. **Accessibility**: Always include proper `aria-label` attributes for icons and buttons

---

## Migration from Product Table

If you're migrating from the product table styles:

**Before:**
```scss
// In component.scss
.pharma-table-wrapper {
  // Custom styles copied from product table
}
```

**After:**
```scss
@import 'app/shared/scss/table-common';

// Add only component-specific overrides here
```

---

## Troubleshooting

### Colspan not working
- Check if you have `display: flex` on the `td` element
- Remove flex display from table cells

### Styles not applied
- Ensure you've imported the file correctly
- Check that class names match exactly (case-sensitive)
- Verify `::ng-deep` is working in your component

### Colors don't match
- Override variables before import
- Check if there are component-specific styles overriding the common ones

---

## Support

For issues or questions:
- Check existing table implementations in `src/main/webapp/app/entities/sales/selling-home/product-table/`
- Consult CLAUDE.md for project-wide conventions
- Create an issue in the project repository
