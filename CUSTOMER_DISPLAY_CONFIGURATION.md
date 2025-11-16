# Customer Display Configuration Guide

This guide explains how to configure the ESC/POS Customer Display Service with different connection types.

## Overview

The `CustomerDisplayEscPosServiceImpl` supports three connection types:

1. **Serial Port** (COM port, USB-to-Serial)
2. **USB Print Service** (USB displays that register as printers)
3. **Network** (TCP/IP socket connection)

## Configuration in application.yml

### Serial Port Connection (Default)

For traditional serial ports or USB-to-Serial adapters:

```yaml
# Windows
port-com: COM3

# Linux
port-com: /dev/ttyUSB0

# Connection type (SERIAL, USB_PRINT_SERVICE, NETWORK)
customer-display:
  connection-type: SERIAL
```

### USB Print Service Connection

For USB customer displays that register as USB printers in the system:

```yaml
# Disable serial port if not using it
port-com: ''

# Configure USB connection
customer-display:
  connection-type: USB_PRINT_SERVICE
  usb-printer-name: 'Customer Display VFD' # Exact name from system
```

**Finding USB Printer Names:**

The service provides a method to list all available USB displays:

```java
@Autowired
private CustomerDisplayEscPosServiceImpl displayService;

// List all available USB displays/printers
List<String> usbDisplays = displayService.listAvailableUsbDisplays();
System.out.println("Available USB displays: " + usbDisplays);
```

On Windows, you can also check:

- Control Panel → Devices and Printers
- Device Manager → Printers

On Linux:

```bash
lpstat -p -d
```

### Network Connection

For network-connected customer displays (Ethernet/WiFi):

```yaml
port-com: ''

customer-display:
  connection-type: NETWORK
  # Network settings are configured programmatically
```

**Note:** Network displays require explicit method calls with IP/port:

```java
displayService.sendToNetworkDisplay(data, "192.168.1.100", 9100);
```

## Programmatic Configuration

### Using DisplayConnectionConfig

For dynamic configuration or multiple displays:

```java
import static com.kobe.warehouse.service.utils.CustomerDisplayEscPosServiceImpl.*;

// Serial Port Configuration
DisplayConnectionConfig serialConfig = DisplayConnectionConfig.forSerialPort("COM3", 9600);

// USB Print Service Configuration
DisplayConnectionConfig usbConfig = DisplayConnectionConfig.forUsbPrintService("Customer Display VFD");

// Network Configuration
DisplayConnectionConfig networkConfig = DisplayConnectionConfig.forNetworkDisplay("192.168.1.100", 9100);

// Send data using custom config
byte[] escPosData = ...; // Your ESC/POS commands
displayService.sendWithConfig(escPosData, usbConfig);
```

## USB Connection Types Explained

### 1. USB-to-Serial (Virtual COM Port)

**How it works:**

- Display has USB port but uses serial protocol internally
- USB driver creates a virtual COM port (e.g., COM3, /dev/ttyUSB0)
- Configure as `SERIAL` connection type

**Advantages:**

- Most common for customer displays
- Simple configuration
- Works with existing serial code

**Configuration:**

```yaml
port-com: COM3 # Or /dev/ttyUSB0 on Linux
customer-display:
  connection-type: SERIAL
```

### 2. USB Print Service (USB Printer)

**How it works:**

- Display registers as USB printer device
- Uses Java Print Service API
- No virtual COM port needed

**Advantages:**

- No driver installation required (uses generic USB printer driver)
- Cross-platform support
- Direct USB communication

**Configuration:**

```yaml
customer-display:
  connection-type: USB_PRINT_SERVICE
  usb-printer-name: 'POS Pole Display'
```

**Identifying USB Print Service Displays:**

Windows PowerShell:

```powershell
Get-Printer | Select-Object Name, DriverName, PortName
```

Linux:

```bash
lpinfo -v
lpstat -a
```

## Testing Connection

```java
// Test current connection
boolean isConnected = displayService.testConnection();
if (isConnected) {
    System.out.println("Display connected successfully!");
} else {
    System.out.println("Connection failed. Check configuration.");
}
```

