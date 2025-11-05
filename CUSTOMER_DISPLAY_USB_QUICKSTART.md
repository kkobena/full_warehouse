# Customer Display USB Connectivity - Quick Start Guide

## What's New

The CustomerDisplayEscPosServiceImpl now supports **three connection types**:
1. **Serial Port** (USB-to-Serial) - Traditional COM port connection
2. **USB Print Service** - Direct USB connection for displays that register as printers
3. **Network** - TCP/IP socket connection for Ethernet/WiFi displays

## Files Added

### Core Implementation
- `CustomerDisplayEscPosServiceImpl.java` - Main service with USB support
- `CustomerDisplayResource.java` - REST API for testing and management

### Documentation & Testing
- `CUSTOMER_DISPLAY_CONFIGURATION.md` - Detailed configuration guide
- `CUSTOMER_DISPLAY_USB_QUICKSTART.md` - This file
- `test-customer-display.bat` - Windows testing script
- `test-customer-display.sh` - Linux/Mac testing script

## Quick Setup

### Step 1: Identify Your Connection Type

#### USB-to-Serial (Most Common)
If your USB display creates a COM port:

```yaml
# application.yml or application-dev.yml
port-com: COM3  # Windows
# port-com: /dev/ttyUSB0  # Linux

customer-display:
  connection-type: SERIAL
```

#### USB Print Service
If your USB display registers as a printer:

```yaml
port-com: ""

customer-display:
  connection-type: USB_PRINT_SERVICE
  usb-printer-name: "Customer Display VFD"
```

#### Network
For Ethernet/WiFi displays:

```yaml
port-com: ""

customer-display:
  connection-type: NETWORK
```

### Step 2: Find Your Device

#### Windows

**List USB Displays:**
```bash
test-customer-display.bat list-usb
```

**List Serial Ports:**
```bash
test-customer-display.bat list-serial
```

**Or use PowerShell:**
```powershell
# List printers/displays
Get-Printer | Select-Object Name

# List COM ports
Get-WmiObject Win32_SerialPort | Select-Object Name, DeviceID
```

#### Linux/Mac

**List USB Displays:**
```bash
./test-customer-display.sh list-usb
```

**List Serial Ports:**
```bash
./test-customer-display.sh list-serial
# Or directly:
ls /dev/tty*
```

### Step 3: Configure application.yml

Example for USB Print Service:

```yaml
# src/main/resources/application-dev.yml

port-com: ""

customer-display:
  connection-type: USB_PRINT_SERVICE
  usb-printer-name: "EPSON VFD Display"  # Use exact name from list-usb
```

Example for USB-to-Serial (COM port):

```yaml
# src/main/resources/application-dev.yml

port-com: COM3  # Or /dev/ttyUSB0 on Linux

customer-display:
  connection-type: SERIAL
```

### Step 4: Test Connection

#### Start Backend

**Windows:**
```bash
mvnw.cmd
```

**Linux/Mac:**
```bash
./mvnw
```

#### Run Tests

**Windows:**
```bash
# Full test suite
test-customer-display.bat full-test

# Individual tests
test-customer-display.bat test
test-customer-display.bat welcome
test-customer-display.bat message "HELLO" "WORLD"
```

**Linux/Mac:**
```bash
chmod +x test-customer-display.sh

# Full test suite
./test-customer-display.sh full-test

# Individual tests
./test-customer-display.sh test
./test-customer-display.sh welcome
./test-customer-display.sh message "HELLO" "WORLD"
```

## API Endpoints

All endpoints are available at `/api/customer-display`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/usb-devices` | List USB displays |
| GET | `/serial-ports` | List serial ports |
| GET | `/test` | Test connection |
| POST | `/clear` | Clear display |
| POST | `/welcome` | Show welcome message |
| POST | `/test-message` | Display custom message |
| POST | `/brightness` | Set brightness (1-4) |
| POST | `/display-total` | Display sale total |
| POST | `/display-change` | Display change |
| POST | `/reset` | Reset display |

### Examples Using curl

```bash
# List USB devices
curl http://localhost:8080/api/customer-display/usb-devices

# Test connection
curl http://localhost:8080/api/customer-display/test

# Display custom message
curl -X POST "http://localhost:8080/api/customer-display/test-message?line1=HELLO&line2=WORLD"

# Display sale total
curl -X POST "http://localhost:8080/api/customer-display/display-total?total=15000"

