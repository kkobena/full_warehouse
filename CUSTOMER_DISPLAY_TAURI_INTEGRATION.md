# Customer Display Tauri Integration Guide

This guide explains how to use customer displays in the Tauri desktop application with ESC/POS commands.

## Overview

The Tauri integration allows the desktop app to communicate directly with customer displays (VFD/pole displays) entirely from the frontend - **no backend dependency required**. This provides:

- **True Offline Operation**: Display works without backend connection
- **Better Performance**: No HTTP overhead, direct hardware access
- **Smaller Payloads**: ~20-50 bytes of ESC/POS commands
- **Client-Side Control**: Each Tauri client controls its own local display

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Angular/TypeScript Frontend                  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CustomerDisplayEscPosService                             │  │
│  │  - Generates ESC/POS commands LOCALLY in TypeScript      │  │
│  │  - Sends raw bytes to Tauri via __TAURI__.core.invoke() │  │
│  │  - NO backend dependency                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Tauri IPC (JSON-RPC)
                            │ (ESC/POS byte array)
┌───────────────────────────▼─────────────────────────────────────┐
│                        Tauri Rust Backend                        │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  customer_display.rs                                      │  │
│  │  - send_to_customer_display()                            │  │
│  │  - Routes to Serial/USB/Network                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
    ┌─────▼──────┐   ┌─────▼──────┐   ┌─────▼──────┐
    │   Serial   │   │    USB     │   │  Network   │
    │   Port     │   │  (as COM)  │   │  TCP/IP    │
    └─────┬──────┘   └─────┬──────┘   └─────┬──────┘
          │                 │                 │
    ┌─────▼─────────────────▼─────────────────▼──────┐
    │         Customer Display (VFD/Pole Display)     │
    │              20 chars x 2 lines                 │
    └─────────────────────────────────────────────────┘
```

**Key Difference from Backend Approach:**
- ❌ OLD: Frontend → Backend REST API → Generate ESC/POS → Return to Frontend → Tauri → Display
- ✅ NEW: Frontend generates ESC/POS → Tauri → Display (no backend involved)

## Why Frontend Generation?

In a Tauri desktop app, the frontend and backend can run on **different machines**:
- Frontend (Tauri app) runs on the cashier's workstation with the display
- Backend (Spring Boot server) runs on a central server

Calling the backend to generate ESC/POS commands for a local display makes no sense because:
1. Backend doesn't know what display hardware is available on the client
2. Backend configuration is for the server's display, not the client's
3. Adds unnecessary network latency
4. Requires internet connection for a purely local operation

## Setup

### 1. Frontend TypeScript Service

Located in `src/main/webapp/app/shared/customer-display/customer-display-escpos.service.ts`

This service:
- Generates ESC/POS commands entirely in TypeScript
- Handles text formatting, padding, alignment
- Sends commands to Tauri via IPC

### 2. Tauri Rust Commands

Located in `src-tauri/src/customer_display.rs`:

```rust
#[tauri::command]
pub async fn send_to_customer_display(
    esc_pos_data: Vec<u8>,
    config: CustomerDisplayConnectionConfig,
) -> Result<String, String>

#[tauri::command]
pub async fn list_serial_ports() -> Result<Vec<String>, String>

#[tauri::command]
pub async fn test_customer_display_connection(
    config: CustomerDisplayConnectionConfig,
) -> Result<String, String>
```

## Usage in Angular/TypeScript

### Basic Example

```typescript
import { CustomerDisplayEscPosService, CustomerDisplayConnectionConfig } from '@shared/customer-display/customer-display-escpos.service';

export class CashRegisterComponent {
  private displayConfig: CustomerDisplayConnectionConfig = {
    connectionType: 'SERIAL',
    serialPort: 'COM3',
    baudRate: 9600,
  };

  constructor(private displayService: CustomerDisplayEscPosService) {}

  async onSaleComplete(total: number) {
    try {
      // Generate ESC/POS in frontend and send to display
      await this.displayService.displaySaleTotal(total, this.displayConfig);
    } catch (error) {
      console.error('Failed to update display:', error);
    }
  }

