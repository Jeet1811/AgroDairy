# AgroDairy AI

AI-powered dairy & agriculture operations platform — animal/milk production tracking, FEFO-managed inventory, e-commerce ordering, recurring subscriptions with automated delivery generation, statistical anomaly detection, and an XGBoost demand-forecasting service.

**Live demo:** https://agro-dairy.vercel.app

## Architecture

```
frontend/    React + TypeScript + Vite          → runs locally with `npm run dev`
backend/     Spring Boot 4.1 (Java 17)           → owns the database, auth, and every REST API
ai-service/  FastAPI (Python) + XGBoost          → internal-only, called by the backend for demand forecasts
postgres     PostgreSQL                          → single source of truth
```

The frontend only ever talks to the backend. The backend calls `ai-service` internally (authenticated with a shared internal API key) for demand forecasts; nothing else talks to `ai-service` directly.

## Quick start (Docker)

**Prerequisites:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) and [Node.js 20.x](https://nodejs.org/).

```bash
# 1. One-time frontend config
cd frontend
copy .env.example .env      # sets VITE_API_BASE_URL=http://localhost:8080
cd ..

# 2. Start postgres + backend + ai-service
docker compose up -d --build

# 3. Start the frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Check the backend/ai-service came up healthy with:
```bash
curl http://localhost:8080/actuator/health   # {"status":"UP"}
curl http://localhost:8000/health            # {"status":"ok"}
```

**Stop it:** `docker compose down` (keeps the database volume) or `docker compose down -v` (also wipes it). `Ctrl+C` the frontend terminal.

## Log in

An admin account is seeded automatically on first backend startup:

| Email | Password |
|---|---|
| `admin@agrodairy.local` | `dev-only-admin-change-me` |

Log in as admin, then use **Users** in the admin sidebar to create STAFF accounts. Anyone can self-register as a CUSTOMER from the **Sign up** page.

## Project structure

```
backend/      Spring Boot API — auth, animals, products, inventory, orders, subscriptions,
              deliveries, AI-alert endpoints, analytics, notifications
              src/main/java/com/agrodairy/<module>/{entity,repository,dto,service,controller}
              src/main/resources/db/migration/   Flyway SQL migrations
              src/test/                          JUnit + Mockito + Testcontainers tests

ai-service/   FastAPI — /predict/demand (XGBoost), /health
              app/                               API code
              training/                          seed-data generator + training/eval scripts

frontend/     React + TS + Vite + Tailwind + React Query
              src/api/          one file per backend resource (typed HTTP calls)
              src/types/        TypeScript types mirroring backend DTOs
              src/pages/public|customer|admin/    route-level page components
              src/components/   shared layout + UI primitives
              src/context/      AuthContext, ToastContext

docker-compose.yml   orchestrates postgres + backend + ai-service for local dev
```

## Running tests

```bash
cd backend && mvn test          # JUnit/Mockito unit tests + Testcontainers integration tests
cd frontend && npm run lint     # ESLint
cd frontend && npx tsc -b       # TypeScript type-check
```

Backend integration tests spin up a throwaway Postgres container automatically via Testcontainers — Docker Desktop must be running.

## Using the application

### As a customer
1. Register at `/register`, or log in if you already have an account.
2. Browse **Shop** (`/products`) — filter by category, search, view a product's detail page and reviews.
3. Add items to your **Cart**, then **Checkout** with a delivery address to place an order.
4. Track the order under **Orders** — see its live status (Pending → Confirmed → … → Delivered) and cancel while still Pending.
5. Set up a recurring **Subscription** (daily / alternate-day / custom weekdays) from the Subscriptions page — pause, resume, skip a date, or cancel any time.
6. **Account** shows your profile and upcoming scheduled deliveries.

### As staff/admin
Log in with a STAFF or ADMIN account to reach `/admin`:
- **Dashboard** — live KPIs (revenue, orders, milk production, inventory value) and 30-day trend charts.
- **Animals** — manage the herd; open an animal to record daily milk production per session (morning/evening). A statistically abnormal drop (vs. the animal's own 21-day baseline) automatically raises an entry on the **Anomalies** page.
- **Products** / categories — manage the catalogue and pricing.
- **Inventory** — batch-level stock with expiry dates; the FEFO (first-expiry-first-out) rule automatically allocates the oldest stock first when customers order. The alert panels flag expiring-soon, low-stock, and out-of-stock items.
- **Orders** — progress any order through its status pipeline; the customer gets a notification on every change.
- **Subscriptions** — monitor all customers' active subscriptions.
- **Deliveries** — tomorrow's deliveries are generated automatically every evening from active subscriptions; view/update delivery status and see the daily pick-list summary (total items needed per product).
- **Anomalies** — review and acknowledge flagged production drops (worded as "review recommended," never a diagnosis).
- **Forecast** — pick a product to see its AI-predicted demand for the next N days, with a confidence band.
- **Users** (ADMIN only) — create new STAFF/ADMIN accounts.

## Troubleshooting

- **`docker compose up` fails or hangs** — make sure Docker Desktop is actually running.
- **Port already in use (8080/8000/5173)** — something else is already using it; stop that first.
- **Frontend shows network errors** — confirm `frontend/.env` has `VITE_API_BASE_URL=http://localhost:8080`, and that the backend is actually up on port 8080.
- **Login fails right after starting the backend** — it takes a few seconds to finish Flyway migrations and seed the admin account; wait ~10s and retry.
- **Changed backend/ai-service code but don't see it** — those run inside Docker images; rebuild with `docker compose up -d --build backend` (or `ai-service`).
