# Shared SCSS Styles - Pharma Smart

This directory contains common SCSS files that can be imported and reused across the application to maintain consistency and reduce code duplication.

## Available Style Files

### 1. `_pharma-nav.scss` - Navigation Styles

Common navigation styles for the Pharma-Smart application, including basic horizontal pills and modern vertical sidebar navigation.

#### Features

- **Basic Navigation Pills**: Simple horizontal navigation with hover and active states
- **Vertical Sidebar Navigation**: Modern vertical navigation with sidebar card layout
- **Responsive Design**: Automatically adjusts for different screen sizes
- **Custom Scrollbars**: Styled scrollbars for better UX
- **Smooth Animations**: Hover effects and transitions

#### Usage

##### Basic Navigation Pills

```scss
// Import in your component SCSS
@import '../../../shared/scss/pharma-nav';
```

```html
<!-- In your component HTML -->
<div class="nav nav-pills">
  <a class="pharma-nav-link active">Tab 1</a>
  <a class="pharma-nav-link">Tab 2</a>
  <a class="pharma-nav-link">Tab 3</a>
</div>
```

##### Vertical Sidebar Navigation with ngbNav

```scss
// Import in your component SCSS
@import '../../../shared/scss/pharma-nav';
```

```html
<!-- In your component HTML -->
<div class="pharma-nav-sidebar">
  <div class="pharma-nav-sidebar-card">
    <div class="pharma-nav-sidebar-header">
      <i class="pi pi-shopping-cart"></i>
      <span>Navigation Title</span>
    </div>

    <div class="pharma-nav-sidebar-content">
      <div
        #nav="ngbNav"
        [(activeId)]="activeId"
        class="nav flex-column nav-pills"
        ngbNav
        orientation="vertical">

        <ng-container ngbNavItem="item1">
          <a class="pharma-nav-vertical-link" ngbNavLink>
            <i class="pi pi-wallet"></i>
            <span>Item 1</span>
            <span class="link-arrow">›</span>
          </a>
          <ng-template ngbNavContent>
            <!-- Your content here -->
          </ng-template>
        </ng-container>

        <ng-container ngbNavItem="item2">
          <a class="pharma-nav-vertical-link" ngbNavLink>
            <i class="pi pi-shield"></i>
            <span>Item 2</span>
            <span class="link-arrow">›</span>
          </a>
          <ng-template ngbNavContent>
            <!-- Your content here -->
          </ng-template>
        </ng-container>

      </div>
    </div>
  </div>
</div>

<!-- Content outlet -->
<div [ngbNavOutlet]="nav"></div>
```

#### Available Classes

##### Sidebar Container Classes

| Class | Description |
|-------|-------------|
| `.pharma-nav-sidebar` | Main sidebar container (240px width, responsive) |
| `.pharma-nav-sidebar-card` | Card wrapper with shadow and rounded corners |
| `.pharma-nav-sidebar-header` | Gradient header with icon support |
| `.pharma-nav-sidebar-content` | Scrollable content area for navigation items |

##### Navigation Link Classes

| Class | Description |
|-------|-------------|
| `.pharma-nav-link` | Basic horizontal navigation pill |
| `.pharma-nav-vertical-link` | Vertical navigation link with icons and arrow |

#### Responsive Behavior

**IMPORTANT**: The sidebar uses `width: 100%` to work seamlessly with Bootstrap's grid system. Control the sidebar width using Bootstrap column classes:

```html
<!-- Recommended Bootstrap Grid Setup -->
<div class="row">
  <div class="col-xl-2 col-lg-2 col-md-3 col-sm-4">
    <div class="pharma-nav-sidebar">
      <!-- Navigation content -->
    </div>
  </div>
  <div class="col-xl-10 col-lg-10 col-md-9 col-sm-8">
    <!-- Main content with [ngbNavOutlet] -->
  </div>
</div>
```