  async onProductAdded(product: Product, qty: number) {
    try {
      // Generate ESC/POS in frontend and send to display
      await this.displayService.displaySalesData(
        product.name,
        qty,
        product.price,
        this.displayConfig
      );
    } catch (error) {
      console.error('Failed to update display:', error);
    }
  }
}
```

### Configuration Examples

#### Serial Port (USB-to-Serial)
```typescript
const config: CustomerDisplayConnectionConfig = {
  connectionType: 'SERIAL',
  serialPort: 'COM3',      // Windows
  // serialPort: '/dev/ttyUSB0', // Linux
  baudRate: 9600,
};
```

#### USB (via Virtual COM Port)
```typescript
const config: CustomerDisplayConnectionConfig = {
  connectionType: 'USB',
  serialPort: 'COM4',  // USB creates virtual COM port
  baudRate: 9600,
};
```

#### Network (TCP/IP)
```typescript
const config: CustomerDisplayConnectionConfig = {
  connectionType: 'NETWORK',
  ipAddress: '192.168.1.100',
  port: 9100,
};
```

### Advanced Usage: Custom ESC/POS Commands

```typescript
// Generate ESC/POS data locally (no backend call)
const escPosData = this.displayService.generateTwoLines(
  'PROMO SPECIAL',
  '-50% REDUCTION',
  'center',
  'center'
);

// Send to display via Tauri
await this.displayService.sendToDisplayViaTauri(escPosData, this.displayConfig);
```

### Detect Tauri Environment

```typescript
export class AppComponent implements OnInit {
  isRunningInTauri = false;

  ngOnInit() {
    this.isRunningInTauri = !!(window as any).__TAURI__;

    if (this.isRunningInTauri) {
      console.log('Running in Tauri desktop app');
      // Customer display available
      this.initializeCustomerDisplay();
    } else {
      console.log('Running in browser');
      // Customer display not available
    }
  }

  private async initializeCustomerDisplay() {
    const config = {
      connectionType: 'SERIAL' as const,
      serialPort: 'COM3',
      baudRate: 9600,
    };

    try {
      // Get store name from application state or config
      const storeName = 'PHARMA SMART';
      await this.displayService.displayWelcomeMessage(storeName, config);
    } catch (error) {
      console.error('Failed to initialize display:', error);
    }
  }
}
```

## Available Methods

```typescript
// Generate ESC/POS commands (returns Uint8Array)
generateWelcomeMessage(storeName: string, welcomeText?: string): Uint8Array
generateSalesData(productName: string, qty: number, price: number): Uint8Array
generateSaleTotal(total: number): Uint8Array
generateChange(change: number): Uint8Array
generateUserMessage(userName: string): Uint8Array
generateTwoLines(line1: string, line2: string, align1?: string, align2?: string): Uint8Array
generateClearDisplay(): Uint8Array
generateResetDisplay(): Uint8Array
generateBrightness(level: number): Uint8Array

// High-level workflow methods (generate + send via Tauri)
displayWelcomeMessage(storeName: string, config: CustomerDisplayConnectionConfig): Promise<void>
displaySalesData(name: string, qty: number, price: number, config: ...): Promise<void>
displaySaleTotal(total: number, config: CustomerDisplayConnectionConfig): Promise<void>
displayChange(change: number, config: CustomerDisplayConnectionConfig): Promise<void>
displayUserMessage(userName: string, config: CustomerDisplayConnectionConfig): Promise<void>
displayTwoLines(line1: string, line2: string, config: ..., align1?: string, align2?: string): Promise<void>
clearDisplay(config: CustomerDisplayConnectionConfig): Promise<void>
resetDisplay(config: CustomerDisplayConnectionConfig): Promise<void>
setBrightness(level: number, config: CustomerDisplayConnectionConfig): Promise<void>

// Low-level method
sendToDisplayViaTauri(escPosData: Uint8Array, config: CustomerDisplayConnectionConfig): Promise<void>
```

## Testing

### 1. List Available Serial Ports

```typescript
const { invoke } = (window as any).__TAURI__.core;
const ports = await invoke('list_serial_ports');
console.log('Available ports:', ports);
```

### 2. Test Connection

```typescript
const { invoke } = (window as any).__TAURI__.core;
const config = {
  connectionType: 'SERIAL',
  serialPort: 'COM3',
  baudRate: 9600,
};

try {
  const result = await invoke('test_customer_display_connection', { config });
  console.log('Test result:', result);
} catch (error) {
  console.error('Test failed:', error);
}
```

### 3. Send Test Message

```typescript
import { CustomerDisplayEscPosService } from '@shared/customer-display/customer-display-escpos.service';

const displayService = inject(CustomerDisplayEscPosService);

const config = {
  connectionType: 'SERIAL',
  serialPort: 'COM3',
  baudRate: 9600,
};

