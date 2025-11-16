# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

Pharma-Smart is a full-stack pharmacy warehouse management system built with Spring Boot (backend)
and Angular (frontend). The application manages pharmaceutical inventory, sales, customers,
suppliers, invoicing, cash registers, and compliance with French pharmacy regulations. The project
uses a PostgreSQL database with Flyway migrations and supports desktop deployment via Tauri.

## Technology Stack

**Backend (Java 25):**

- Spring Boot 4.0.0-RC1 (release candidate)
- Hibernate 7.1.0 with JPA
- PostgreSQL database
- Flyway 11.11.2 for database migrations
- Caffeine/JCache for caching
- Spring Security with JWT OAuth2 resource server
- Flying Saucer & OpenPDF for PDF generation
- Apache POI & EasyExcel for Excel operations
- Barcode4j & ZXing for barcode generation/reading
- Thymeleaf for server-side HTML rendering
- jSerialComm for thermal printer integration

**Frontend (Angular 20):**

- Angular 20.3.7 with TypeScript 5.9.2
- PrimeNG 20.2.0 (use `p-button` and other PrimeNG components)
- ng-bootstrap 19.0.1 for Bootstrap components
- AG Grid 34.3.0 for advanced data tables
- Chart.js for data visualization
- ngx-translate for internationalization
- RxJS 7.8.2 for reactive programming
- Jest for unit testing
- Standalone components (Angular 20+ pattern)

## Quick Reference Documentation

- **[HOW-TO-CONFIGURE-BACKEND.md](HOW-TO-CONFIGURE-BACKEND.md)** - Simple guide for configuring backend URL (non-technical users)
- **[LOGS-QUICK-REFERENCE.md](LOGS-QUICK-REFERENCE.md)** - Quick guide to finding and reading backend logs
- **[CUSTOM-TITLEBAR.md](CUSTOM-TITLEBAR.md)** - Custom titlebar implementation and customization guide
- **[TAURI_BACKEND_SETUP.md](TAURI_BACKEND_SETUP.md)** - Complete Tauri backend integration guide (technical)

## Build & Development Commands

### Backend (Maven)

**Note:** On Windows, use `mvnw.cmd` instead of `./mvnw`. On Linux/macOS, use `./mvnw`.

```bash
# Run application in dev mode (with hot reload)
./mvnw                          # Linux/macOS
mvnw.cmd                        # Windows CMD
./mvnw.cmd                      # Windows Git Bash

# Build for production
./mvnw clean package -Pprod

# Run tests
./mvnw test

# Run integration tests
./mvnw verify -Pfailsafe

# Skip frontend build during Maven build
./mvnw -Dskip.npm

# Generate JavaDocs
./mvnw javadoc:javadoc

# Flyway commands
./mvnw flyway:info              # Show migration status
./mvnw flyway:migrate           # Run pending migrations
```

### Frontend (npm)

```bash
# Install dependencies
npm install

# Start dev server with HMR (http://localhost:4200)
npm start

# Build for development
npm run webapp:build

# Build for production
npm run webapp:prod

# Run tests with coverage
npm test

# Run linter
npm run lint

# Fix linting issues
npm run lint:fix

# Format code
npm run prettier:format
```

### Desktop Application (Tauri)

**Prerequisites:** Requires Rust toolchain installed (`rustup`, `cargo`).

```bash
# Development mode (hot-reload)
npm run tauri:dev

# Build production desktop app
npm run tauri:build

# Build debug version (faster compilation)
npm run tauri:build:debug

# Fast build (assumes frontend already built)
npm run tauri:build:fast

# Tauri CLI commands
npm run tauri -- info           # System information
npm run tauri -- icon [path]    # Generate icons
```

**Build outputs:** Located in `src-tauri/target/release/bundle/` (NSIS/MSI installers for Windows)

### Combined Workflow

