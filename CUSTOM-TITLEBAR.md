# Custom Titlebar for Tauri App

PharmaSmart includes a custom titlebar for the Tauri desktop application, providing a modern, native look and feel.

## Features

✅ **Custom window controls** - Minimize, Maximize/Restore, Close buttons
✅ **New instance button** - Open multiple windows with one click
✅ **Draggable titlebar** - Click and drag to move the window
✅ **Dynamic title** - Shows app name and current route
✅ **Modern design** - Gradient background with smooth animations
✅ **Cross-platform** - Works on Windows, Linux, and macOS
✅ **Tauri-only** - Automatically hidden in web mode
✅ **Responsive** - Adapts to window state (maximized/restored)

---

## Architecture

### Components

**File: `src/main/webapp/app/shared/titlebar/titlebar.component.ts`**

- Detects Tauri environment
- Manages window state (minimized, maximized, restored)
- Handles window control button clicks
- Tracks current route for display

**File: `src/main/webapp/app/shared/titlebar/titlebar.component.html`**

- Custom titlebar layout
- App icon, title, and current route display
- Window control buttons (minimize, maximize, close)
- Draggable region using `data-tauri-drag-region`

**File: `src/main/webapp/app/shared/titlebar/titlebar.component.scss`**

- Modern gradient design
- Smooth hover/active transitions
- Fixed positioning at top of window
- Alternative color schemes (commented out)

---

## Configuration

### Tauri Configuration

**Files Updated:**

- `src-tauri/tauri.conf.json`
- `src-tauri/tauri.bundled.conf.json`

**Change:**

```json
{
  "app": {
    "windows": [
      {
        "decorations": false // Removes default titlebar
      }
    ]
  }
}
```

### Capabilities Configuration (Required for Multi-Window Support)

**File:** `src-tauri/capabilities/default.json`

**IMPORTANT:** To allow creating new windows dynamically, the capabilities must apply to all windows:

```json
{
  "windows": ["*"], // Apply to all windows (not just "main")
  "permissions": [
    "core:window:allow-create",
    "core:webview:allow-create-webview-window"
    // ... other permissions
  ]
}
```

If `"windows"` is restricted to `["main"]`, the new instance button will fail silently because dynamically created windows won't have the necessary permissions.

### Integration

**File: `src/main/webapp/app/layouts/main/main.component.html`**

Added titlebar at the top:

```html
<app-titlebar></app-titlebar>
```

**File: `src/main/webapp/app/layouts/main/main.component.ts`**

Imported and registered:

```typescript
import { TitlebarComponent } from 'app/shared/titlebar/titlebar.component';

@Component({
  imports: [..., TitlebarComponent]
})
```

---

## Design

### Default Theme (Gradient Purple)

```scss
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
color: white;
height: 32px;
```

### Layout

```
┌───────────────────────────────────────────────────────────────────┐
│ 📦 PharmaSmart - Ventes        [+]        [ _ ] [ □ ] [ × ]     │
└───────────────────────────────────────────────────────────────────┘
     ↑          ↑                  ↑              ↑
  App Icon   Current Route   New Instance   Window Controls
```

**Route Display:**

- Shows clean, user-friendly route names (e.g., "Ventes" instead of "sales")
- Query parameters are automatically removed (e.g., `/sales?page=0&size=10...` → "Ventes")
- Supports French and English route mappings
- Falls back to formatted route name if not mapped

### Titlebar Actions

| Button           | Icon  | Action           | Description                      |
| ---------------- | ----- | ---------------- | -------------------------------- |
| **New Instance** | `[+]` | Opens new window | Creates a new independent window |

### Window Controls

