# AgroDairy AI

AI-powered dairy & agriculture operations platform — animal/milk production tracking, FEFO-managed inventory, e-commerce ordering, recurring subscriptions with automated delivery generation, statistical anomaly detection, and an XGBoost demand-forecasting service.

## Architecture

```
frontend/    React + TypeScript + Vite          → http://localhost:5173  (run with `npm run dev`, NOT in Docker)
backend/     Spring Boot 4.1 (Java 17)          → http://localhost:8080  (Docker)
ai-service/  FastAPI (Python)                   → http://localhost:8000  (Docker, internal-only in production)
postgres     PostgreSQL 16                      → localhost:5432         (Docker)
```

The backend is the only service the frontend talks to directly. The backend calls ai-service internally (`X-Internal-Api-Key` header) for demand forecasts.

## Two ways to run this

| | **Mode A — Docker** | **Mode B — Native (no Docker)** |
|---|---|---|
| postgres | Docker container | your local PostgreSQL 17 install |
| backend | Docker container | `mvn spring-boot:run` with the `local` Spring profile |
| ai-service | Docker container | `uvicorn` from a local Python venv |
| frontend | always `npm run dev` (never Docker) — same either way | always `npm run dev` (never Docker) — same either way |
| Good for | quickest start, matches how it'll deploy | debugging with breakpoints, faster edit-compile-run loop, offline/no-Docker machines |

Both modes are fully supported and use the **same dev credentials** (`agrodairy`/`devpassword`, admin seed, internal API key) — pick whichever you want at any time. They just don't share a live database: Mode A's data lives in the Docker volume, Mode B's lives in your native Postgres. See **Local Postgres backup/restore** below if you want to copy data between them.

**Don't run both backend/ai-service at once** — they'd both try to bind ports 8080/8000. Postgres is the one exception: Docker's Postgres and your native Postgres 17 can coexist on port 5432 without conflict (see **Troubleshooting** for why), so it's fine to leave Docker's Postgres running even while working in Mode B.

## Prerequisites