```bash
# Full production build (backend + frontend)
./mvnw clean package -Pprod

# Development mode: Start backend, then in separate terminal start frontend
./mvnw                # Terminal 1 (backend on port 8080)
npm start             # Terminal 2 (frontend on port 4200)

# Tauri desktop development (requires backend running)
./mvnw                # Terminal 1 (backend on port 8080)
npm run tauri:dev     # Terminal 2 (Tauri app with frontend)
```

## Project Architecture

### Backend Package Structure

```
com.kobe.warehouse/
├── aop/                      # Aspect-Oriented Programming (logging aspects)
├── config/                   # Spring configuration classes
│   ├── SecurityConfiguration.java      # Security setup with multiple filter chains
│   ├── DatabaseConfiguration.java      # JPA and datasource config
│   ├── CacheConfiguration.java         # Caffeine cache setup
│   └── FlywayConfig.java              # Database migration config
├── domain/                   # JPA entities (168 entities)
│   ├── Sales.java           # Sales transactions
│   ├── Customer.java        # Customer/client management
│   ├── Produit.java        # Product/pharmaceutical items
│   ├── Storage.java        # Inventory storage locations
│   └── ...                 # Many other domain entities
├── repository/              # Spring Data JPA repositories
├── security/               # Security utilities and constants
│   ├── SecurityUtils.java  # Authentication/authorization helpers
│   └── AuthoritiesConstants.java
├── service/                # Business logic layer
│   ├── dto/               # Data Transfer Objects
│   ├── cash_register/     # Cash register operations
│   ├── facturation/       # Invoicing/billing services
│   ├── receipt/          # Receipt generation
│   ├── report/           # Report generation (PDF/Excel)
│   ├── stock/            # Inventory management
│   └── csv/              # CSV import/export
├── web/rest/              # REST API controllers
│   ├── AccountResource.java
│   ├── cash_register/    # Cash register endpoints
│   ├── commande/         # Order management endpoints
│   └── facturation/      # Billing endpoints
├── printer/              # Thermal printer integration
└── constant/            # Application constants

```

### Frontend Structure

```
src/main/webapp/app/
├── account/              # User account management (login, password reset)
├── admin/               # Admin dashboard, logs, metrics, configuration
├── core/                # Core services and utilities
│   ├── auth/           # Authentication guards and interceptors
│   ├── config/         # App configuration
│   └── interceptor/    # HTTP interceptors
├── entities/            # Feature modules for each domain entity
│   ├── customer/       # Customer management UI
│   ├── ajustement/     # Stock adjustments
│   ├── commande/       # Order management
│   ├── facturation/    # Invoicing UI
│   ├── cash-register/  # POS/cash register interface
│   └── ...            # Many other entity modules
├── layouts/            # Application layout components
└── shared/            # Shared components, pipes, directives
```

### Database

- **RDBMS**: PostgreSQL
- **Schema**: `warehouse`
- **Migrations**: Located in `src/main/resources/db/migration/`
  - V1.0.1\_\_init.sql - Initial schema
  - V1.0.2\_\_referentiels.sql - Reference data
  - V1.0.3\_\_procedures.sql - Stored procedures
  - V1.0.4\_\_menus.sql - Menu/navigation structure
  - V1.0.5\_\_id_generator.sql - ID generation functions
- **Flyway**: Manages versioned migrations automatically on startup

## Key Design Patterns & Conventions

### Security Architecture

The application uses Spring Security 6.x with JWT OAuth2 resource server and multiple security
filter chains:

**Filter Chains:**

1. **Main web application** (`/api/**`): JWT OAuth2 authentication with CSRF protection
2. **Mobile API** (`/api-user-account`): Permit all for mobile app access

**Authorization:**

- Role-based with fine-grained privilege system
- Roles defined in `Authority` domain entities
- Privileges mapped to menus and actions via `AuthorityPrivilege` and `Menu` entities
- `SecurityUtils` class in `com.kobe.warehouse.security` for programmatic security checks
- `@Secured` annotations on service methods for declarative security
- `AuthoritiesConstants` defines role constants (e.g., `ADMIN`, `USER`)

