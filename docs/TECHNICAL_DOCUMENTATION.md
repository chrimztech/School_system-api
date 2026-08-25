# SRMS API — Technical Documentation

**School Records Management System (SRMS) — Backend API**
Version: 0.0.1-SNAPSHOT | Spring Boot 3.2.5 | Java 17

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Multi-Tenancy Model](#3-multi-tenancy-model)
4. [Security & Authentication](#4-security--authentication)
5. [Common Infrastructure](#5-common-infrastructure)
6. [Database](#6-database)
7. [Configuration & Environment Variables](#7-configuration--environment-variables)
8. [Running & Building the API](#8-running--building-the-api)
9. [Roles & Permissions](#9-roles--permissions)
10. [API Reference (by module)](#10-api-reference-by-module)
11. [Known Gaps & Production Hardening Notes](#11-known-gaps--production-hardening-notes)

---

## 1. Overview

SRMS API is the backend for a multi-tenant school administration platform serving Zambian schools. It is a **modular monolith**: a single Spring Boot application organized into 45 business-domain packages (`com.srms.api.modules.*`), each typically containing its own controller, service, repository, entity and DTO classes.

The API is consumed by the companion frontend (see the sibling repo `School_system`, a TanStack Start/React app) and is designed to serve many schools ("tenants") from one deployment.

**Tech stack:**

| Concern | Technology |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.2.5 (Web, Data JPA, Security, Validation, Mail) |
| Database | PostgreSQL |
| ORM | Hibernate / Spring Data JPA (`ddl-auto=update`) |
| Auth | JWT (JJWT 0.12.5, HMAC-SHA), Spring Security, stateless sessions |
| API docs | springdoc-openapi 2.3.0 (Swagger UI) |
| Password hashing | BCrypt |
| Email | Spring Boot Mail (SMTP) |
| SMS | Zamtel BulkSMS API |
| Build | Maven |

---

## 2. Architecture

```
com.srms.api
├── common/        shared response wrapper, base entity
├── config/        Spring configuration (CORS, security, OpenAPI, async, JPA auditing)
├── exception/      GlobalExceptionHandler and custom exception types
├── security/       JWT provider, authentication filter, user details
└── modules/        45 business domains (see §10), each self-contained:
    └── <module>/
        ├── controller  (@RestController, @RequestMapping)
        ├── service
        ├── repository  (Spring Data JPA)
        ├── entity      (@Entity, extends BaseEntity)
        └── dto
```

Each module is scoped to a school via a `schoolId` path segment and/or column — there is no separate module for "core" vs "tenant" data; multi-tenancy is handled uniformly at the entity/repository level (see §3).

Cross-cutting concerns (auditing, async, exception handling, CORS, JWT filtering) live outside the modules in `common`, `config`, `exception`, and `security`.

---

## 3. Multi-Tenancy Model

SRMS uses **row-level (shared schema) multi-tenancy**, not schema-per-tenant or database-per-tenant:

- A single PostgreSQL database (`school_system`) holds all schools' data.
- Nearly every entity (70+ of them) carries a `school_id` column that scopes the row to one school.
- REST endpoints for school-scoped resources are nested under `/api/schools/{schoolId}/...`, and the service layer is expected to filter/validate against that path parameter.
- `SUPER_ADMIN` is the one role that is **not** scoped to a school (`schoolId = null` on their `app_users` row) — this is the platform-operator role that can see across all schools via `/api/admin/**` and `/api/platform/**` endpoints.
- The `School` entity itself is the tenant registry (`/api/schools`), holding branding, subscription/plan status, feature flags, and academic structure (levels/campuses) as JSON columns.

**Implication for integrators:** every school-scoped request must include the correct `schoolId` in the path, and the JWT's `schoolId` claim should match (or the caller must be `SUPER_ADMIN`). There is currently no database-level tenant isolation (e.g. row-level security policies) — isolation is enforced in application code, so treat `schoolId` validation in service methods as a security-critical path when extending the system.

---

## 4. Security & Authentication

### Login flow

1. `POST /api/auth/login` with `{ email, password }`.
2. `AuthService` looks up the user by email and verifies the password against the BCrypt hash in `app_users.password_hash`.
3. On success, `JwtTokenProvider` issues a signed JWT containing:
   - `sub` — user id
   - `email`
   - `role` — one of the system roles (§9)
   - `schoolId` — omitted/null for `SUPER_ADMIN`
   - `iat` / `exp`
4. Response body includes the token plus a lightweight user profile (id, name, email, role, schoolId, initials).
5. The client sends the token on every subsequent request as `Authorization: Bearer <token>`.
6. `JwtAuthenticationFilter` validates the signature/expiry, extracts claims, and populates the Spring Security context with a `UsernamePasswordAuthenticationToken` carrying the role as a granted authority.

### Token details

- Algorithm: HMAC-SHA (`Keys.hmacShaKeyFor`) via `jwt.secret`.
- Expiration: `jwt.expiration` = 86,400,000 ms (24 hours).
- **No refresh-token mechanism** — clients must re-authenticate after expiry.
- Session policy: `STATELESS` (no server-side session state); CSRF protection is disabled since there are no cookie-based sessions.

### Endpoint protection

- Public (no auth required): `/api/auth/**`, `/api/public/**`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`), `/actuator/**`.
- Everything else requires a valid bearer token.
- Role checks are enforced implicitly by the claims/route structure (e.g. `/api/admin/**` and `/api/platform/**` are intended for `SUPER_ADMIN`) rather than via `@PreAuthorize`/`@Secured` method annotations — see §11 for the hardening implication.

### Passwords

- BCrypt (`BCryptPasswordEncoder`, default strength).
- Minimum length 8 characters (enforced at the DTO/validation layer on signup/creation).
- `POST /api/auth/change-password` requires the current password to be supplied and verified before setting a new one.

### CORS

Configured via `cors.allowed-origins` (comma-separated). Out of the box this allows the common local dev ports for the frontend (`3000`, `5173`, `4173`, `8081`, `8082`). **Update this for any deployed frontend origin.**

### Custom roles

Beyond the six built-in system roles, a school can define **custom roles** (`CustomRole` + `CustomRolePermission` entities) via `/api/schools/{schoolId}/roles`, each carrying a named set of permission strings (e.g. `READ_STUDENTS`, `EDIT_MARKS`). These are additive/administrative and don't replace the six system roles used for JWT claims.

---

## 5. Common Infrastructure

### Standard response envelope

All controllers return a generic `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Optional message",
  "data": { "...": "..." },
  "timestamp": "2026-07-01T12:00:00"
}
```

Factory helpers: `ApiResponse.ok(data)`, `ApiResponse.ok(message, data)`, `ApiResponse.created(data)`, `ApiResponse.error(message)`.

### Global exception handling

`GlobalExceptionHandler` centralizes error responses:

| Exception | HTTP status | Notes |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found by id |
| `BusinessException` | 400 | Domain/business-rule violation |
| `MethodArgumentNotValidException` | 400 | Bean validation failures; returns a field → message map |
| Any other `Exception` | 500 | Generic fallback |

### Base entity

Every JPA entity extends a shared `BaseEntity` (`@MappedSuperclass`) providing:

- `id` (UUID, generated, immutable)
- `createdAt` (set once, via `@EnableJpaAuditing`)
- `updatedAt` (refreshed on every save)

### Pagination

There is no shared pagination wrapper — list endpoints currently return full collections. If a module needs paging, it currently does so ad hoc via query parameters in its own controller/repository (verify per-endpoint in Swagger before assuming `page`/`size` support).

---

## 6. Database

- **Engine:** PostgreSQL.
- **Schema management:** Hibernate `ddl-auto=update` — the schema is generated/evolved automatically from `@Entity` classes at boot. There are **no Flyway/Liquibase migrations** in this project; schema changes ship as code changes to entity classes and apply automatically on next boot.
- **Default connection:** `jdbc:postgresql://localhost:5432/school_system`, user `postgres`.

### Representative tables

| Table | Purpose | Notable columns |
|---|---|---|
| `app_users` | Login accounts (all roles) | email (unique), password_hash, role, school_id (nullable), active |
| `schools` | Tenant registry | slug, subscription_status, features_json, levels_json, campuses_json |
| `students` | Learner records | admission_number (unique), guardian_email/phone, status, fee_balance |
| `teachers` | Staff/teacher profiles | qualifications, specialization, active |
| `school_classes` | Forms/streams | grade, section, class_teacher_id, capacity |
| `subjects`, `departments` | Curriculum org | — |
| `attendance_records` | Daily attendance | student_id, class_id, date, status |
| `assessments`, `assessment_results` | Continuous assessment marks | assessment_id, student_id, score |
| `exams`, `timetable_slots` | Scheduling | — |
| `fee_payments`, `fee_structures` | Finance | amount, payment_method, status |
| `payroll_runs`, `payslip_entries` | Payroll | gross, deductions, net |
| `library_books`, `library_loans` | Library | isbn, due_date, return_date |
| `discipline_cases` | Behavior records | status (open/resolved) |
| `hostel_rooms`, `hostel_allocations` | Boarding | capacity, vacate_date |
| `audit_events` | Change/audit trail | user_id, action, entity_type, timestamp |
| `custom_roles`, `custom_role_permissions` | Per-school RBAC | permission_code |

All tables carry `id`, `created_at`, `updated_at`; nearly all (except `app_users` for `SUPER_ADMIN`) carry `school_id`.

---

## 7. Configuration & Environment Variables

Source: `src/main/resources/application.properties`.

```properties
spring.application.name=srms-api
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/school_system
spring.datasource.username=postgres
spring.datasource.password=11111111
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT
jwt.secret=srms-production-secret-key-minimum-256-bits-long-change-in-prod
jwt.expiration=86400000

# CORS
cors.allowed-origins=http://localhost:3000,http://localhost:5173,http://localhost:4173,http://localhost:8081,http://localhost:8082

# System admin bootstrap (only used if admin@srms.zm does not exist yet)
app.admin.email=admin@srms.zm
app.admin.password=Admin@SRMS2024!

# Email (SMTP)
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.from=${MAIL_FROM:noreply@srms.zm}

# Zamtel BulkSMS
zamtel.bulksms.base-url=${ZAMTEL_BULKSMS_BASE_URL:https://bulksms.zamtel.co.zm/api}
zamtel.bulksms.api-key=${ZAMTEL_BULKSMS_API_KEY:}
zamtel.bulksms.sender-id=${ZAMTEL_BULKSMS_SENDER_ID:DCL}
```

### Environment variables to override in any real deployment

| Variable | Purpose | Default |
|---|---|---|
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP for the email notification channel | Gmail SMTP, blank creds |
| `ZAMTEL_BULKSMS_API_KEY`, `ZAMTEL_BULKSMS_SENDER_ID` | Zamtel BulkSMS channel | blank (SMS disabled) |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Bootstrap super-admin account created on first boot if missing | `admin@srms.zm` / `Admin@SRMS2024!` |

**⚠️ Security note:** `application.properties` currently hardcodes a database password, a JWT signing secret, and a default super-admin password directly in the committed file. Before any shared/production deployment, move these to environment variables (Spring already supports `${VAR:default}` — extend that pattern to `spring.datasource.password` and `jwt.secret`) and rotate the default admin password immediately after first login.

There are no Spring profiles (`dev`/`prod`) configured — everything runs under the `default` profile.

---

## 8. Running & Building the API

### Prerequisites

- JDK 17+
- Maven 3.6+
- PostgreSQL 12+ reachable at the configured URL, with the `school_system` database created (`CREATE DATABASE school_system;`)

### Build & run

```bash
# from F:\school\School_system-api
mvn clean package
java -jar target/srms-api-0.0.1-SNAPSHOT.jar

# or, for local dev with auto-reload of config:
mvn spring-boot:run
```

### First boot

1. Hibernate creates/updates all tables automatically (`ddl-auto=update`) — no manual migration step needed.
2. If no user exists with email `admin@srms.zm` (or the `APP_ADMIN_EMAIL` override), a bootstrap `SUPER_ADMIN` account is created with the configured password.
3. Log in via `POST /api/auth/login` to obtain a JWT, then use it as a bearer token for all further calls.

### Endpoints of interest

- Base URL: `http://localhost:8080`
- API docs (Swagger UI): `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Docker

No `Dockerfile`/`docker-compose.yml` is currently checked in. To containerize, a standard Spring Boot multi-stage build (Maven build stage → `eclipse-temurin` JRE runtime stage) plus a Postgres service would be the natural next step.

---

## 9. Roles & Permissions

### System roles (fixed enum on `AppUser`)

| Role | Scope | Typical capabilities |
|---|---|---|
| `SUPER_ADMIN` | Platform-wide (`schoolId = null`) | Create/manage schools, manage all users, platform dashboard, billing/plan administration |
| `SCHOOL_ADMIN` | One school | Manage that school's users, students, staff, classes, settings |
| `TEACHER` | One school | Attendance, assessments/marks, timetable, own classes/students |
| `HOD` | One school (department-level) | Supervises teachers/classes within a department, plus teacher-level capabilities |
| `FINANCE` | One school | Fees, billing, payroll, procurement, accounting |
| `PARENT` | One school | Read-mostly access scoped to their own linked child (attendance, marks, fee balance, communication) |

### Custom roles

Schools may define additional named roles with granular permission strings via `/api/schools/{schoolId}/roles`, layered on top of (not replacing) the six system roles.

### Enforcement caveat

Authorization today is largely **implicit**: route namespacing (`/api/admin/**`, `/api/platform/**` vs `/api/schools/{schoolId}/**`) plus JWT claim checks in service code, rather than declarative `@PreAuthorize`ANNOTATIONs on every controller method. When adding new endpoints, follow the existing pattern of validating `schoolId`/role in the service layer — don't assume Spring Security is doing it for you beyond "is this request authenticated at all."

---

## 10. API Reference (by module)

All school-scoped endpoints are rooted at `/api/schools/{schoolId}/...` unless noted otherwise. This is a reference of base paths and endpoints per module — for exact request/response schemas use the live Swagger UI (`/swagger-ui.html`), which is generated from the actual DTOs and is the source of truth.

### Auth & Users
`/api/auth`, `/api/admin/users`, `/api/schools/{schoolId}/users`, `/api/schools/{schoolId}/roles`

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/login` | Authenticate, returns JWT + profile |
| GET | `/api/auth/me` | Current user profile |
| POST | `/api/auth/change-password` | Change own password |
| GET/POST | `/api/admin/users` | List/create platform users (SUPER_ADMIN) |
| PATCH/DELETE | `/api/admin/users/{userId}` | Update/deactivate user |
| GET/POST | `/api/schools/{schoolId}/users` | List/create users in a school |
| PATCH/DELETE | `/api/schools/{schoolId}/users/{userId}` | Update/deactivate school user |
| GET/POST/PUT/DELETE | `/api/schools/{schoolId}/roles` | Manage custom roles |
| GET/PUT | `/api/schools/{schoolId}/roles/{roleName}/permissions` | Get/set role permissions |

### Academic (Classes, Subjects, Departments)
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/classes` / `/classes/{id}` | Manage classes |
| GET/POST/DELETE | `/classes/{classId}/enrolments` | Enrol/unenrol students |
| GET/POST/DELETE | `/classes/{classId}/teachers` | Assign/remove teacher-subject assignments |
| GET/POST/PUT/DELETE | `/subjects`, `/subjects/bulk`, `/subjects/{id}` | Manage subjects |
| GET/POST/PUT/DELETE | `/departments`, `/departments/{id}` | Manage departments |

### Students / Teachers
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/students`, `/students/{id}` | CRUD; `GET /students/by-guardian` filters by guardian email |
| GET/POST/PUT/DELETE | `/teachers`, `/teachers/{id}` | CRUD |

### School Registry
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/public/schools/by-slug/{slug}` | Public lookup (no auth) |
| GET/POST/PUT/DELETE | `/api/schools`, `/api/schools/{id}` | Manage tenant schools |

### Attendance
| Method | Path | Purpose |
|---|---|---|
| GET | `/attendance`, `/attendance/summary`, `/attendance/date/{date}`, `/attendance/student/{studentId}` | Query attendance |
| POST | `/attendance` | Record/batch mark attendance |

### Exams / Assessments
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/exams`, `/exams/{id}` | Manage exam papers |
| GET/POST/PUT | `/assessments`, `/assessments/{id}` | Manage assessment templates |
| GET/POST | `/assessments/{id}/results`, `/assessments/{id}/results/bulk` | Record marks |
| GET | `/assessments/student/{studentId}`, `/assessments/student/{studentId}/enriched` | Student mark history |

### Timetable
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/timetable`, `/timetable/{id}` | Manage schedule slots (filter by classId/teacherId) |

### Fees / Billing / Payroll / Accounting
| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/fees/payments`, `/fees/payments/student/{studentId}` | Record/query fee payments |
| GET | `/fees/collected` | Total collected |
| GET/POST/PATCH | `/fees/structures`, `/fees/levies`, `/fees/discounts`, `/fees/billing-rules` | Fee configuration |
| GET/POST/PATCH | `/billing/invoices`, `/billing/invoices/{id}/pay` | Invoices |
| GET/POST | `/payroll/runs`, `/payroll/runs/{id}/payslips` | Payroll |
| POST | `/payroll/runs/{id}/process` | Finalize a payroll run |
| GET/POST/PATCH | `/accounting/journal`, `/accounting/journal/{id}/post` | Journal entries |
| GET/POST | `/accounting/expenses` | Expenses |
| GET/POST/PATCH | `/bursaries`, `/bursaries/applications`, `/bursaries/renewals` | Bursary/scholarship management |

### Library / Inventory / Transport / Canteen / Hostel
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/library/books` | Book catalog |
| GET/POST/PUT | `/library/loans`, `/library/loans/{id}/return` | Loans |
| GET/POST/PUT/DELETE | `/inventory`, `/inventory/{id}` | Stock items |
| GET/POST | `/inventory/movements` | Stock movements |
| GET/POST/PUT/DELETE | `/transport/vehicles` | Vehicles |
| GET/POST | `/transport/routes`, `/transport/enrolments` | Routes and student enrolments |
| GET/POST/PUT/DELETE | `/canteen/menu` | Menu items |
| GET/POST | `/canteen/orders` | Orders |
| GET/POST/PUT/DELETE | `/hostel/rooms` | Rooms |
| GET/POST/PUT | `/hostel/allocations`, `/hostel/allocations/{id}/vacate`, `/hostel/allocations/{id}/sign-in` | Allocations |
| GET/POST/PATCH | `/hostel/leaves`, `/hostel/leaves/{id}/status` | Leave requests |

### Admissions / Health / HR / Discipline / Welfare
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT | `/admissions`, `/admissions/{id}`, `/admissions/{id}/accept`, `/admissions/{id}/reject` | Admission pipeline |
| GET/POST/PUT | `/health/records`, `/health/records/student/{studentId}` | Health records |
| GET/POST | `/health/visits` | Clinic visits |
| GET/POST/PUT/DELETE | `/hr/staff` | Staff records |
| GET/POST/PUT | `/hr/leave`, `/hr/leave/{id}/approve`, `/hr/leave/{id}/reject` | Leave requests |
| GET/POST/PATCH | `/discipline`, `/discipline/student/{studentId}`, `/discipline/{id}/resolve` | Discipline cases |
| GET/POST/PUT | `/welfare/cases`, `/welfare/sessions` | Pastoral care / counselling |

### Operations (Facilities / Visitors / Lost & Found / Incidents / Procurement / Vendors / Activities / Alumni / Calendar / Duty Roster)
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/PATCH | `/facilities`, `/facilities/{id}/close` | Work orders |
| GET/POST/PUT | `/visitors`, `/visitors/{id}/checkout` | Visitor log |
| GET/POST/PUT | `/lost-found`, `/lost-found/{id}/claim` | Lost & found |
| GET/POST/PUT/PATCH | `/incidents`, `/incidents/{id}/resolve` | Incident management |
| GET/POST/PUT | `/procurement`, `/procurement/{id}/approve` | Procurement requests |
| GET/POST/PUT | `/vendors` | Vendor management |
| GET/POST/PUT/DELETE | `/activities`, `/activities/{id}/enrolments` | Clubs/activities |
| GET/POST/PUT/DELETE | `/alumni`, `/alumni/{id}` | Alumni records |
| GET/POST/DELETE | `/calendar` | Calendar events |
| GET/POST/DELETE | `/duty-roster` | Duty assignments |

### Governance & Reporting (Compliance / Risk / Strategic / Reporting / Audit / Report Comments)
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT | `/compliance` | Compliance checklist items |
| GET/POST/PUT | `/risk-register` | Risk entries |
| GET/POST/PATCH | `/strategic-plan/goals`, `/strategic-plan/actions`, `/strategic-plan/reviews` | Strategic planning |
| GET/POST | `/reporting/reports` | Saved custom reports |
| GET/POST | `/audit` | Audit event log |
| GET/PUT | `/report-comments/student/{studentId}` | Teacher report-card comments |

### Staff Development
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT | `/staff-development` | Training records |
| GET/POST/PATCH | `/staff-development/appraisals`, `/staff-development/observations`, `/staff-development/pdps` | Appraisals, observations, development plans |

### Integrations & Communication
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PATCH | `/integrations`, `/integrations/{code}` | Third-party integration connections |
| GET/POST/PUT/DELETE | `/announcements` | Announcements |
| GET/POST/PUT | `/messages`, `/messages/{id}/reply`, `/messages/{id}/close` | Messaging threads |

### Dashboards
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/schools/{schoolId}/dashboard` | School-level KPI summary |
| GET | `/api/platform/dashboard` | Platform-wide KPI summary (SUPER_ADMIN) |
| GET/PUT | `/api/platform/workspace` | Platform workspace settings |

---

## 11. Known Gaps & Production Hardening Notes

These are things a developer picking up this codebase should be aware of before a production deployment:

1. **Secrets in source control**: DB password, JWT secret, and default admin password are committed in plaintext in `application.properties`. Externalize via environment variables and rotate before go-live.
2. **No refresh tokens**: sessions simply expire after 24h with no silent renewal; frontend must handle re-login gracefully.
3. **No Flyway/Liquibase**: schema evolves via `ddl-auto=update`, which is convenient for development but risky for production migrations (no rollback path, no explicit migration history). Consider introducing versioned migrations before scaling.
4. **Authorization is route/service-level, not declarative**: there's no consistent `@PreAuthorize` layer, so a new endpoint added without care could unintentionally skip a role check. Review the equivalent existing module's service code as a template when adding endpoints.
5. **No database-level tenant isolation**: `schoolId` scoping is enforced entirely in application code; a bug in a service method could leak cross-tenant data. Treat `schoolId` filtering as security-critical.
6. **No containerization** checked in yet (no Dockerfile/compose) — add if moving to containerized deployment.