## Discovering Available Devices

### List USB Displays

```java
List<String> usbDisplays = displayService.listAvailableUsbDisplays();
System.out.println("USB displays found:");
usbDisplays.forEach(System.out::println);
```

### List Serial Ports

```java
List<String> serialPorts = displayService.listAvailableSerialPorts();
System.out.println("Serial ports found:");
serialPorts.forEach(System.out::println);
```

## Example Configurations

### Example 1: Single USB Display (Print Service)

```yaml
# src/main/resources/application-dev.yml
port-com: ''

customer-display:
  connection-type: USB_PRINT_SERVICE
  usb-printer-name: 'EPSON VFD Display'
```

### Example 2: USB-to-Serial Display

```yaml
# Windows
port-com: COM4

# Linux
# port-com: /dev/ttyUSB0

customer-display:
  connection-type: SERIAL
```

### Example 3: Network Display

```yaml
port-com: ''

customer-display:
  connection-type: NETWORK
```

Then in code:

```java
ByteArrayOutputStream out = new ByteArrayOutputStream();
// Build ESC/POS commands...
displayService.sendToNetworkDisplay(out.toByteArray(), "192.168.1.50", 9100);
```

## Common USB Display Models

| Model                  | Connection Type   | Configuration         |
| ---------------------- | ----------------- | --------------------- |
| Epson DM-D30           | USB_PRINT_SERVICE | Use USB printer name  |
| Bixolon BCD-1100       | SERIAL            | USB creates COM port  |
| Star Micronics SCD222U | USB_PRINT_SERVICE | Use USB printer name  |
| Logic Controls LD9900U | SERIAL            | USB-to-Serial adapter |
| Aures OCD              | USB_PRINT_SERVICE | Use USB printer name  |
| Partner Tech CD-7220U  | SERIAL            | Virtual COM port      |

## Troubleshooting

### Issue: USB display not detected

**Solution:**

1. Check if driver is installed
2. Verify device appears in Device Manager (Windows) or `lsusb` (Linux)
3. Try listing available devices:
   ```java
   displayService.listAvailableUsbDisplays();
   displayService.listAvailableSerialPorts();
   ```

### Issue: "USB display not found" error

**Solution:**

1. Verify exact printer name (case-sensitive):
   ```java
   displayService.listAvailableUsbDisplays();
   ```
2. Update `usb-printer-name` in application.yml

### Issue: Connection works but no display

**Solution:**

1. Check display is powered on
2. Verify ESC/POS commands are correct for your display model
3. Some displays require initialization sequence:
   ```java
   displayService.resetDisplay();
   displayService.clearDisplay();
   ```

### Issue: Windows COM port changes

**Solution:**

- Fix COM port number in Device Manager:
  1. Device Manager → Ports (COM & LPT)
  2. Right-click device → Properties → Port Settings → Advanced
  3. Set fixed COM port number

### Issue: Linux permissions

**Solution:**
Add user to dialout group:

```bash
sudo usermod -a -G dialout $USER
sudo chmod 666 /dev/ttyUSB0
```

## Advanced Usage

### Multiple Displays

```java
// Display 1: USB
DisplayConnectionConfig display1 = DisplayConnectionConfig.forUsbPrintService("Display 1");

// Display 2: Serial
DisplayConnectionConfig display2 = DisplayConnectionConfig.forSerialPort("COM4");

// Send to different displays
displayService.sendWithConfig(data1, display1);
displayService.sendWithConfig(data2, display2);
```

### Custom Baud Rates

```java
// For high-speed serial displays
DisplayConnectionConfig config = DisplayConnectionConfig.forSerialPort("COM3", 115200);

```

## References

- [ESC/POS Command Reference](https://reference.epson-biz.com/modules/ref_escpos/index.php)
- [jSerialComm Documentation](https://fazecast.github.io/jSerialComm/)
- [Java Print Service Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/jps/spec/JPSTOC.fm.html)