**Key Configuration:**

- JWT converter: `JwtAuthenticationConverter` in `com.kobe.warehouse.security.jwt`
- Password encoding: BCrypt
- Session management: Stateless (JWT-based)

### Entity/Domain Model

- **168 domain entities** representing pharmacy business objects
- Entities use Hibernate with second-level caching (Caffeine)
- Standard JPA annotations with Hibernate-specific features
- Physical naming strategy: Standard (preserves entity field names as-is)
- Implicit naming strategy: JPA compliant
- Most entities extend base audit entities with created/modified timestamps

### Service Layer Patterns

- Service interfaces with implementation classes (e.g., `CustomerService` → `CustomerServiceImpl`)
- DTOs for data transfer between layers (in `service/dto/`)
- Service methods are transactional by default via `@Transactional`
- Complex queries use Criteria API or native queries via repositories
- Report generation services use Flying Saucer + Thymeleaf templates (in
  `src/main/resources/templates/`)

### REST API Conventions

- Base path: `/api/`
- Admin endpoints: `/api/admin/`
- Resource-based naming (e.g., `/api/customers`, `/api/sales`)
- Use Spring's `@RestController`, `@RequestMapping`, and standard HTTP methods
- DTOs for request/response bodies
- Pagination with Spring Data's `Pageable`

### Frontend Architecture

**Component Pattern (Angular 20):**

- **Standalone components** (no NgModules, use `imports` array directly)
- Functional route resolvers using `inject()` (see `customer.route.ts:11`)
- Route guards: `UserRouteAccessService` for authorization
- Lazy-loaded routes with `loadComponent: () => import(...).then(m => m.Component)`

**Route Structure Example:**

```typescript
const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./entity.component').then(m => m.EntityComponent),
    data: { authorities: [Authority.ADMIN] },
    canActivate: [UserRouteAccessService],
  },
];
export default routes; // Default export for routing
```

**UI Component Usage:**

- **PrimeNG 20.2.0**: Use `p-button`, `p-table`, `p-dialog`, etc. for primary UI
- **AG Grid**: For advanced data tables with filtering/sorting/pagination
- **Reactive Forms**: Template-driven forms discouraged
- **Services**: HTTP communication via Angular `HttpClient`, returns `Observable<HttpResponse<T>>`
- **Internationalization**: `@ngx-translate/core` for i18n

### Caching Strategy

- Second-level Hibernate cache using Caffeine (JCache provider)
- Cache configuration in `CacheConfiguration.java`
- Entity collections and query results are cached
- Manual cache invalidation in service layer when needed

### PDF & Report Generation

- **PDF Generation**: Flying Saucer (xhtmlrenderer) + OpenPDF
- **Templates**: Thymeleaf templates in `src/main/resources/templates/`
- **Barcode rendering**: Custom `BarcodeImageReplacedElementFactory` for embedding barcodes in PDFs
- **Reports location**: Generated PDFs are saved to `reports/` directory
- Use `CommonReportService`, `SaleReceiptService`, `SaleInvoiceReportService` for standard report
  types

### CSV Import/Export

- **Export**: Use Apache Commons CSV or custom CSV service implementations
- **Import**: EasyExcel library for Excel file parsing
- Import services are in `service/csv/` package
- Reference README.md for SQL queries used in CSV exports (legacy migration support)

## Development Workflows

### Adding a New Entity

**Backend:**

1. Create JPA entity in `domain/` package with annotations (`@Entity`, `@Table`, `@Cache`)
2. Create Spring Data repository in `repository/` package extending `JpaRepository`
3. Create DTO in `service/dto/` if needed for data transfer
4. Implement service interface in `service/` and implementation class
5. Add `@Transactional` to service methods as needed
6. Create REST controller in `web/rest/` with `@RestController` annotation
7. Add Flyway migration in `src/main/resources/db/migration/` (format:
   `V{version}__{description}.sql`)