# Set brightness
curl -X POST "http://localhost:8080/api/customer-display/brightness?level=4"
```

## Programmatic Usage

### Basic Display Operations

```java
@Autowired
private CustomerDisplayEscPosServiceImpl displayService;

// Show welcome message
displayService.welcomeMessage();

// Display sale data
displayService.displaySalesData("ASPIRIN 500MG", 2, 2500);

// Display total
displayService.displaySaleTotal(15000);

// Display change
displayService.displayMonnaie(5000);

// Clear display
displayService.clearDisplay();
```

### Advanced: Custom Connection

```java
import static com.kobe.warehouse.service.utils.CustomerDisplayEscPosServiceImpl.*;

// USB Print Service
DisplayConnectionConfig usbConfig =
    DisplayConnectionConfig.forUsbPrintService("Customer Display VFD");

// Serial Port
DisplayConnectionConfig serialConfig =
    DisplayConnectionConfig.forSerialPort("COM3", 9600);

// Network
DisplayConnectionConfig networkConfig =
    DisplayConnectionConfig.forNetworkDisplay("192.168.1.100", 9100);

// Build ESC/POS data
ByteArrayOutputStream out = new ByteArrayOutputStream();
// ... add ESC/POS commands ...

// Send with custom config
displayService.sendWithConfig(out.toByteArray(), usbConfig);
```

### Device Discovery

```java
// List all USB displays
List<String> usbDisplays = displayService.listAvailableUsbDisplays();
usbDisplays.forEach(System.out::println);

// List all serial ports
List<String> serialPorts = displayService.listAvailableSerialPorts();
serialPorts.forEach(System.out::println);

// Test connection
boolean connected = displayService.testConnection();
```

## Troubleshooting

### Problem: "USB display not found"

**Solution:**
1. Run device discovery:
   ```bash
   test-customer-display.bat list-usb
   ```
2. Copy the exact display name (case-sensitive)
3. Update `application.yml`:
   ```yaml
   customer-display:
     usb-printer-name: "Exact Display Name From List"
   ```

### Problem: Display works but shows garbled text

**Solution:**
- Check character encoding (Windows-1252 is used for French)
- Try resetting display:
  ```bash
  test-customer-display.bat reset
  ```
- Verify ESC/POS commands are compatible with your display model

### Problem: Connection test fails

**Solution:**
1. Verify display is powered on
2. Check USB cable connection
3. On Windows: Check Device Manager for driver issues
4. Try different connection type (SERIAL vs USB_PRINT_SERVICE)

### Problem: Linux permission denied

**Solution:**
```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Set permissions on serial port
sudo chmod 666 /dev/ttyUSB0

# Reload groups (or logout/login)
newgrp dialout
```

## Common Display Models & Settings

| Model | Connection | Config |
|-------|-----------|--------|
| Epson DM-D30 | USB Printer | `USB_PRINT_SERVICE` |
| Bixolon BCD-1100 | USB-Serial | `SERIAL` (COM port) |
| Star Micronics SCD222U | USB Printer | `USB_PRINT_SERVICE` |
| Logic Controls LD9900U | USB-Serial | `SERIAL` (COM port) |
| Aures OCD | USB Printer | `USB_PRINT_SERVICE` |
| Partner Tech CD-7220U | USB-Serial | `SERIAL` (COM port) |

## Testing Checklist

- [ ] Backend started successfully
- [ ] Devices listed with `list-usb` or `list-serial`
- [ ] Configuration updated in `application.yml`
- [ ] Connection test passes (`test-customer-display.bat test`)
- [ ] Welcome message displays correctly
- [ ] Custom messages display correctly
- [ ] Total and change amounts display correctly
- [ ] Display clears and resets properly

## Next Steps

1. **For USB-to-Serial displays**: Just configure the COM port in `application.yml`
2. **For USB Printer displays**: Find the exact printer name and configure it
3. **For Network displays**: Use programmatic methods with IP address

See `CUSTOMER_DISPLAY_CONFIGURATION.md` for detailed configuration options.

## Support

If you encounter issues:
1. Check the logs for detailed error messages
2. Verify the display works with manufacturer's software
3. Try the test scripts to isolate the problem
4. Check that ESC/POS commands are compatible with your display model

## Architecture

The implementation follows the same pattern as `AbstractJava2DReceiptPrinterService`:
- ESC/POS helper methods for low-level commands
- Multiple transport methods (Serial, USB, Network)
- Configuration class for flexible setup
- Automatic connection management
- Windows-1252 encoding for French character support

All ESC/POS commands are standard and should work with most VFD/customer pole displays.
