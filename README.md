# AI Car Rental SaaS

Multi-tenant vehicle rental platform with staff operations, a public customer marketplace,
AI-assisted pricing, reservation and rental workflows, payments, invoices, notifications,
and event-driven processing.

## Technology Stack

- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA
- Database: PostgreSQL and Flyway migrations
- Messaging: Kafka with the transactional outbox pattern
- Cache: Redis
- AI: Spring AI with Google Gemini
- Frontend: Angular 19, TypeScript, Angular Material, SCSS
- Local infrastructure: Docker Compose

## Architecture

The backend separates HTTP DTOs, application services, domain entities, persistence, and
infrastructure concerns. Tenant-scoped staff operations and global customer accounts use
separate JWT principal types. Public booking services enforce tenant ownership, availability,
date overlap, insurance, and payment idempotency rules.

Domain events are written to the outbox in the same transaction as business state. A scheduler
publishes pending messages to Kafka, where notification and downstream consumers process them.

## Prerequisites

- Java 17 or newer
- Node.js and npm
- Docker Desktop

No global Maven installation is required; the repository includes Maven Wrapper.

## Environment Configuration

Copy `.env.example` to `.env` and replace all placeholder values. The `.env` file is ignored by
Git and is imported by Spring Boot when the application starts from the repository root. Docker
Compose also uses the same file for PostgreSQL configuration.

Generate a JWT secret containing at least 32 random bytes and store its Base64 representation in
`JWT_SECRET`. Never commit real credentials or API keys.

Set `SPRING_PROFILES_ACTIVE=local` for local development if you want demo seed data. Production
deployments must set an explicit non-local profile and must not rely on a default `dev` profile.
Set `CORS_ALLOWED_ORIGINS` to the exact frontend origin for the target environment.

Customer email verification and password reset emails can be tested with Gmail SMTP. Enable
2-Step Verification on the Gmail account, create a Gmail App Password, and put the app password
in `.env`; do not use or commit the normal Gmail password.

```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_gmail_address@gmail.com
MAIL_PASSWORD=your_gmail_app_password
MAIL_FROM=your_gmail_address@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
CUSTOMER_FRONTEND_BASE_URL=http://localhost:4200
STAFF_FRONTEND_BASE_URL=http://localhost:4200
```

When `MAIL_ENABLED=false`, the application uses the logging email sender for local development.

## Run Locally

Start PostgreSQL, Kafka, Kafka UI, and Redis:

```powershell
docker compose up -d
```

Start the backend from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The local backend runs at `http://localhost:8080`. Swagger UI is available at
`http://localhost:8080/swagger-ui/index.html`, and Kafka UI at `http://localhost:8085`.

Start the Angular frontend in a second terminal:

```powershell
cd frontend
npm install
npm start
```

The frontend runs at `http://localhost:4200`.

## Main User Flows

### Customer marketplace

1. Open `/rent` and choose pickup and return dates.
2. Filter available vehicles across active rental companies.
3. Continue as a guest or register a customer account.
4. Create a reservation and complete the mock deposit payment.
5. Track a guest reservation with its reservation code and email, or view account reservations.

### AI vehicle search API

The public AI interpreter converts Turkish or English natural-language requests into the same
validated filters used by the deterministic marketplace search. It never returns or invents
vehicles; clients apply the returned criteria to the regular availability endpoint.

```http
POST /api/public/marketplace/vehicle-search/interpret
Content-Type: application/json

{
  "query": "2000-5000 TL arasi, otomatik ve gunluk en az 500 km arac"
}
```

Pickup and return dates remain explicit marketplace inputs. The AI endpoint is limited through
Redis to 10 requests per client per minute by default. Configure the limit with
`AI_MARKETPLACE_RATE_LIMIT_MAX_REQUESTS` and `AI_MARKETPLACE_RATE_LIMIT_WINDOW_SECONDS`.

### Staff operations

1. Open `/login` and authenticate as `SUPER_ADMIN`, `TENANT_ADMIN`, or `TENANT_STAFF`.
2. Manage vehicles, reservations, rentals, payments, reports, notifications, and AI pricing.
3. Tenant administrators manage users, insurance packages, and tenant settings.
4. Super administrators manage tenants and platform-wide records.

## Database Migrations

Flyway is the only schema change mechanism. Hibernate runs with `ddl-auto=validate`; it never
updates the schema. New database changes must be added as the next immutable migration under
`src/main/resources/db/migration`.

To verify a clean schema, run the application or tests against an empty PostgreSQL database and
allow Flyway to apply migrations from `V1` onward.

## Verification

Backend tests use a disposable PostgreSQL Testcontainer and do not require the development
database:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Build the frontend:

```powershell
cd frontend
npm run build
```

Docker must be running for the backend integration test.

## Current Integration Boundaries

- Deposit and refund processing use a mock payment provider.
- Email delivery is interface based. Local development can log emails, while Gmail SMTP can be enabled through environment variables for testable customer verification and password reset flows.
- Staff and customer access tokens are kept in browser memory and renewed through separate rotating HttpOnly refresh cookies.
- The example access-token lifetime is 15 minutes; refresh sessions default to seven days.
- For HTTPS production deployments, set `AUTH_REFRESH_COOKIE_SECURE=true`. Cross-site frontend/backend deployments also require a compatible `AUTH_REFRESH_COOKIE_SAME_SITE` value and an exact credentialed CORS origin.
- Marketplace customers can describe vehicle preferences in Turkish or English. AI extracts semantic price and segment intent, while PostgreSQL applies availability, tenant safety, category and live price-percentile filters.
- Relative expressions such as "not too expensive" use the current matching fleet price distribution; explicit numeric price limits always take priority.
- Customer accounts require email verification before login and support password reset through single-use emailed links.
- Gemini is optional at runtime only where rule-based fallback behavior is available.