**Frontend (Angular 20):**

1. Create entity model interface in `src/main/webapp/app/shared/model/`
2. Create standalone service in `app/entities/<entity>/<entity>.service.ts`
3. Create standalone components:

- `<entity>.component.ts` (list view)
- `<entity>-detail.component.ts` (detail view)
- `<entity>-update.component.ts` (create/edit form)
- `<entity>-delete-dialog.component.ts` (delete confirmation)

4. Create routes file `<entity>.route.ts` with functional resolvers (use `inject()`)
5. Use default export for routes: `export default entityRoutes;`
6. Import PrimeNG/ng-bootstrap components directly in component `imports` array

### Database Changes

- **Always use Flyway migrations** for schema changes
- Never modify existing migration files
- Create new versioned migration: `V{version}__{description}.sql`
- Test migration with `./mvnw flyway:info` and `./mvnw flyway:migrate`

### Running Tests

**Backend:**

```bash
./mvnw test                    # Unit tests only
./mvnw verify                  # Unit + integration tests
```

**Frontend:**

```bash
npm test                       # Run Jest tests with coverage
npm run test:watch            # Watch mode
```

### Working with Thermal Printers

**Backend Integration:**

- Printer code in `com.kobe.warehouse.printer/` package
- Uses jSerialComm 2.11.2 for serial port communication
- Common printer widths: 80mm (standard receipts), 57mm (compact receipts)
- Receipt templates: Thymeleaf format in `src/main/resources/templates/`
- Test endpoints in cash register REST controllers

**Tauri Desktop Integration:**

- Tauri can invoke printer commands via backend REST API
- Alternative: Use Tauri's shell plugin to execute printer commands directly
- For production, bundle Spring Boot JAR with Tauri app

### Tauri Desktop Architecture

**Directory Structure:**

```
src-tauri/
├── src/
│   ├── main.rs          # Tauri app entry point
│   └── lib.rs           # Rust commands/plugins
├── Cargo.toml           # Rust dependencies
├── tauri.conf.json      # Tauri configuration
└── icons/               # App icons (generated)
```

**Configuration (`src-tauri/tauri.conf.json`):**

- `build.devUrl`: Points to Angular dev server (`http://localhost:4200`)
- `build.frontendDist`: Points to compiled Angular app (`../target/classes/static`)
- `bundle.targets`: Build NSIS and MSI installers for Windows
- Plugins: `shell`, `http`, `dialog`, `fs` for system integration

**Angular Environment:**

- Tauri build uses `environment.tauri.ts` (file replacement in `angular.json:105`)
- API URLs should point to `localhost:8080` for backend communication
- Use `@tauri-apps/api` package to detect Tauri environment and invoke Rust commands

**Security:**

- Tauri enforces strict CSP (Content Security Policy)
- Only allowed APIs are accessible from frontend
- Backend must run separately or be bundled with the app

### Spring Profiles

- **dev** (default): Development mode with Spring DevTools, relaxed security
- **prod**: Production mode with optimized builds, strict security
- **tls**: Enable TLS/HTTPS
- **webapp**: Build frontend during Maven lifecycle

Set profile with: `./mvnw -Dspring.profiles.active=prod` or via environment variable

## Important Notes

### Java Version

This project uses **Java 25** for development. However, the Tauri bundled backend only checks for Java/JRE availability without enforcing a specific version. Ensure your JDK is properly configured with `JAVA_HOME` set correctly.

### Node Version

Requires **Node.js >= 22.14.0** with **npm >= 11.0.0**

### Rust (for Tauri Desktop)

If building desktop apps with Tauri, install Rust toolchain:

```bash
# Windows
winget install Rustlang.Rustup
# Or download from https://rustup.rs/

# Verify installation
rustc --version
cargo --version
```

Additional Windows requirements:

- Microsoft Visual C++ Build Tools
- WebView2 Runtime (usually pre-installed on Windows 10/11)

### PostgreSQL Configuration

- Recommended settings for 8GB RAM system are documented in README.md
- Database user: `warehouse`
- Schema: `warehouse`
- Use connection pooling (HikariCP is configured)

### Performance Considerations

- Enable Hibernate query caching for frequently-used queries
- Use `@BatchSize` on entity collections to avoid N+1 queries
- Batch inserts/updates are configured (batch size: 50)
- Second-level cache is enabled for most entities
- Frontend uses lazy loading for routes to reduce initial bundle size

### Known Configuration

- Backend runs on port 8080
- Frontend dev server runs on port 4200
- Reports are generated to `reports/` directory
- Email service uses Gmail SMTP (credentials required in application.yml)

## Common Pitfalls

- **Hibernate Envers**: Some entities use audit history tracking. Be aware when modifying audited
  entities.
- **Spring Boot Release Candidate**: Version 4.0.0-RC1 is a release candidate; some features may
  change.
- **Flyway schemas**: Ensure `flyway.schemas` is set to `warehouse` in `application.yml`
- **CSRF/JWT tokens**: Frontend must handle JWT tokens properly; CSRF enabled for web endpoints
- **Timezone**: All timestamps stored in UTC (configured in JPA properties via
  `hibernate.jdbc.time_zone=UTC`)
- **Transaction boundaries**: Lazy-loaded collections must be accessed within `@Transactional` scope
- **Windows paths**: Use forward slashes in code even on Windows; Maven wrapper is `mvnw.cmd` on
  Windows
- **Angular 20 patterns**: Do NOT use NgModules; all components are standalone with direct imports
- **PrimeNG components**: Must use PrimeNG 20.x compatible components and themes (
  `@primeuix/themes`)

## Debugging

**Backend:**

- Enable SQL logging: Set `spring.jpa.show-sql=true` in application.yml
- Enable Hibernate statistics: Set `hibernate.generate_statistics=true`
- Check logs in console output (Logback configuration)

**Frontend:**

- Use browser DevTools
- Angular DevTools extension for component inspection
- Check Network tab for API errors
- Console for JavaScript errors

**Database:**

```bash
# Connect to PostgreSQL database
psql -U warehouse -d warehouse

# Useful PostgreSQL commands
\dt warehouse.*              # List all tables in warehouse schema
\d warehouse.table_name      # Describe table structure
\df warehouse.*              # List all functions/procedures
\di warehouse.*              # List all indexes
```

## Code Style & Conventions

**Java/Spring:**

- Package naming: `com.kobe.warehouse.<layer>.<feature>`
- Service pattern: Interface + Implementation (`ServiceName` → `ServiceNameImpl`)
- Use constructor injection (avoid field injection with `@Autowired`)
- DTOs for API responses (in `service/dto/` package)
- Always add `@Transactional` explicitly where needed

**TypeScript/Angular:**

- Use Angular 20 standalone components (NO NgModules)
- Functional programming patterns (use `inject()` instead of constructor DI in functions)
- RxJS operators for reactive streams
- PrimeNG components for UI (`p-button`, `p-table`, etc.)
- Route files use default exports
- Strict TypeScript mode enabled

**Database:**

- Schema: `warehouse` (all tables prefixed or in schema)
- Flyway migrations are versioned and immutable
- Use stored procedures for complex queries (see `V1.0.3__procedures.sql`)
- Indexes on foreign keys and frequently queried columns
- Code_Writer
- responsiveLayout is deprecated
- styleClass is deprecated
- InputTextareaModule not exists in primeng 20+
- not use styleClass
- not use p-dialog
- not use prime Dialog use ngbModal
- not use \*ngif
