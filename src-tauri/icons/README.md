# Tauri Icons

This directory should contain application icons for Tauri.

## Required Icons

Place the following icon files in this directory:

- `32x32.png` - 32x32 pixel PNG icon
- `128x128.png` - 128x128 pixel PNG icon
- `128x128@2x.png` - 256x256 pixel PNG icon (for Retina displays)
- `icon.icns` - macOS icon file
- `icon.ico` - Windows icon file

## Generating Icons

You can use the Tauri CLI to generate icons from a single source image:

```bash
npm run tauri icon path/to/your/icon.png
```

Or use the existing favicon from the webapp:
```bash
npm run tauri icon src/main/webapp/favicon.ico
```

This will automatically generate all required icon sizes.

## Current Setup

Currently, icons need to be generated. Use the favicon.ico from `src/main/webapp/favicon.ico` as the source.