await displayService.displayTwoLines('HELLO', 'WORLD', config);
```

## Building Tauri App

### Development Build

```bash
npm run tauri:dev
```

### Production Build

```bash
npm run tauri:build
```

The serial port support is enabled by default. See `src-tauri/Cargo.toml`:

```toml
[features]
default = ["serialport"]
serialport = ["dep:serialport"]
```

## Complete POS Workflow Example

```typescript
export class POSComponent {
  private config: CustomerDisplayConnectionConfig = {
    connectionType: 'SERIAL',
    serialPort: 'COM3',
    baudRate: 9600,
  };

  constructor(
    private displayService: CustomerDisplayEscPosService,
    private store: Store
  ) {}

  async ngOnInit() {
    // Show welcome on startup
    const storeName = this.store.selectSnapshot(state => state.magasin.name);
    await this.displayService.displayWelcomeMessage(storeName, this.config);
  }

  async onUserLogin(userName: string) {
    // Show connected user
    await this.displayService.displayUserMessage(userName, this.config);
  }

  async onProductScanned(product: Product, qty: number) {
    // Update display with product info
    await this.displayService.displaySalesData(
      product.name,
      qty,
      product.price,
      this.config
    );
  }

  async onCheckout(total: number) {
    // Show total
    await this.displayService.displaySaleTotal(total, this.config);
  }

  async onPaymentReceived(amountPaid: number, total: number) {
    const change = amountPaid - total;
    // Show change
    await this.displayService.displayChange(change, this.config);
  }

  async onSaleComplete() {
    // Clear display after delay
    setTimeout(async () => {
      await this.displayService.clearDisplay(this.config);
      const storeName = this.store.selectSnapshot(state => state.magasin.name);
      await this.displayService.displayWelcomeMessage(storeName, this.config);
    }, 3000);
  }
}
```

## Performance Comparison

| Aspect | Frontend Generation | Backend Generation (Wrong) |
|--------|-------------------|--------------------------|
| **Network calls** | 0 | 1 per operation |
| **Payload size** | 20-50 bytes | ~200B HTTP request + response |
| **Latency** | 5-10ms | 50-100ms+ |
| **Offline support** | ✅ Yes | ❌ No |
| **Server load** | 0 | High on busy systems |
| **Scalability** | Perfect | Poor |

## Troubleshooting

### Issue: "Tauri environment not detected"

**Solution:**
This service only works in Tauri desktop apps, not in browsers.
```typescript
if ((window as any).__TAURI__) {
  // Tauri app - display available
  await displayService.displayWelcomeMessage('Store', config);
} else {
  // Browser - display not available
  console.warn('Customer display only available in desktop app');
}
```

### Issue: COM port access denied (Windows)

**Solution:**
1. Close any other applications using the port
2. Check Device Manager for port conflicts
3. Verify the port name is correct (use `list_serial_ports`)

### Issue: Permission denied (Linux)

**Solution:**
```bash
sudo usermod -a -G dialout $USER
sudo chmod 666 /dev/ttyUSB0
newgrp dialout
```

### Issue: Display not responding

**Solution:**
1. Verify display is powered on
2. Check cable connections
3. Test with device discovery:
   ```typescript
   const ports = await invoke('list_serial_ports');
   console.log('Available:', ports);
   ```
4. Try different baud rates (9600, 19200, 38400, 115200)
5. Reset display using the reset method

## Security Considerations

1. **Hardware Access**: Tauri's permission system controls hardware access
2. **No Server-Side Processing**: Display operations happen client-side only
3. **Configuration Storage**: Store display config securely (consider encrypting serial port configs)
4. **Input Validation**: Service validates text length and formatting before sending

## Backend REST API (Server-Side Display Only)

The backend still has REST endpoints for managing a display **directly connected to the server**:

```
POST /api/customer-display/welcome
POST /api/customer-display/test-message?line1=X&line2=Y
POST /api/customer-display/display-total?total=15000
GET /api/customer-display/usb-devices
GET /api/customer-display/serial-ports
```

These are ONLY for scenarios where the Spring Boot server has its own display attached (e.g., central server with a status display). **Tauri clients should NOT use these endpoints** - they should generate ESC/POS commands locally.

## Summary

The correct architecture for Tauri customer display integration:

1. ✅ Frontend generates ESC/POS commands using `CustomerDisplayEscPosService`
2. ✅ Frontend sends commands to Tauri via `invoke('send_to_customer_display')`
3. ✅ Tauri Rust code sends bytes to local hardware (serial/USB/network)
4. ❌ Frontend does NOT call backend REST API for ESC/POS generation
5. ❌ Backend REST API is for server-side displays only

This approach provides true offline operation, better performance, and proper client-server separation.