| Button       | Symbol    | Action                   | Hover Color   |
| ------------ | --------- | ------------------------ | ------------- |
| **Minimize** | `—`       | Minimizes window         | Light white   |
| **Maximize** | `□` / `⧉` | Toggles maximize/restore | Light white   |
| **Close**    | `×`       | Closes application       | Red (#e81123) |

---

## Customization

### Change Theme Colors

Edit `titlebar.component.scss`:

#### Option 1: macOS Style

```scss
.titlebar {
  background: #ececec;
  color: #333;
  border-bottom: 1px solid #d1d1d1;
}
```

#### Option 2: Dark Mode

```scss
.titlebar {
  background: #1e1e1e;
  color: #cccccc;
  border-bottom: 1px solid #333;
}
```

#### Option 3: Minimal/Flat

```scss
.titlebar {
  background: #ffffff;
  color: #333333;
  border-bottom: 1px solid #e0e0e0;
}
```

### Change Height

Default: `32px`

**Update in 3 places:**

1. `titlebar.component.scss`:

```scss
.titlebar {
  height: 40px; // Change here
}
```

2. `main.component.scss`:

```scss
.sticky-navbar {
  top: 40px; // Change here
}

.main-content {
  padding-top: 40px; // Change here
}
```

### Change App Icon

Edit `titlebar.component.html`, replace the SVG:

```html
<div class="app-icon">
  <!-- Your custom icon here -->
  <img src="assets/images/logo.png" alt="PharmaSmart" />
</div>
```

### Hide Route Name

Edit `titlebar.component.html`, remove:

```html
<span class="route-divider">-</span> <span class="current-route">{{ currentRoute }}</span>
```

### Add Custom Route Mappings

Edit `titlebar.component.ts`, add entries to `routeNameMap`:

```typescript
const routeNameMap: { [key: string]: string } = {
  sales: 'Ventes',
  customers: 'Clients',
  'my-custom-route': 'Mon Module', // Add your route here
  // ...
};
```

**Examples:**

- `'account'` → `'Compte'`
- `'admin'` → `'Administration'`
- `'customer-detail'` → `'Détails Client'`

---

## Browser Compatibility

The custom titlebar only appears in **Tauri mode**. In web browser mode:

- Titlebar is automatically hidden
- Browser's native titlebar is used
- No layout adjustments needed

**Detection:**

```typescript
if (typeof window !== 'undefined' && '__TAURI__' in window) {
  this.isTauri = true; // Show titlebar
}
```

---

## Window Control Functions

### Open New Instance

```typescript
async openNewInstance(): Promise<void> {
  const { WebviewWindow } = await import('@tauri-apps/api/webviewWindow');

  // Generate unique label
  const timestamp = Date.now();
  const label = `pharmasmart-${timestamp}`;

  // Create new window
  const newWindow = new WebviewWindow(label, {
    url: '/',
    title: 'PharmaSmart',
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 768,
    center: true,
    decorations: false,
    resizable: true,
  });

  // Listen for events
  newWindow.once('tauri://created', () => {
    console.log('New instance created');
  });
}
```

**Features:**

- Each window is independent
- All windows share the same backend connection
- Windows can be closed individually
- No limit on number of instances
- Each window has its own navigation state

### Minimize Window

```typescript
async minimizeWindow(): Promise<void> {
  const { getCurrentWindow } = await import('@tauri-apps/api/window');
  await getCurrentWindow().minimize();
}
```

### Maximize/Restore Window

```typescript
async maximizeWindow(): Promise<void> {
  const { getCurrentWindow } = await import('@tauri-apps/api/window');
  const appWindow = getCurrentWindow();

  if (this.isMaximized) {
    await appWindow.unmaximize();
  } else {
    await appWindow.maximize();
  }
}
```

### Close Window

```typescript
async closeWindow(): Promise<void> {
  const { getCurrentWindow } = await import('@tauri-apps/api/window');
  await getCurrentWindow().close();
}
```

---

## Draggable Region

The titlebar is draggable using Tauri's `data-tauri-drag-region` attribute:

```html
<div class="titlebar" data-tauri-drag-region>
  <!-- Most of titlebar is draggable -->
</div>
```

**Exception:** Window control buttons are NOT draggable:

```html
<div class="titlebar-right">
  <!-- No drag attribute here -->
  <button class="titlebar-button">...</button>
</div>
```

---

## Troubleshooting

### Titlebar Not Showing

**Check:**

1. Running in Tauri mode? (Not web browser)
2. `decorations: false` in `tauri.conf.json`?
3. Component imported in `main.component.ts`?
4. `<app-titlebar>` added to `main.component.html`?

**Verify Tauri Detection:**

```typescript
console.log('Tauri:', '__TAURI__' in window);
```

### Window Can't Be Moved

**Check:**

1. Titlebar has `data-tauri-drag-region` attribute
2. Not dragging on window control buttons
3. `-webkit-app-region: drag` in CSS

### Buttons Not Working

**Check:**

1. Click events attached correctly
2. Tauri window APIs imported
3. Running in Tauri mode (not web)

**Debug:**

```typescript
async minimizeWindow(): Promise<void> {
  console.log('Minimize clicked, isTauri:', this.isTauri);
  // ... rest of code
}
```

### New Instance Button Not Working

**Most Common Cause:** Capabilities not configured correctly

**Check:**

1. Open `src-tauri/capabilities/default.json`
2. Verify `"windows": ["*"]` (not `["main"]`)
3. Verify permissions include:
   - `"core:window:allow-create"`
   - `"core:webview:allow-create-webview-window"`

**Test:**

```bash
# Restart dev mode after changing capabilities
npm run tauri:dev
```

**Debug in browser console:**

- Click the [+] button
- Look for "New instance created: pharmasmart-{timestamp}"
- If you see errors, check the exact error message

### Layout Shifted/Overlapped

**Check:**

1. `padding-top: 32px` in `main.component.scss`
2. `sticky-navbar` has `top: 32px`
3. Titlebar `z-index: 9999`

---

## Testing

### Test in Development Mode

```bash
npm run tauri:dev
```

**What to test:**

- [ ] Titlebar appears at top
- [ ] App title displays correctly
- [ ] Current route updates when navigating
- [ ] Can drag window by titlebar
- [ ] New instance button works
- [ ] Multiple windows can be opened
- [ ] Each window navigates independently
- [ ] Minimize button works
- [ ] Maximize/restore button works
- [ ] Close button closes app
- [ ] Hover effects on buttons
- [ ] Layout doesn't overlap content

### Test in Production Build

```bash
npm run tauri:build
```

Run the built executable and verify all features work.

---

## Performance

**Impact:**

- ✅ Minimal: ~5KB JavaScript (compressed)
- ✅ No runtime overhead
- ✅ Conditional rendering (Tauri only)
- ✅ Lazy-loaded Tauri APIs

**Optimization:**

- Dynamic imports for Tauri APIs
- Simple SVG icons (no image files)
- Minimal CSS (< 200 lines)

---

## Accessibility

### Keyboard Navigation

Window controls support keyboard navigation:

- `Tab` to focus buttons
- `Enter` or `Space` to activate
- `Escape` to close window (if focused on close button)

### Screen Readers

Add ARIA labels for better accessibility:

```html
<button aria-label="Minimize window" class="titlebar-button minimize" (click)="minimizeWindow()">
  <svg>...</svg>
</button>
```

---

## Future Enhancements

### Use Cases for Multiple Instances

**Why multiple windows?**

1. **Compare Data**: View sales from different periods side-by-side
2. **Multi-tasking**: Work on customer orders in one window, inventory in another
3. **Reference**: Keep one window as reference while working in another
4. **Different Contexts**: Separate windows for different stores/locations
5. **Workflow**: Sales in one window, invoicing in another

**Example Workflows:**

```
Window 1: Sales Dashboard      Window 2: Customer Details
Window 3: Inventory Management Window 4: Reports
```

### Suggested Improvements

- [ ] **Context menu** on titlebar (right-click)
- [ ] **App menu** (File, Edit, View, etc.)
- [ ] **Search bar** in titlebar
- [ ] **User profile** icon/menu
- [ ] **Theme switcher** button
- [ ] **Breadcrumbs** for navigation
- [ ] **Notification badges**
- [ ] **Quick actions** toolbar
- [ ] **Window list menu** - Show all open windows
- [ ] **Restore all windows** - Reopen last session

### Advanced Features

- [ ] **Custom title** per route (via route data)
- [ ] **Double-click** to maximize/restore
- [ ] **Animations** on maximize/minimize
- [ ] **Transparency/blur** effects
- [ ] **Multi-window** support with window list
- [ ] **System tray** integration

---

## Related Files

```
src/
├── main/webapp/app/
│   ├── shared/titlebar/
│   │   ├── titlebar.component.ts       # Component logic
│   │   ├── titlebar.component.html     # Template
│   │   └── titlebar.component.scss     # Styles
│   └── layouts/main/
│       ├── main.component.html         # Integration
│       ├── main.component.ts           # Import
│       └── main.component.scss         # Layout adjustments
└── tauri/
    ├── tauri.conf.json                 # Tauri config (standard)
    └── tauri.bundled.conf.json         # Tauri config (bundled)
```

---

## Resources

- **Tauri Window API**: https://v2.tauri.app/reference/javascript/api/namespacew
- **Custom Titlebar Guide**: https://v2.tauri.app/develop/user-interface/window-customization/
- **Draggable Regions**: https://v2.tauri.app/develop/user-interface/window-customization/#draggable-region

---

_Last Updated: 2025-01-16_
