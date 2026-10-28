# Spendly — Expense Tracker

A beginner-friendly full-stack expense tracker built with **Spring Boot 3, Java 17, Spring Security, JWT, Spring Data JPA, MySQL, HTML, CSS and vanilla JavaScript**. The frontend talks to the real API; it does not use demo transactions.

## Features

- Sign up and sign in with JWT authentication; passwords are stored as BCrypt hashes.
- Personal expense create, list, update and delete.
- Personal categories, with seven useful starter categories created on signup; create extra categories from the category panel.
- Filter transactions by title/category/date and show a period summary.
- Category-wise spending breakdown and totals.
- Every expense/category operation is scoped to the signed-in user.
- Centralized API error responses with field-level validation messages.
- Docker Compose setup for frontend, backend and MySQL.
- Basic service-layer unit tests.

## Quick start (recommended)

Requirements: Docker Desktop with Docker Compose.

From the project root:

```bash
docker compose up --build
```

Open the frontend at **http://localhost:5500**. The API is at **http://localhost:8080/api**.

Health check:

```bash
curl http://localhost:8080/api/health
```

Stop services:

```bash
docker compose down
```

To remove the local database too (this deletes saved accounts and expenses):

```bash
docker compose down -v
```

The Docker credentials and JWT secret are for local development only. Do not use them for a public deployment.

## Run locally without Docker

Requirements: Java 17+, Maven 3.9+, MySQL 8+.

1. Create the database:

   ```sql
   CREATE DATABASE expense_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Set environment variables as needed:

   - `DB_URL=jdbc:mysql://localhost:3306/expense_tracker?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - `DB_USERNAME=root`
   - `DB_PASSWORD=your_mysql_password`
   - `JWT_SECRET=replace-with-a-random-secret-at-least-32-characters-long`
   - `CORS_ALLOWED_ORIGIN=http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://127.0.0.1:3000`

3. Start the backend:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. Serve `frontend/` using VS Code Live Server on port 5500 (or any port listed in `CORS_ALLOWED_ORIGIN`). Open the local URL shown by Live Server. Do not open `index.html` as a `file://` URL because browsers block API calls from that origin.

## API endpoints

Except for signup, login and health, endpoints require `Authorization: Bearer <token>`.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Health check |
| POST | `/api/auth/signup` | Register and create starter categories |
| POST | `/api/auth/login` | Sign in and receive JWT |
| GET, POST | `/api/categories` | List/create your categories |
| PUT, DELETE | `/api/categories/{id}` | Update/delete your category |
| GET, POST | `/api/expenses` | List/create expenses; list supports `categoryId`, `from`, `to` |
| GET, PUT, DELETE | `/api/expenses/{id}` | Read/update/delete one of your expenses |
| GET | `/api/summary` | Total and per-category summary; supports `from`, `to` |

Dates use ISO format `YYYY-MM-DD`. Expense amount must be positive and have at most two decimal places.

### Example signup

`POST http://localhost:8080/api/auth/signup`

```json
{
  "name": "Demo User",
  "email": "demo@example.com",
  "password": "StrongPass123"
}
```

The response includes a JWT and a user object. The frontend stores the token in browser local storage and sends it in the `Authorization` header on protected API calls. Sign out clears the local session.

### Consistent error format

API errors use one shape, including validation errors:

```json
{
  "timestamp": "2026-10-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Please check the submitted fields.",
  "path": "/api/expenses",
  "fields": { "title": "must not be blank" }
}
```

Expected business errors use `ApiException`; `GlobalExceptionHandler` maps application and validation exceptions to this format. Spring Security's unauthorized/forbidden responses use the same response shape.

## Project layout

```text
expense-tracker-full/
├── backend/
│   ├── src/main/java/com/golu/expense/
│   │   ├── controller/   # HTTP endpoints only
│   │   ├── dto/          # Request/response models
│   │   ├── entity/       # JPA entities
│   │   ├── exception/    # ApiException, ApiError, GlobalExceptionHandler
│   │   ├── repository/   # Database queries
│   │   ├── security/     # JWT filter/service and Spring Security config
│   │   └── service/      # Business logic
│   ├── src/main/resources/db/
│   ├── src/test/java/    # Service tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/             # HTML, CSS, JS, and frontend Dockerfile
├── docker-compose.yml
└── README.md
```

## Database schema note

The running application is configured for **MySQL**, matching the assignment's core requirement. A PostgreSQL reference schema is also included at `backend/src/main/resources/db/schema-postgresql.sql` to address the submission note requesting a PostgreSQL schema/dump. That SQL file is a reference for PostgreSQL; it is not used by the MySQL runtime configuration.

For a production deployment, replace `spring.jpa.hibernate.ddl-auto=update` with a migration-based workflow (Flyway/Liquibase) and `ddl-auto=validate`, use environment-managed secrets, and configure HTTPS.

## Run tests

From the `backend/` directory:

```bash
mvn test
```

The tests cover date/category filter forwarding and invalid date range handling. Before submitting, run the project on your own machine, create an account, and verify add/edit/delete/filter flows in the browser.
