# Spring Boot Configuration via config.json

## Overview

PharmaSmart Tauri standalone application now supports **full customization of Spring Boot application properties** through the `config.json` file. All configuration changes are passed as command-line arguments to Spring Boot, overriding the default values in `application.yml` and `application-standalone.yml`.

## Benefits

- **No recompilation needed**: Change any Spring Boot property without rebuilding
- **Production-ready**: Users can customize file paths, API endpoints, and credentials
- **Version-controlled defaults**: The bundled `config.default.json` provides documented defaults
- **Override hierarchy**: config.json → command-line args → application-standalone.yml → application.yml

## Configuration Structure

The `config.json` file is located next to the PharmaSmart executable and contains the following sections:

```json
{
  "server": { ... },
  "logging": { ... },
  "jvm": { ... },
  "file": { ... },
  "fne": { ... },
  "mail": { ... },
  "port-com": { ... }
}
```

## File Paths Configuration

Configure all file storage locations for reports, images, and import data.

### Configuration

```json
{
  "file": {
    "report": "./reports",
    "images": "./images",
    "import": {
      "json": "./json",
      "csv": "./csv",
      "excel": "./excel"
    },
    "pharmaml": "pharmaml"
  }
}
```

### Spring Boot Properties

These map to the following Spring Boot properties:

```yaml
file:
  report: ./reports
  images: ./images
  import:
    json: ./json
    csv: ./csv
    excel: ./excel
  pharmaml: pharmaml
```

### Use Cases

**Example 1: Store files on network drive**

```json
{
  "file": {
    "report": "Z:/PharmaSmart/Reports",
    "images": "Z:/PharmaSmart/Images",
    "import": {
      "json": "Z:/PharmaSmart/Import/JSON",
      "csv": "Z:/PharmaSmart/Import/CSV",
      "excel": "Z:/PharmaSmart/Import/Excel"
    },
    "pharmaml": "pharmaml"
  }
}
```

**Example 2: Absolute Windows paths**

```json
{
  "file": {
    "report": "C:/ProgramData/PharmaSmart/Reports",
    "images": "C:/ProgramData/PharmaSmart/Images",
    "import": {
      "json": "C:/ProgramData/PharmaSmart/Import/JSON",
      "csv": "C:/ProgramData/PharmaSmart/Import/CSV",
      "excel": "C:/ProgramData/PharmaSmart/Import/Excel"
    },
    "pharmaml": "pharmaml"
  }
}
```

**Note:** Use forward slashes `/` even on Windows (Spring Boot handles path conversion).

## FNE Configuration

