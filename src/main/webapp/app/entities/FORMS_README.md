# Forms Design Pattern & Guidelines

This document outlines the standardized form design patterns used throughout the Pharma-Smart application. Follow these guidelines when creating or modifying forms to ensure consistency and optimal user experience.

## Table of Contents

1. [Overview](#overview)
2. [Section Organization](#section-organization)
3. [Layout Patterns](#layout-patterns)
4. [Styling Guidelines](#styling-guidelines)
5. [Component Structure](#component-structure)
6. [Responsive Behavior](#responsive-behavior)
7. [Common Issues & Solutions](#common-issues--solutions)
8. [Examples](#examples)

## Overview

All forms in the application follow a consistent pattern:
- **Required fields first**: Critical information at the top with red-themed headers
- **Logical grouping**: Related fields organized into sections with p-card components
- **Multi-column layouts**: Responsive grid layouts that adapt to screen size
- **Scrollable modal body**: Forms inside modals have scrollable content areas
- **Sticky footer**: Action buttons remain visible at the bottom

## Section Organization

### Required Fields Section
Always place required fields in the first section with a distinctive red theme:

```html
<p-card>
  <ng-template #header>
    <div class="section-header section-header-required">
      <i class="pi pi-exclamation-circle"></i>
      <span>Champs Obligatoires</span>
    </div>
  </ng-template>

  <div class="form-grid-compact">
    <!-- Required fields here -->
    <div class="form-field form-field-full">
      <label for="field_name">Field Name <span class="required">*</span></label>
      <input pInputText id="field_name" formControlName="name" class="w-full" />
      @if (editForm.get('name')?.invalid && editForm.get('name')?.touched) {
        @if (editForm.get('name')?.errors?.required) {
          <small class="p-error" jhiTranslate="entity.validation.required">Ce champ est obligatoire</small>
        }
      }
    </div>
  </div>
</p-card>
```

### Optional/Informational Sections
Use blue-themed headers for optional or informational sections:

```html
<p-card>
  <ng-template #header>
    <div class="section-header">
      <i class="pi pi-info-circle"></i>
      <span>Informations Complémentaires</span>
    </div>
  </ng-template>

  <div class="form-grid-compact">
    <!-- Optional fields here -->
  </div>
</p-card>
```

### Section Header Icons
Use appropriate PrimeIcons based on section purpose:
- `pi-exclamation-circle`: Required fields
- `pi-info-circle`: General information
- `pi-user`: User/customer information
- `pi-file-edit`: Billing/invoicing parameters
- `pi-calculator`: Financial calculations/limits
- `pi-users`: Client/group information
- `pi-shield`: Insurance/security

## Layout Patterns

### Two-Column Layout
For forms with primary and secondary sections side-by-side:

```html
<div class="card-grid-2col">
  <p-card>
    <!-- First column content -->
  </p-card>

  <p-card>
    <!-- Second column content -->
  </p-card>
</div>
```

**Use cases**:
- Required fields + Optional fields
- Primary info + Secondary info
- Main data + Metadata

### Three-Column Layout
For forms with multiple equal-weight sections:

```html
<div class="card-grid-3col">
  <p-card>
    <!-- First column content -->
  </p-card>

  <p-card>
    <!-- Second column content -->
  </p-card>

  <p-card>
    <!-- Third column content -->
  </p-card>
</div>
```

**Use cases**:
- Multiple parameter sections
- Detailed configuration forms
- Dashboard-style layouts

### Single-Column Inside Cards
**Important**: Inside each card, use single-column layout with `form-field-full`:

```html
<div class="form-grid-compact">
  <div class="form-field form-field-full">
    <label>Field Label</label>
    <input pInputText formControlName="fieldName" class="w-full" />
  </div>

  <div class="form-field form-field-full">
    <label>Another Field</label>
    <input pInputText formControlName="anotherField" class="w-full" />
  </div>
</div>
```

### Form Grid for Multiple Columns Inside Cards
If you need multiple columns inside a single card:

```html
<div class="form-grid-compact">
  <div class="form-field form-field-2col">
    <label>Two-column field</label>
    <input pInputText formControlName="field1" class="w-full" />
  </div>

  <div class="form-field">
    <label>Regular field</label>
    <input pInputText formControlName="field2" class="w-full" />
  </div>
</div>
```

## Styling Guidelines

### Import Centralized Styles
All form components should import the centralized form styles:

```scss
@import '../../../shared/scss/form-styles';

:host {
  @include modal-theme;
}
```

### Modal Structure
Forms inside modals should follow this structure:

```html
<div class="modal-header">
  <h4 class="modal-title">{{ header }}</h4>
  <button (click)="cancel()" class="btn-close" type="button">&times;</button>
</div>

<form [formGroup]="editForm" (ngSubmit)="save()">
  <div class="modal-body">
    <!-- Form content here -->
  </div>

  <div class="form-actions-compact">
    <p-button label="Annuler" [text]="true" (click)="cancel()"></p-button>
    <p-button label="Enregistrer" severity="primary" type="submit"
              [disabled]="editForm.invalid" [loading]="isSaving"></p-button>
  </div>
</form>
```

### Required Field Indicator
Always add asterisk with the `required` class:

```html
<label for="field_name">Field Name <span class="required">*</span></label>
```

### Error Messages
Use PrimeNG's `p-error` class with Angular control flow:

```html
@if (editForm.get('fieldName')?.invalid && editForm.get('fieldName')?.touched) {
  @if (editForm.get('fieldName')?.errors?.required) {
    <small class="p-error" jhiTranslate="entity.validation.required">Ce champ est obligatoire</small>
  }
  @if (editForm.get('fieldName')?.errors?.min || editForm.get('fieldName')?.errors?.max) {
    <small class="p-error">La valeur doit être entre {{ min }} et {{ max }}</small>
  }
}
```

## Component Structure

### Standalone Component
Use Angular 20 standalone components with signals:

```typescript
@Component({
  selector: 'jhi-entity-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    // ... other PrimeNG modules
  ],
  templateUrl: './entity-form.component.html',
  styleUrl: './entity-form.component.scss'
})
export class EntityFormComponent implements OnInit {
  editForm = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    name: ['', [Validators.required]],
    // ... other fields
  });

  isSaving = signal(false);

  // ... component logic
}
```

### Form Validation
Use reactive forms with validators:

```typescript
editForm = inject(FormBuilder).nonNullable.group({
  name: ['', [Validators.required, Validators.minLength(3)]],
  email: ['', [Validators.email]],
  phone: ['', [Validators.pattern(/^\d{10}$/)]],
  taux: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
});
```

## Responsive Behavior

### Breakpoints
The form layouts adapt at these breakpoints:

- **Large screens (>1200px)**: Full multi-column layout
- **Medium-large (992px-1200px)**: 3-column layouts become 2-column
- **Medium (768px-992px)**: All multi-column layouts become single column
- **Mobile (<768px)**: Optimized spacing, stacked buttons

### Testing Responsiveness
Test your forms at these widths:
- 1920px (desktop)
- 1200px (laptop)
- 992px (tablet landscape)
- 768px (tablet portrait)
- 375px (mobile)

## Common Issues & Solutions

### Issue 1: Dropdown Menu Clipped/Hidden

**Problem**: p-select or p-autoComplete dropdown is hidden behind modal or card borders.

**Solution**:
```scss
::ng-deep .p-card {
  overflow: visible;

  .p-card-body,
  .p-card-content {
    overflow: visible;
  }
}

.modal-body {
  overflow-x: hidden; // Prevent horizontal scroll
  overflow-y: auto;   // Allow vertical scroll
}
```

### Issue 2: p-autoComplete Dropdown Icon Wrapping

**Problem**: Dropdown button wraps to next line.

**Solution**: Already handled in centralized styles with flexbox layout.

### Issue 3: p-autoComplete Fields Shrinking

**Problem**: Autocomplete fields become too narrow in grid layouts.

**Solution**: Use `min-width: 0` on grid containers (already in centralized styles):
```scss
.form-grid-compact {
  .form-field {
    min-width: 0;
  }
}
```

### Issue 4: Modal Content Not Scrollable

**Problem**: Long forms extend beyond viewport.

**Solution**:
```scss
.modal-body {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.form-actions-compact {
  position: sticky;
  bottom: 0;
  z-index: 100;
}
```

### Issue 5: Form Actions Hidden

**Problem**: Save/Cancel buttons not visible on long forms.

**Solution**: Use sticky footer pattern (already in centralized styles).

## Examples

### Example 1: Simple Two-Section Form

**Template** (`simple-form.component.html`):
```html
<div class="modal-header">
  <h4 class="modal-title">Create Item</h4>
  <button (click)="cancel()" class="btn-close" type="button">&times;</button>
</div>

<form [formGroup]="editForm" (ngSubmit)="save()">
  <div class="modal-body">
    <div class="card-grid-2col">
      <!-- Required Fields -->
      <p-card>
        <ng-template #header>
          <div class="section-header section-header-required">
            <i class="pi pi-exclamation-circle"></i>
            <span>Champs Obligatoires</span>
          </div>
        </ng-template>

        <div class="form-grid-compact">
          <div class="form-field form-field-full">
            <label for="field_name">Name <span class="required">*</span></label>
            <input pInputText id="field_name" formControlName="name" class="w-full" />
          </div>
        </div>
      </p-card>

      <!-- Optional Fields -->
      <p-card>
        <ng-template #header>
          <div class="section-header">
            <i class="pi pi-info-circle"></i>
            <span>Optional Information</span>
          </div>
        </ng-template>

        <div class="form-grid-compact">
          <div class="form-field form-field-full">
            <label for="field_description">Description</label>
            <input pInputText id="field_description" formControlName="description" class="w-full" />
          </div>
        </div>
      </p-card>
    </div>
  </div>

  <div class="form-actions-compact">
    <p-button label="Annuler" [text]="true" type="button" (click)="cancel()"></p-button>
    <p-button label="Enregistrer" severity="primary" type="submit"
              [disabled]="editForm.invalid" [loading]="isSaving"></p-button>
  </div>
</form>
```

**Styles** (`simple-form.component.scss`):
```scss
@import '../../../shared/scss/form-styles';

:host {
  @include modal-theme;
}

// Add any component-specific styles here
```

### Example 2: Complex Multi-Section Form

See `form-tiers-payant.component.html` for a complete example with:
- 2-column layout (Required + General Info)
- 3-column layout (Parameters + Limits + Client Limits)
- Conditional sections
- Multiple field types (input, select, toggleswitch, input-number)

### Example 3: Stepper Form

See `assure-form-step.component.html` for a complete example with:
- Multi-step wizard
- Form validation per step
- Sticky footer with step navigation
- Nested components per step

## Migration Checklist

When updating an existing form to use this pattern:

- [ ] Import centralized form styles in SCSS
- [ ] Restructure HTML with required fields first
- [ ] Wrap sections in p-card components
- [ ] Add section headers with icons
- [ ] Use card-grid-2col or card-grid-3col for layout
- [ ] Use form-grid-compact inside cards
- [ ] Add form-field-full to all fields inside cards
- [ ] Move action buttons to form-actions-compact footer
- [ ] Add required asterisks to labels
- [ ] Implement proper error messages
- [ ] Test responsive behavior at all breakpoints
- [ ] Remove component-specific styles that duplicate centralized styles

## Best Practices

1. **Always validate**: Use reactive forms with validators
2. **Accessibility**: Use proper labels with `for` attributes
3. **Consistency**: Follow icon and color conventions
4. **Performance**: Use trackBy in @for loops
5. **Signals**: Use Angular signals for reactive state
6. **Error handling**: Show clear, localized error messages
7. **Loading states**: Disable and show loading on buttons during save
8. **Focus management**: Auto-focus first field with `#fieldRef` and `autofocus`
9. **Keyboard navigation**: Ensure tab order is logical
10. **Mobile-first**: Test on mobile devices, not just browser responsive mode

## Questions or Issues?

If you encounter issues not covered here, check:
- `src/main/webapp/app/shared/scss/form-styles.scss` - Centralized styles
- `src/main/webapp/app/entities/tiers-payant/form-tiers-payant/` - Reference implementation
- `src/main/webapp/app/entities/customer/assure-form-step/` - Stepper reference implementation

For new patterns or improvements, update this document and the centralized styles.