| Tool | Needed for | Check with |
|---|---|---|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Mode A | `docker --version` |
| [Node.js 20.x](https://nodejs.org/) + npm | frontend (both modes) | `node --version` |
| [PostgreSQL 17](https://www.postgresql.org/download/) | Mode B | already installed |
| JDK 17+ and Maven | Mode B backend | `java -version`, `mvn -version` |
| Python 3.12 + the `ai-service/venv` virtualenv | Mode B ai-service | already set up |
| [VS Code](https://code.visualstudio.com/) | editor | — |

For **Mode A** you don't need Java, Maven, or Python installed at all — the backend and ai-service run inside their Docker images, which bring their own runtimes.

Recommended VS Code extensions (it will prompt you, or install manually via `Ctrl+Shift+X`):
- **ESLint** (`dbaeumer.vscode-eslint`) — lints `frontend/`
- **Tailwind CSS IntelliSense** (`bradlc.vscode-tailwindcss`) — autocomplete for `frontend/`'s Tailwind classes
- **Extension Pack for Java** (`vscjava.vscode-java-pack`) — needed for editing/debugging `backend/`, including Mode B's launch config
- **Python** (`ms-python.python`) — needed for editing/debugging `ai-service/`, including Mode B's launch config
- **Docker** (`ms-azuretools.vscode-docker`) — view/manage containers from the sidebar, useful for Mode A

## Open the project in VS Code

```
code C:\Users\91886\Desktop\proj
```

This repo already ships `.vscode/launch.json` and `.vscode/tasks.json` with one-click configs for everything below (Run and Debug panel, or `Ctrl+Shift+P` → "Tasks: Run Task").

## Mode A — With Docker

**1. Configure the frontend's env file** (one-time):
```
cd frontend
copy .env.example .env
```
`frontend/.env` should contain `VITE_API_BASE_URL=http://localhost:8080`. The backend/ai-service get their config straight from `docker-compose.yml` — nothing to edit there.

**2. Start Docker Desktop**, then from the repo root (VS Code terminal, `` Ctrl+` ``, or Task: "Docker: Start All"):
```
docker compose up -d --build
```
First build takes a few minutes (Maven/pip downloads); after that it's fast. Check health:
```
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health
```
You should see `"status":"UP"` and `"status":"ok"`. Logs: `docker compose logs -f backend` (or `ai-service`).

**3. Start the frontend** in a second terminal (or Task: "Frontend: Run Dev Server"):
```
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**.

**Stop it**: `docker compose down` (keeps the DB volume) or `docker compose down -v` (also wipes the database). `Ctrl+C` the frontend terminal.

## Mode B — Without Docker (native)

Everything runs directly on your machine: your local **PostgreSQL 17** service, the backend via Maven, ai-service via its Python venv, and the frontend the same way as Mode A.

**1. Database** — if you haven't already, restore a copy of the schema+data into your local Postgres (see **Local Postgres backup/restore** below). This only needs doing once; after that the `local` profile below just points at it.

**2. Configure `ai-service/.env`** (one-time — already created for you, mirrors `docker-compose.yml`'s dev values but pointing at `localhost` instead of the Docker network):
```
DATABASE_URL=postgresql://agrodairy:devpassword@localhost:5432/agrodairy
INTERNAL_API_KEY=dev-only-internal-key
```

**3. Start the backend** with the `local` Spring profile, which points it at `localhost:5432` and `http://localhost:8000` instead of the Docker service names:
- **VS Code**: Run and Debug panel → **"Backend (local, no Docker)"** (F5), or
- **Terminal** / Task "Local: Run Backend (no Docker)":
  ```
  cd backend
  mvn spring-boot:run -Dspring-boot.run.profiles=local
  ```

**4. Start ai-service**:
- **VS Code**: Run and Debug panel → **"AI Service (local, no Docker)"** (F5), or
- **Terminal** / Task "Local: Run AI Service (no Docker)":
  ```
  cd ai-service
  venv\Scripts\activate
  uvicorn app.main:app --reload --port 8000
  ```

**5. Start the frontend** — identical to Mode A (`cd frontend && npm run dev`).

**Stop it**: `Ctrl+C` in each terminal, or stop the debug sessions in VS Code.

## Log in (either mode)

An admin account is seeded automatically on first backend startup:

| Email | Password |
|---|---|
| `admin@agrodairy.local` | `dev-only-admin-change-me` |

Log in as admin, then use **Users** in the admin sidebar to create STAFF accounts. Anyone can self-register as a CUSTOMER from the **Sign up** page.

## Local Postgres backup/restore

To copy the Docker database's data into your local Postgres 17 (or refresh it later):
```powershell
# 1. Dump from the running Docker container
docker exec proj-postgres-1 pg_dump -U agrodairy -d agrodairy --no-owner --no-privileges > agrodairy_backup.sql

# 2. One-time: create the role + database locally (skip if they already exist)
$env:PGPASSWORD = "<your local postgres superuser password>"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -c "CREATE ROLE agrodairy LOGIN PASSWORD 'devpassword';"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE agrodairy OWNER agrodairy;"

# 3. Restore (drop/recreate the database first if this isn't the first time)
$env:PGPASSWORD = "devpassword"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U agrodairy -h localhost -d agrodairy -f agrodairy_backup.sql
```
`agrodairy_backup.sql` is gitignored — it contains real data, never commit it. This is a one-way snapshot copy: Mode A and Mode B's databases don't stay in sync automatically; re-run this whenever you want Mode B to catch up to Mode A's current data.

## Training the forecasting model (optional)

The admin **Forecast** page needs a trained model. `ai-service/app/models/demand_model.joblib` is already trained and present on this machine (used by both modes — it's just a file on disk, not something Docker-specific); regenerate it only if you want to retrain from scratch:

**Mode A:**
```
docker compose run --rm ai-service python training/generate_seed_data.py
docker compose run --rm ai-service python training/train_demand_model.py
docker compose restart ai-service
```

**Mode B:**
```
cd ai-service
venv\Scripts\activate
python training/generate_seed_data.py
python training/train_demand_model.py
```
Then restart the ai-service process (Ctrl+C, rerun) so it picks up the new model file.

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

```
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

- **`docker compose up` fails / hangs** — make sure Docker Desktop is actually running (its whale icon in the system tray should be steady, not animating).
- **Port already in use (8080/8000/5173)** — something else (maybe Mode B's backend/ai-service, still running) is using that port; stop it first, or change the port mapping in `docker-compose.yml` / `vite.config.ts`.
- **Port 5432 "conflict" between Docker and native Postgres** — on Windows, Docker's proxy binds the IPv6 wildcard (`::`) while a native Postgres service binds the IPv4 wildcard (`0.0.0.0`); both can listen on "5432" at once without erroring, and in practice the native service tends to win connections from `localhost`/`127.0.0.1`. This is harmless for this project (the backend always reaches Docker's Postgres via the internal Docker network name `postgres`, never through the host port) — it only matters if you connect a host tool (psql, pgAdmin) to port 5432 and need to be sure which server you're actually hitting; check with `SELECT version();` (Docker image is Postgres 16, native install here is 17).
- **Frontend shows network errors** — confirm `frontend/.env` has `VITE_API_BASE_URL=http://localhost:8080`, and that *something* (Docker's backend or Mode B's `mvn spring-boot:run`) is actually up on port 8080.
- **Login fails right after starting the backend** — it takes a few seconds to finish Flyway migrations and seed the admin account; wait ~10s and retry, or check the backend's logs (`docker compose logs backend` for Mode A, the terminal/Debug Console for Mode B).
- **Mode A: changes to backend/ai-service code don't show up** — those run inside Docker images; after editing, rebuild with `docker compose up -d --build backend` (or `ai-service`). Mode B doesn't have this problem — Maven/uvicorn `--reload` pick up changes directly.
- **Mode B backend won't start / wrong DB** — confirm `spring.profiles.active=local` is actually set (VS Code launch config sets it automatically; via terminal it's the `-Dspring-boot.run.profiles=local` flag) and that your local Postgres has the `agrodairy` database (see **Local Postgres backup/restore**).
- The frontend, in either mode, hot-reloads instantly since it always runs directly via `npm run dev`, never in Docker.