Configure the FNE (Feuille de Norme d'Envoi) service for French pharmacy electronic invoicing.

### Configuration

```json
{
  "fne": {
    "url": "http://54.247.95.108/ws/external/invoices/sign",
    "api-key": "nSXimInFusKqICZaJ95QZvQT85FOZvHW",
    "point-of-sale": ""
  }
}
```

### Spring Boot Properties

```yaml
fne:
  url: http://54.247.95.108/ws/external/invoices/sign
  api-key: nSXimInFusKqICZaJ95QZvQT85FOZvHW
  point-of-sale: ""
```

### Fields

- **url**: FNE web service endpoint for invoice signing
- **api-key**: Authentication key for FNE service
- **point-of-sale**: Point of sale identifier (optional)

### Use Cases

**Example: Production FNE endpoint**

```json
{
  "fne": {
    "url": "https://production.fne.fr/ws/external/invoices/sign",
    "api-key": "YOUR_PRODUCTION_API_KEY_HERE",
    "point-of-sale": "PHARMACY_001"
  }
}
```

**Example: Test/Staging environment**

```json
{
  "fne": {
    "url": "http://staging.fne.fr/ws/external/invoices/sign",
    "api-key": "YOUR_STAGING_API_KEY_HERE",
    "point-of-sale": ""
  }
}
```

## Mail Configuration

Configure email settings for SMTP communication.

### Configuration

```json
{
  "mail": {
    "username": "easyshopws@gmail.com",
    "email": "badoukobena@gmail.com"
  }
}
```

### Spring Boot Properties

```yaml
spring:
  mail:
    username: easyshopws@gmail.com

mail:
  email: badoukobena@gmail.com
```

### Fields

- **username**: SMTP authentication username (also used as sender email)
- **email**: Default email address for notifications/replies

### Use Cases

**Example: Custom SMTP account**

```json
{
  "mail": {
    "username": "pharmacy@yourdomain.com",
    "email": "support@yourdomain.com"
  }
}
```

**Example: Office 365 email**

```json
{
  "mail": {
    "username": "pharmacy@yourcompany.onmicrosoft.com",
    "email": "noreply@yourcompany.com"
  }
}
```

**Note:** Additional SMTP settings (host, port, password) are configured in `application.yml` and are not currently exposed in `config.json`.

## Port-Com Configuration

Configure legacy communication URLs for external systems.

### Configuration

```json
{
  "port-com": {
    "legacy-url": "http://localhost:9090/laborex"
  }
}
```

### Spring Boot Properties

```yaml
port-com:
  legacy-url: http://localhost:9090/laborex
```

### Fields

- **legacy-url**: URL for legacy system integration (e.g., external lab system)

### Use Cases

**Example: Remote lab server**

```json
{
  "port-com": {
    "legacy-url": "http://192.168.1.100:9090/laborex"
  }
}
```

**Example: Cloud-based lab system**

```json
{
  "port-com": {
    "legacy-url": "https://lab-api.example.com/laborex"
  }
}
```

## Complete Configuration Example

Here's a complete `config.json` with all sections configured:

```json
{
  "server": {
    "port": 9080
  },
  "logging": {
    "directory": "./logs",
    "file": "./logs/pharmasmart.log"
  },
  "installation": {
    "directory": ""
  },
  "jvm": {
    "heap_min": "2g",
    "heap_max": "2g",
    "metaspace_size": "256m",
    "metaspace_max": "384m",
    "direct_memory_size": "384m",
    "max_gc_pause_millis": "200",
    "additional_options": []
  },
  "file": {
    "report": "./reports",
    "images": "./images",
    "import": {
      "json": "./json",
      "csv": "./csv",
      "excel": "./excel"
    },
    "pharmaml": "pharmaml"
  },
  "fne": {
    "url": "http://54.247.95.108/ws/external/invoices/sign",
    "api-key": "nSXimInFusKqICZaJ95QZvQT85FOZvHW",
    "point-of-sale": ""
  },
  "mail": {
    "username": "easyshopws@gmail.com",
    "email": "badoukobena@gmail.com"
  },
  "port-com": {
    "legacy-url": "http://localhost:9090/laborex"
  }
}
```

## How It Works

### 1. Configuration Loading

When PharmaSmart starts, the Tauri backend manager (`src-tauri/src/backend_manager.rs`):

1. Reads `config.json` from the installation directory
2. If not found, creates it from `config.default.json`
3. Parses the JSON into Rust configuration structs (`src-tauri/src/config.rs`)

### 2. Argument Building

The backend manager builds Spring Boot command-line arguments:

```rust
// File configuration
args.push("--file.report=./reports");
args.push("--file.images=./images");
args.push("--file.import.json=./json");
// ... etc

// FNE configuration
args.push("--fne.url=http://...");
args.push("--fne.api-key=...");

// Mail configuration
args.push("--spring.mail.username=...");
args.push("--mail.email=...");

// Port-Com configuration
args.push("--port-com.legacy-url=...");
```

### 3. Spring Boot Launch

The Java process is launched with these arguments:

```bash
java -Xms2g -Xmx2g ... -jar pharmaSmart.jar \
  --spring.profiles.active=standalone,tauri,prod \
  --server.port=9080 \
  --file.report=./reports \
  --file.images=./images \
  --fne.url=http://... \
  --spring.mail.username=... \
  # ... etc
```

### 4. Property Override

Spring Boot applies configuration in this order (lowest to highest priority):

1. `application.yml` (default values)
2. `application-standalone.yml` (standalone profile)
3. `application-tauri.yml` (Tauri profile)
4. `application-prod.yml` (production profile)
5. **Command-line arguments** (from `config.json`) ← Highest priority

This means values in `config.json` **always override** YAML configuration files.

## Implementation Details

### Rust Configuration Structs

Located in `src-tauri/src/config.rs`:

```rust
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileImportConfig {
    pub json: String,
    pub csv: String,
    pub excel: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileConfig {
    pub report: String,
    pub images: String,
    pub import: FileImportConfig,
    pub pharmaml: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FneConfig {
    pub url: String,
    #[serde(rename = "api-key")]
    pub api_key: String,
    #[serde(rename = "point-of-sale")]
    pub point_of_sale: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MailConfig {
    pub username: String,
    pub email: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortComConfig {
    #[serde(rename = "legacy-url")]
    pub legacy_url: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub logging: LoggingConfig,
    pub installation: InstallationConfig,
    pub jvm: JvmConfig,
    pub file: FileConfig,
    pub fne: FneConfig,
    pub mail: MailConfig,
    #[serde(rename = "port-com")]
    pub port_com: PortComConfig,
}
```

### Default Values

Default values are defined as functions in `config.rs`:

```rust
// File configuration defaults
fn default_report_dir() -> String { "./reports".to_string() }
fn default_images_dir() -> String { "./images".to_string() }
fn default_json_dir() -> String { "./json".to_string() }
fn default_csv_dir() -> String { "./csv".to_string() }
fn default_excel_dir() -> String { "./excel".to_string() }
fn default_pharmaml() -> String { "pharmaml".to_string() }

// FNE configuration defaults
fn default_fne_url() -> String {
    "http://54.247.95.108/ws/external/invoices/sign".to_string()
}
fn default_fne_api_key() -> String {
    "nSXimInFusKqICZaJ95QZvQT85FOZvHW".to_string()
}
fn default_fne_point_of_sale() -> String { String::new() }

// Mail configuration defaults
fn default_mail_username() -> String {
    "easyshopws@gmail.com".to_string()
}
fn default_mail_email() -> String {
    "badoukobena@gmail.com".to_string()
}

// Port-Com configuration defaults
fn default_legacy_url() -> String {
    "http://localhost:9090/laborex".to_string()
}
```

## Troubleshooting

### Configuration Not Applied

**Problem:** Modified `config.json` but changes not reflected in application

**Solution:**
1. Ensure `config.json` is in the same directory as `PharmaSmart.exe`
2. Check JSON syntax is valid (use https://jsonlint.com/)
3. **Restart PharmaSmart completely** (configuration is only loaded on startup)
4. Check console/logs for "Configuration loaded successfully" message

### Invalid JSON Format

**Problem:** Application fails to start after editing `config.json`

**Solution:**
1. Validate JSON at https://jsonlint.com/
2. Common issues:
   - Trailing commas (not allowed in JSON)
   - Missing quotes around strings
   - Incorrect use of single quotes (must use double quotes)
3. If corrupted, delete `config.json` - it will be recreated from `config.default.json`

### File Paths Not Working

**Problem:** Application can't find files at configured paths

**Solution:**
1. Use forward slashes `/` even on Windows
2. Ensure directories exist (create them manually if needed)
3. Check permissions (application must have write access)
4. Use absolute paths if relative paths don't work:
   ```json
   "report": "C:/PharmaSmart/Reports"
   ```

### FNE Service Errors

**Problem:** Invoice signing fails

**Solution:**
1. Verify `fne.url` is accessible (test in browser)
2. Check `fne.api-key` is correct (contact FNE provider)
3. Ensure network allows outbound HTTP requests
4. Check firewall settings

### Mail Configuration Issues

**Problem:** Emails not sending

**Solution:**
1. Verify `mail.username` matches SMTP credentials in `application.yml`
2. Check SMTP host/port/password configuration in `application.yml`
3. For Gmail, ensure "Less secure app access" is enabled or use App Password
4. Check server logs for detailed SMTP errors

## Best Practices

### 1. Keep Defaults in config.default.json

Never modify `config.default.json` - it serves as documentation and template.

### 2. Backup config.json

Before making changes:
```bash
copy config.json config.json.backup
```

### 3. Use Environment-Specific Configs

For multiple environments, maintain separate config files:

```
config.production.json
config.staging.json
config.development.json
```

Copy the appropriate one to `config.json` when deploying.

### 4. Document Custom Values

Add comments (in a separate README) explaining why you changed default values.

### 5. Test After Changes

1. Edit `config.json`
2. Save file
3. Restart PharmaSmart
4. Verify in logs: "Configuration loaded successfully"
5. Test affected functionality

### 6. Keep Sensitive Data Secure

The `config.json` contains sensitive information:
- FNE API keys
- Email credentials (username)
- File paths

Ensure proper file permissions and don't commit to version control.

## Related Documentation

- **[HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md)** - General backend configuration guide
- **[CUSTOMIZE-JVM-OPTIONS.md](CUSTOMIZE-JVM-OPTIONS.md)** - JVM memory and performance tuning
- **[JVM-QUICK-START.md](JVM-QUICK-START.md)** - 5-minute JVM setup guide
- **[TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md)** - Technical Tauri integration details

## Summary

PharmaSmart's configuration system provides:

✅ **Full customization** without recompilation
✅ **Production-ready** configuration management
✅ **Override hierarchy** for flexible deployments
✅ **Default templates** for easy setup
✅ **Type-safe** Rust configuration parsing
✅ **Command-line arguments** passed to Spring Boot

All Spring Boot application properties can now be customized via `config.json` for maximum deployment flexibility.