**Breakpoint Behavior:**
- **Desktop (> 1200px)**: Vertical sidebar layout (width controlled by Bootstrap columns)
- **Tablets (768px - 1200px)**: Horizontal layout with 300px max height
- **Mobile (< 768px)**: Full width vertical layout

#### Color Variables

The following Pharma-Smart color variables are defined and can be used:

```scss
$pharma-primary: #5b89a6;
$pharma-primary-dark: #4a7189;
$pharma-primary-light: #e8f4f8;
$pharma-success: #5cb85c;
$pharma-info: #5bc0de;
$pharma-secondary: #95a5a6;
$pharma-danger: #d9534f;

// Gray scale
$gray-50 to $gray-900

// Shadows
$pharma-shadow-sm
$pharma-shadow-md
$pharma-shadow-hover

// Border radius
$pharma-radius (4px)
$pharma-radius-sm (6px)
$pharma-radius-lg (12px)
```

#### Example Implementation

See the selling-home component for a complete example:
- `src/main/webapp/app/entities/sales/selling-home/selling-home.component.html`
- `src/main/webapp/app/entities/sales/selling-home/selling-home.component.scss`

---

### 2. `_modal-theme.scss` - Modal Styles

Common modal styling (implementation details TBD)

---

### 3. `_table-common.scss` - Table Styles

Common table styling for data grids (implementation details TBD)

---

## Troubleshooting

### Navigation Overlaps with Content

If you experience overlapping between the sidebar and content on medium screens (14-inch laptops), ensure:

1. **Use Bootstrap Grid Properly**: Always wrap the sidebar in proper Bootstrap column classes
   ```html
   <div class="row">
     <div class="col-xl-2 col-lg-2 col-md-3 col-sm-4">
       <div class="pharma-nav-sidebar">...</div>
     </div>
     <div class="col-xl-10 col-lg-10 col-md-9 col-sm-8">
       <div [ngbNavOutlet]="nav"></div>
     </div>
   </div>
   ```

2. **Avoid Fixed Widths**: Don't add custom `width` styles to `.pharma-nav-sidebar` as it uses `width: 100%` to work with Bootstrap columns

3. **Check Parent Container**: Ensure the parent has proper Bootstrap classes (`container-fluid` or `container`)

4. **Test at 1366px**: Most 14-inch screens are 1366x768. Test your layout at this resolution.

### Sidebar Takes Up Too Much Space (Flexbox Layouts)

If you're using a **flexbox layout** instead of Bootstrap grid (like in `selling-home.component`), you need to override the sidebar width:

```scss
// In your component.scss
.your-sidebar-wrapper {
  &.pharma-nav-sidebar {
    width: 240px; // Fixed width for flexbox layouts
  }

  @media (max-width: 1400px) and (min-width: 1201px) {
    &.pharma-nav-sidebar {
      width: 220px; // Slightly smaller on medium screens
    }
  }

  @media (max-width: 1200px) {
    &.pharma-nav-sidebar {
      width: 100%; // Full width on tablets
    }
  }
}
```

**When to use this pattern:**
- Component uses CSS flexbox for layout (not Bootstrap grid)
- Parent container uses `display: flex`
- Example: `selling-home.component` uses this pattern

## Best Practices

1. **Always import shared styles** instead of duplicating CSS
2. **Use the provided class names** to ensure consistency
3. **Extend, don't override**: If you need component-specific styles, extend the common classes
4. **Test responsiveness**: Verify your implementation works on all screen sizes (especially 1366px width)
5. **Follow the pattern**: Use the example implementations as templates
6. **Use Bootstrap grid**: Always use Bootstrap's column system for layout control

## Contributing

When adding new shared styles:

1. Create descriptive class names prefixed with `pharma-`
2. Document the usage with examples
3. Update this README
4. Test across different components and screen sizes
5. Ensure backward compatibility

## Questions?

For questions or issues related to these shared styles, please contact the frontend team or create an issue in the project repository.
