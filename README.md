# Pharma-Smart Warehouse Management System

A full-stack pharmacy warehouse management system built with Spring Boot (backend) and Angular (frontend). Manages pharmaceutical inventory, sales, customers, suppliers, invoicing, cash registers, and compliance with French pharmacy regulations.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development Setup](#development-setup)
- [Database Setup](#database-setup)
- [Building for Production](#building-for-production)
- [Desktop Application](#desktop-application)
- [Configuration](#configuration)
- [Documentation](#documentation)

## 🔧 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 25** - JDK with `JAVA_HOME` configured
- **Node.js 22.14.0+** with **npm 11.0.0+**
- **PostgreSQL** - Latest stable version
- **Maven** - Included via Maven wrapper (`mvnw`)
- **Git** - For version control

### Optional (for desktop application)

- **Rust toolchain** - For Tauri desktop builds (`rustup`, `cargo`)
- **Microsoft Visual C++ Build Tools** - Windows only
- **WebView2 Runtime** - Usually pre-installed on Windows 10/11

## 🚀 Quick Start

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Backend Server

**Windows (Command Prompt):**

```bash
mvnw.cmd
```

**Windows (Git Bash) / Linux / macOS:**

```bash
./mvnw
```

Backend will start on `http://localhost:8080`

### 3. Start Frontend Development Server

```bash
npm start
```

Frontend will start on `http://localhost:4200`

**That's it!** Open your browser and navigate to `http://localhost:4200`

## 💻 Development Setup

### Backend Development

The backend runs on Spring Boot with hot reload enabled by default.

```bash
# Run application in dev mode
./mvnw                          # or mvnw.cmd on Windows

# Skip frontend build during Maven build
./mvnw -Dskip.npm

# Run tests
./mvnw test

# Check Flyway migration status
./mvnw flyway:info

# Generate JavaDocs
./mvnw javadoc:javadoc
```

### Frontend Development

The frontend uses Angular with PrimeNG components.

```bash
# Start dev server with hot reload
npm start

# Run tests with coverage
npm test

# Run linter
npm run lint

# Fix linting issues
npm run lint:fix

# Format code with Prettier
npm run prettier:format

# Build for development
npm run webapp:build

# Build for production
npm run webapp:prod
```

## 🗄️ Database Setup

### PostgreSQL Installation

1. **Create database and user:**

```sql
CREATE DATABASE pharma_smart;
CREATE USER pharma_smart WITH PASSWORD 'warehouse2802';
GRANT ALL PRIVILEGES ON DATABASE pharma_smart TO pharma_smart;
```

2. **Connect to the database:**

```bash
psql -U postgres
\c pharma_smart
```

3. **Create schema:**

```sql
CREATE SCHEMA pharma_smart AUTHORIZATION pharma_smart;
GRANT ALL PRIVILEGES ON SCHEMA pharma_smart TO pharma_smart;
```

### PostgreSQL Configuration

Recommended settings based on available RAM:

#### For 8GB RAM Systems

Edit `postgresql.conf`:

```properties
# Memory
shared_buffers = 2GB
work_mem = 16MB
maintenance_work_mem = 256MB
effective_cache_size = 6GB

# Connections
max_connections = 150

# Write-Ahead Log (WAL)
wal_level = replica
wal_buffers = 8MB
checkpoint_completion_target = 0.9

# Autovacuum
autovacuum = on
autovacuum_naptime = 30s
autovacuum_vacuum_cost_limit = -1

# Parallelism
max_worker_processes = 4
max_parallel_workers = 4
max_parallel_workers_per_gather = 2

# Logging
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%Y-%m-%d.log'
log_statement = 'ddl'
log_min_duration_statement = 500
enable_partition_pruning = on
```

#### For 16GB RAM Systems

```properties
# Memory
shared_buffers = 4GB
work_mem = 32MB
maintenance_work_mem = 512MB
effective_cache_size = 12GB

# Connections
max_connections = 200

# WAL
wal_level = replica
wal_buffers = 16MB
checkpoint_completion_target = 0.9

# Autovacuum
autovacuum = on
autovacuum_naptime = 30s
autovacuum_vacuum_cost_limit = -1

# Parallelism
max_worker_processes = 8
max_parallel_workers = 8
max_parallel_workers_per_gather = 4

# Logging
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%Y-%m-%d.log'
log_statement = 'ddl'
log_min_duration_statement = 500
```

### Database Migrations

Flyway automatically runs database migrations on application startup.

- **Migrations location:** `src/main/resources/db/migration/`
- **Migration files:**
  - `V1.0.1__init.sql` - Initial schema
  - `V1.0.2__referentiels.sql` - Reference data
  - `V1.0.3__procedures.sql` - Stored procedures
  - `V1.0.4__menus.sql` - Menu/navigation structure
  - `V1.0.5__id_generator.sql` - ID generation functions

**Check migration status:**

```bash
./mvnw flyway:info
```

**Run pending migrations:**

```bash
./mvnw flyway:migrate
```

## 📦 Building for Production

### Full Production Build

```bash
# Build both backend and frontend
./mvnw clean package -Pprod
```

This creates:

- Backend JAR: `target/warehouse-*.jar`
- Frontend assets: Bundled in JAR at `target/classes/static/`

### Run Production Build

```bash
java -jar target/warehouse-*.jar --spring.profiles.active=prod
```

## 🖥️ Desktop Application

Pharma-Smart supports desktop deployment via **Tauri** for a native desktop experience.

### Tauri Desktop

**Modern, lightweight framework (Recommended)**

- Bundle size: ~5MB
- Rust backend
- Native webview
- Better security

#### Development

```bash
# Start backend first (Terminal 1)
./mvnw

# Start Tauri app (Terminal 2)
npm run tauri:dev
```

#### Build

```bash
# Production build
npm run tauri:build

# Debug build (faster compilation)
npm run tauri:build:debug

# Fast build (assumes frontend already built)
npm run tauri:build:fast
```

**Build outputs:** `src-tauri/target/release/bundle/` (NSIS/MSI installers for Windows)

For detailed Tauri setup and configuration, see [TAURI_README.md](TAURI_README.md)

## ⚙️ Configuration

### Spring Profiles

- **dev** (default): Development mode with Spring DevTools, hot reload, relaxed security
- **prod**: Production mode with optimized builds, strict security
- **tls**: Enable TLS/HTTPS
- **webapp**: Build frontend during Maven lifecycle

**Set profile:**

```bash
./mvnw -Dspring.profiles.active=prod
# or via environment variable
export SPRING_PROFILES_ACTIVE=prod
```

### Application Configuration

Main configuration file: `src/main/resources/application.yml`

Key settings:

- **Server port:** 8080 (backend)
- **Frontend dev port:** 4200
- **Database schema:** `warehouse`
- **Reports directory:** `reports/`
- **Email:** Gmail SMTP (requires credentials)

### Security Configuration

- **Authentication:** JWT OAuth2 Resource Server
- **Session:** Stateless (JWT-based)
- **Password encoding:** BCrypt
- **CSRF protection:** Enabled for web endpoints
- **Timezone:** UTC (for all timestamps)

## 📚 Documentation

For comprehensive documentation, see:

- **[CLAUDE.md](CLAUDE.md)** - Complete development guide covering:

  - Project architecture
  - Technology stack details
  - Code conventions and patterns
  - Frontend/Backend workflows
  - Testing strategies
  - Common pitfalls and debugging

- **[TAURI_README.md](TAURI_README.md)** - Tauri desktop application setup

### Key Technologies

**Backend:**

- Spring Boot 4.0.0-RC1
- Hibernate 7.1.0 with JPA
- PostgreSQL with Flyway migrations
- Spring Security with JWT
- Flying Saucer & OpenPDF for PDF generation
- jSerialComm for thermal printer integration

**Frontend:**

- Angular 20.3.7 with TypeScript 5.9.2
- PrimeNG 20.2.0 (UI components)
- ng-bootstrap 19.0.1
- AG Grid 34.3.0 (advanced tables)
- RxJS 7.8.2 (reactive programming)
- ngx-translate (internationalization)

### Project Structure

```
warehouse/
├── src/main/
│   ├── java/com/kobe/warehouse/    # Backend Java code
│   │   ├── domain/                  # JPA entities
│   │   ├── repository/              # Data access layer
│   │   ├── service/                 # Business logic
│   │   ├── web/rest/                # REST controllers
│   │   └── security/                # Security configuration
│   ├── resources/
│   │   ├── db/migration/            # Flyway migrations
│   │   ├── templates/               # Thymeleaf templates
│   │   └── application.yml          # Configuration
│   └── webapp/app/                  # Angular frontend
│       ├── entities/                # Feature modules
│       ├── layouts/                 # Application layout
│       ├── shared/                  # Shared components
│       └── core/                    # Core services
├── src-tauri/                       # Tauri desktop app
├── target/                          # Build output
└── reports/                         # Generated reports
```

## 🔍 Useful Commands

### Database

```bash
# Connect to database
psql -U pharma_smart -d pharma_smart

# List tables
\dt pharma_smart.*

# Describe table
\d pharma_smart.table_name

# List functions
\df pharma_smart.*
```

### Maven

```bash
# Clean and build
./mvnw clean package

# Run specific test
./mvnw test -Dtest=ClassName

# Skip tests
./mvnw package -DskipTests

# Integration tests
./mvnw verify -Pfailsafe
```

### npm

```bash
# Install specific package
npm install <package-name>

# Update dependencies
npm update

# Check for outdated packages
npm outdated
```

### Deployment — Distribution ZIP

Produit `target/pharmasmart-<version>-full.zip` contenant : installeur Tauri NSIS (bundled-jre),
`pharmasmart-batch.jar`, `pharmasmart-backup.exe`, `config.default.json`, scripts de service.

```bash
# Windows
mvnw.cmd clean package -P full-dist -DskipTests

# Linux / macOS
./mvnw clean package -P full-dist -DskipTests
```

Prérequis : Rust/`cargo` installé, Node.js + Tauri CLI (`npm install` fait).
Sortie : `target/pharmasmart-<version>-full.zip`

### Deployment — Service Installation (server, no Tauri)

```powershell
# Install all services (app + batch + backup tasks)
.\service\pharmasmart-setup.ps1 install

# Install with custom directories
.\service\pharmasmart-setup.ps1 install -AppDir "D:\PharmaSmart" -BatchDir "D:\PharmaSmartBatch"

# Install only the app service
.\service\pharmasmart-setup.ps1 install -Target app

# Install only the batch service
.\service\pharmasmart-setup.ps1 install -Target batch

# Install only the backup scheduled tasks
.\service\pharmasmart-setup.ps1 install -Target backup

# Uninstall all services (keeps files by default)
.\service\pharmasmart-setup.ps1 uninstall

# Uninstall and delete files
.\service\pharmasmart-setup.ps1 uninstall -Target all   # then remove dirs manually

# Check service status
.\service\pharmasmart-setup.ps1 status

# Update JVM heap settings (reads from config.json)
.\service\pharmasmart-setup.ps1 update-jvm

# Update JVM for a specific service only
.\service\pharmasmart-setup.ps1 update-jvm -Target app
.\service\pharmasmart-setup.ps1 update-jvm -Target batch

# Refresh service config after editing config.json (java_home, db credentials…)
.\service\pharmasmart-setup.ps1 refresh-config
```

### Deployment — Batch Standalone Installer

```powershell
# Install batch service (auto-reads config.json)
.\pharmaSmart-batch\service\install-batch-service.ps1

# Override JRE path
.\pharmaSmart-batch\service\install-batch-service.ps1 -JavaHome "C:\jdk25"

# Override heap sizes
.\pharmaSmart-batch\service\install-batch-service.ps1 -HeapMin 256m -HeapMax 1g

# Full override
.\pharmaSmart-batch\service\install-batch-service.ps1 `
    -InstallDir "D:\PharmaSmartBatch" `
    -JavaHome "C:\jdk25" `
    -HeapMin 256m -HeapMax 1g
```

### Deployment — Windows Service Management

```powershell
# Start / stop / restart services
Start-Service pharmasmart-app
Stop-Service  pharmasmart-app
Restart-Service pharmasmart-app

Start-Service pharmasmart-batch
Stop-Service  pharmasmart-batch

# Check service state
Get-Service pharmasmart-app, pharmasmart-batch | Select-Object Name, Status, StartType

# View last 50 log lines (app)
Get-Content "C:\PharmaSmartApp\logs\pharmasmart-app.out.log" -Tail 50

# View last 50 log lines (batch)
Get-Content "C:\PharmaSmartBatch\logs\pharmasmart-batch.out.log" -Tail 50

# Follow log in real time
Get-Content "C:\PharmaSmartApp\logs\pharmasmart-app.out.log" -Wait -Tail 20
```

### Deployment — Backup Scheduled Tasks

```powershell
# Register backup tasks (AtStartup + delay)
.\service\setup-backup-tasks.ps1

# With custom backup directory
.\service\setup-backup-tasks.ps1 -BackupDir "D:\Backups\PharmaSmart"

# Remove backup tasks
.\service\remove-backup-tasks.ps1

# List registered tasks
Get-ScheduledTask | Where-Object TaskName -like "PharmaSmart*" | Select-Object TaskName, State
```

### Deployment — JVM Tuning (config.json)

```json
// src-tauri/config.default.json — copy to config.json and edit
{
  "jvm": {
    "java_home": "C:\\jdk25",
    "app": {
      "heap_min": "2g",
      "heap_max": "2g"
    },
    "batch": {
      "heap_min": "128m",
      "heap_max": "512m"
    }
  }
}
```

```powershell
# After editing config.json, apply new JVM settings without reinstalling
.\service\pharmasmart-setup.ps1 update-jvm
# or
.\service\pharmasmart-setup.ps1 refresh-config
```

## 📝 Additional Notes

### Thermal Printer Support

- **Supported formats:**
  - Mobile terminals: 57mm × 50mm, 57mm × 40mm, 57mm × 38mm
  - Receipt printers: 80mm × 80mm, 80mm × 70mm, 80mm × 60mm
- **Integration:** jSerialComm 2.11.2
- **Printer code:** `com.kobe.warehouse.printer/` package

### Performance Tips

- Enable Hibernate second-level caching for frequently-used queries
- Use `@BatchSize` on entity collections to avoid N+1 queries
- Batch inserts/updates configured (batch size: 50)
- Frontend uses lazy loading for routes

### Common Issues

1. **Port already in use:** Change port in `application.yml` or `angular.json`
2. **Database connection failed:** Check PostgreSQL is running and credentials are correct
3. **Build fails:** Ensure Java 25 and Node 22.14.0+ are installed
4. **Frontend errors:** Clear node_modules and reinstall: `rm -rf node_modules && npm install`

## 📄 License

[Add your license information here]

## 👥 Contributors

[Add contributor information here]

---

### Activation shared_preload_libraries 
shared_preload_libraries = 'pg_cron'
pour activer l'extension pg_cron pour les tâches planifiées sur windows
il faut ajouter cette ligne dans le fichier postgresql.conf
et redémarrer le service postgresql
En amont il faut installer l'extension pgAgent 

create schema pharma_smart;

alter schema pharma_smart owner to pharma_smart;
## reparation de la base de données pharma_smart
mvn -Dflyway.url="jdbc:postgresql://localhost:5432/pharma_smart" -Dflyway.user=pharma_smart -Dflyway.password="2802_pharma_smart" -Dflyway.schemas=pharma_smart -Dflyway.table=pharma_smart_history   flyway:repair
"Get users to their primary task as fast as possible"
