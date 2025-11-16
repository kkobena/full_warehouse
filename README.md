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
CREATE DATABASE warehouse;
CREATE USER warehouse WITH PASSWORD 'warehouse2802';
GRANT ALL PRIVILEGES ON DATABASE warehouse TO warehouse;
```

2. **Connect to the database:**

```bash
psql -U postgres
\c warehouse
```

3. **Create schema:**

```sql
CREATE SCHEMA warehouse AUTHORIZATION warehouse;
GRANT ALL PRIVILEGES ON SCHEMA warehouse TO warehouse;
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
psql -U warehouse -d warehouse

# List tables
\dt warehouse.*

# Describe table
\d warehouse.table_name

# List functions
\df warehouse.*
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

For questions or issues, please contact the development team or open an issue in the repository.
