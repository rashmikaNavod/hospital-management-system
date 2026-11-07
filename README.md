# Hospital Management System

A backend portfolio project for managing hospital accounts and, in future iterations, appointments and staff workflows. The project follows a monorepo structure with a planned microservices architecture.

**Status:** Under active development. Only the authentication service is currently being implemented. There is no frontend; API examples and local setup instructions are provided here for reviewers and contributors.

## Current implementation

- PostgreSQL schema managed through Flyway migrations.
- UUID-based user accounts and a many-to-many relationship between users and roles.
- Seeded roles: `PATIENT`, `DOCTOR`, `NURSE`, and `ADMIN`.
- Case-insensitive email uniqueness enforced by a database index.
- Registration service logic that checks existing emails, hashes passwords with BCrypt, assigns the `PATIENT` role, and saves the account.
- Registration request and response DTOs, including request validation annotations.
- Centralized exception handling with `application/problem+json` responses and a shared response factory.
- Docker Compose configuration for the authentication database, including a health check.

Registration is not yet exposed as a working HTTP endpoint: the controller method currently needs its `@PostMapping("/register")` annotation. Login, token issuance, and role-based endpoint authorization are not implemented yet.

## Technology stack

| Technology | Purpose |
| --- | --- |
| Java 25 | Application language and runtime |
| Spring Boot 4.1.1 / Spring MVC | Backend application and HTTP API |
| Spring Data JPA / Hibernate | Persistence and entity mapping |
| PostgreSQL 18 | Authentication database |
| Flyway | Versioned database migrations |
| Spring Security Crypto | BCrypt password hashing |
| Jakarta Validation | Request validation constraints |
| Lombok | Constructors, accessors, and builders |
| Maven Wrapper | Build and dependency management |
| Docker Compose | Local database setup |

Spring Security Crypto supplies password hashing; a complete authentication and authorization configuration is still planned.

## Repository structure

```text
.
├── README.md
├── compose.yaml
├── platform/
└── services/
    └── auth-service/
        ├── pom.xml
        ├── mvnw
        ├── mvnw.cmd
        └── src/
            ├── main/
            │   ├── java/org/example/authservice/
            │   │   ├── config/
            │   │   ├── controller/
            │   │   ├── dto/
            │   │   ├── entity/
            │   │   ├── exception/
            │   │   ├── handler/
            │   │   ├── repository/
            │   │   └── service/
            │   └── resources/
            │       ├── application.yaml
            │       └── db/migration/
            └── test/
```

The auth service owns account credentials and role assignments. Patient profiles, appointments, and staff scheduling are intended to become separate service responsibilities as the project grows.

## Run locally

### Prerequisites

- JDK 25, with `JAVA_HOME` configured.
- Docker Desktop or Docker Engine with Docker Compose.
- Git and a terminal. Maven is available through the included wrapper.
- Port `5432` available for PostgreSQL and port `8080` for the application.

The commands below use PowerShell and assume the repository root is the starting directory.

### 1. Configure the database

Create a local `.env` file beside `compose.yaml`:

```dotenv
AUTH_DB_PASSWORD=replace_with_your_local_database_password
```

Use your own password. The repository ignores `.env` files; do not commit real credentials.

The Compose configuration currently uses this Windows bind mount:

```yaml
volumes:
  - "G:/DockerData/hospital-management-system/auth-db:/var/lib/postgresql"
```

Before starting Docker, change the host path on the left to a persistent directory on your machine if needed. Keep the container path `/var/lib/postgresql` for this PostgreSQL 18 image.

Start the database:

```powershell
docker compose up -d auth-db
docker compose ps
```

Wait until `auth-db` reports healthy. If startup fails, inspect its logs:

```powershell
docker compose logs auth-db
```

| Setting | Local value |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `hospital_auth` |
| Username | `postgres` |
| Password | Your `AUTH_DB_PASSWORD` value |

The database port is bound to `127.0.0.1`. Data persists in the host directory configured above. Changing `.env` does not change the password of a database that has already been initialized.

### 2. Configure and start the application

Docker Compose reads the root `.env` file, but the Spring application does not automatically load that file. Supply the same password to the application process:

```powershell
$env:AUTH_DB_PASSWORD = "replace_with_your_local_database_password"
cd services/auth-service
.\mvnw.cmd spring-boot:run
```

For IntelliJ IDEA, set `AUTH_DB_PASSWORD` in the application's Run Configuration under **Environment variables**, then run `AuthServiceApplication`.

The application uses `http://localhost:8080` by default and connects to `jdbc:postgresql://localhost:5432/hospital_auth`. Flyway applies pending migrations on startup.

For Bash, set the variable with `export AUTH_DB_PASSWORD='your-local-password'` and run `./mvnw spring-boot:run` from the service directory.

## Database design

| Table | Responsibility |
| --- | --- |
| `users` | Email, password hash, enabled state, and timestamps |
| `roles` | Available role names |
| `user_roles` | Links users to one or more roles |

The initial schema is defined in [V1__initial_migration.sql](services/auth-service/src/main/resources/db/migration/V1__initial_migration.sql).

The unique index on `LOWER(email)` prevents case-only email duplicates. Check constraints reject blank values and emails with leading or trailing spaces; those constraints do not modify input. Registration trims the email in application code before saving it.

Add future schema changes as new migration files, such as `V2__describe_change.sql`, rather than editing a migration already applied to an existing database.

## Registration API contract — in progress

The intended endpoint is:

```http
POST /api/v1/users/register
Content-Type: application/json
```

**Current limitation:** `UserController.register()` does not yet have a method-level request mapping. The example below describes the intended contract and will only work after that mapping is added and the application is restarted.

Request:

```json
{
  "email": "kamal@example.com",
  "password": "MyTestPassword123!"
}
```

Expected successful response: **201 Created**

```json
{
  "id": "b7a0e199-c465-4f17-a9bc-bf451618a5af",
  "email": "kamal@example.com"
}
```

The ID above is illustrative. The backend assigns `PATIENT`; the request does not accept a role. Neither the password nor its hash is included in the registration response.

After the route is enabled, try it with PowerShell:

```powershell
$body = @{
    email = "kamal@example.com"
    password = "MyTestPassword123!"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8080/api/v1/users/register" `
    -ContentType "application/json" `
    -Body $body
```

### Error responses

The current custom handlers map `DuplicateUserException` to **409 Conflict**, `UserNotFoundException` to **404 Not Found**, and unexpected exceptions to a generic **500 Internal Server Error** response.

Illustrative duplicate-user response:

```json
{
  "type": "about:blank",
  "title": "Duplicate User",
  "status": 409,
  "detail": "User with username 'kamal@example.com' already exists",
  "instance": "/api/v1/users/register",
  "timestamp": "2026-11-07T10:00:00Z"
}
```

Dedicated validation-error handling is still pending. The current catch-all handler can return 500 for validation errors rather than the intended 400. Concurrent duplicate registrations are prevented by the database index, but translating that constraint failure into a consistent 409 response is also pending.

## Tests

The repository currently contains a Spring Boot application-context smoke test. Registration and API integration tests are planned; the API examples above are not test results.

With the database running and `AUTH_DB_PASSWORD` set, run from `services/auth-service`:

```powershell
.\mvnw.cmd test
```

The current test loads the application configuration and connects to the configured database, including Flyway startup. Use a local development database. An isolated test database setup is planned.

## Roadmap

- [ ] Complete the registration route and verify success and failure responses.
- [ ] Add an explicit service transaction boundary for registration.
- [ ] Return consistent validation errors and database-conflict responses.
- [ ] Add registration unit and integration tests with an isolated database.
- [ ] Implement login and token-based authentication.
- [ ] Enforce role-based access to protected endpoints.
- [ ] Allow administrators to provision doctor and nurse accounts.
- [ ] Add patient profile and appointment services.
- [ ] Add staff availability and leave workflows.
- [ ] Publish OpenAPI documentation and a reusable API request collection.

## Project scope

This is an evolving backend learning and portfolio project. The current repository demonstrates the foundations of persistence, migrations, registration logic, and structured error handling. A complete distributed hospital system and production deployment are future work.
