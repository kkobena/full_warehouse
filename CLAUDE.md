# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pharma-Smart is a full-stack pharmacy warehouse management system built with Spring Boot (backend) and Angular (frontend). The application manages pharmaceutical inventory, sales, customers, suppliers, invoicing, cash registers, and compliance with French pharmacy regulations. The project uses a PostgreSQL database with Flyway migrations.

## Technology Stack

**Backend (Java 25):**
- Spring Boot 4.0.0-M3 (milestone release)
- Hibernate 7.1.0 with JPA
- PostgreSQL database
- Flyway for database migrations
- Caffeine/JCache for caching
- Spring Security with CSRF protection
- Flying Saucer & OpenPDF for PDF generation
- Apache POI & EasyExcel for Excel operations
- Barcode4j & ZXing for barcode generation/reading
- Thymeleaf for server-side HTML rendering

**Frontend (Angular 20):**
- Angular 20.3.2 with TypeScript 5.9.2
- PrimeNG 20.2.0 and ng-bootstrap for UI components
- AG Grid for advanced data tables
- Chart.js for data visualization
- ngx-translate for internationalization
- RxJS 7.8.2 for reactive programming
- Jest for unit testing

## Build & Development Commands

### Backend (Maven)
```bash
# Run application in dev mode (with hot reload)
./mvnw

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

### Combined Workflow
```bash
# Full production build (backend + frontend)
./mvnw clean package -Pprod

# Development mode: Start backend, then in separate terminal start frontend
./mvnw                # Terminal 1 (backend on port 8080)
npm start             # Terminal 2 (frontend on port 4200)
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
  - V1.0.1__init.sql - Initial schema
  - V1.0.2__referentiels.sql - Reference data
  - V1.0.3__procedures.sql - Stored procedures
  - V1.0.4__menus.sql - Menu/navigation structure
  - V1.0.5__id_generator.sql - ID generation functions
- **Flyway**: Manages versioned migrations automatically on startup

## Key Design Patterns & Conventions

### Security Architecture

The application uses Spring Security with three separate security filter chains:
1. **Main web application** (`/api/**`): Form-based login with CSRF protection, remember-me functionality
2. **Java client endpoints** (`/java-client/**`): HTTP Basic authentication (for desktop/printer clients)
3. **Mobile API** (`/api-user-account`): Permit all for mobile app access

Authorization is role-based with a fine-grained privilege system:
- Roles are defined in `Authority` domain entities
- Privileges are mapped to menus and actions via `AuthorityPrivilege` and `Menu` entities
- Use `SecurityUtils` for programmatic security checks in services
- Use `@Secured` annotations on service methods for declarative security

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
- Report generation services use Flying Saucer + Thymeleaf templates (in `src/main/resources/templates/`)

### REST API Conventions

- Base path: `/api/`
- Admin endpoints: `/api/admin/`
- Resource-based naming (e.g., `/api/customers`, `/api/sales`)
- Use Spring's `@RestController`, `@RequestMapping`, and standard HTTP methods
- DTOs for request/response bodies
- Pagination with Spring Data's `Pageable`

### Frontend Architecture

- Feature module per entity (lazy-loaded routes)
- Standalone components (Angular 20+ pattern)
- Services for HTTP communication (use Angular's `HttpClient`)
- RxJS observables for async operations
- PrimeNG components for complex UI (tables, dialogs, forms)
- AG Grid for high-performance data tables
- Reactive forms for user input

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
- Use `CommonReportService`, `SaleReceiptService`, `SaleInvoiceReportService` for standard report types

### CSV Import/Export

- **Export**: Use Apache Commons CSV or custom CSV service implementations
- **Import**: EasyExcel library for Excel file parsing
- Import services are in `service/csv/` package
- Reference README.md for SQL queries used in CSV exports (legacy migration support)

## Development Workflows

### Adding a New Entity

1. Create JPA entity in `domain/` package with appropriate annotations
2. Create Spring Data repository in `repository/` package
3. Create DTO in `service/dto/` if needed
4. Implement service interface and implementation in `service/`
5. Create REST controller in `web/rest/`
6. Add Flyway migration in `src/main/resources/db/migration/`
7. Create Angular service in `src/main/webapp/app/entities/<entity>/service/`
8. Create Angular components (list, form, detail) in `src/main/webapp/app/entities/<entity>/`
9. Add routes to `entity.routes.ts`

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

- Printer integration code is in `printer/` package
- Uses jSerialComm library for serial port communication
- Common printer widths: 80mm (most receipts), 57mm (compact receipts)
- Receipt templates are in Thymeleaf format
- Test printer connectivity using cash register endpoints

### Spring Profiles

- **dev** (default): Development mode with Spring DevTools, relaxed security
- **prod**: Production mode with optimized builds, strict security
- **tls**: Enable TLS/HTTPS
- **webapp**: Build frontend during Maven lifecycle

Set profile with: `./mvnw -Dspring.profiles.active=prod` or via environment variable

## Important Notes

### Java Version
This project uses **Java 25** (milestone/early access). Ensure your JDK is properly configured.

### Node Version
Requires **Node.js >= 22.14.0** with **npm 11.0.0**

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

- **Hibernate Envers**: Some entities use audit history tracking. Be aware when modifying audited entities.
- **Spring Boot Milestone**: Version 4.0.0-M3 is a milestone release; some features may be unstable.
- **Flyway schemas**: Ensure `flyway.schemas` is set to `warehouse` in application.yml
- **CSRF tokens**: Frontend must include CSRF token from cookie in requests
- **Timezone**: All timestamps stored in UTC (configured in JPA properties)
- **Transaction boundaries**: Be careful with lazy-loaded collections outside transaction scope

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
psql -U warehouse -d warehouse
\dt warehouse.*              # List all tables
\d warehouse.table_name      # Describe table structure
```
- use angular 20
- use JDK 25
- use primeng 20
- use p-button
- angular 20 new flow