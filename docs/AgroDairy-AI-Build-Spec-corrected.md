# AgroDairy AI — Build Specification

**This document is self-contained and written to be handed directly to an AI coding assistant (Claude Code or similar) to scaffold and implement the entire project.** It supersedes the earlier narrative spec by adding exact schemas, contracts, configs, and a step-by-step build order. Read this file, not any earlier one.

---

## 0. Instructions for the AI Agent Implementing This

- Build in the exact phase order given in §16. Do not start AI features (§9) before the core CRUD backend (§8) works and is tested.
- Every entity, field, and endpoint below is intentionally exact — implement as specified rather than inventing alternatives, so the result matches this spec.
- All JSON uses `camelCase`; all database columns use `snake_case` (Hibernate's default naming strategy handles this — do not override it).
- All timestamps are UTC, ISO-8601 (`2026-07-15T09:30:00Z`). All IDs are UUIDv4 (`gen_random_uuid()`, requires `pgcrypto`).
- Use the response/error envelopes in §7 for **every** endpoint, no exceptions.
- If a dependency version isn't pinned, use the current stable release at build time.
- After each phase in §16, run the phase's tests before moving to the next phase. Do not implement two phases in one pass.

---

## 1. Project Overview

**AgroDairy AI** digitizes a dairy/agriculture business end to end — animal → milk production → product → inventory → customer order/subscription — with two AI features that solve real operational problems: flagging abnormal drops in an animal's milk output, and forecasting near-term product demand.

**Roles**: `ADMIN` (full access), `STAFF` (animals, production, inventory, orders, deliveries), `CUSTOMER` (browse, order, subscribe, track own deliveries).

**Explicitly out of scope for v1** (see §17 for later phases): international B2B/export module, veterinary/vaccination records, feed management, QR traceability, payment gateway (sandbox stub only), multi-warehouse, business-analyst role, AI chat assistant, churn prediction, recommendation engine. Building these before the core loop works is how these projects stall — resist the urge.

---

## 2. Repository & Module Structure

```
agrodairy-ai/
├── backend/                          # Spring Boot
│   ├── src/main/java/com/agrodairy/
│   │   ├── AgroDairyApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── AiServiceClientConfig.java   # RestClient bean for calling ai-service
│   │   ├── common/
│   │   │   ├── exception/ (GlobalExceptionHandler, ApiException, NotFoundException, ValidationException)
│   │   │   ├── dto/ (ApiError, PageResponse<T>)
│   │   │   └── util/
│   │   ├── auth/          (controller, service, dto, entity: User, RefreshToken; repository; security: JwtService, JwtAuthFilter)
│   │   ├── animal/        (controller, service, dto, entity: Animal, MilkProductionRecord; repository)
│   │   ├── product/       (controller, service, dto, entity: ProductCategory, Product; repository)
│   │   ├── inventory/     (controller, service, dto, entity: InventoryBatch; repository)
│   │   ├── order/         (controller, service, dto, entity: Cart, CartItem, Order, OrderItem; repository)
│   │   ├── subscription/  (controller, service, dto, entity: Subscription, SubscriptionSkipDate; repository; scheduler: DailyDeliveryGeneratorJob)
│   │   ├── delivery/      (controller, service, dto, entity: Delivery; repository)
│   │   ├── review/        (controller, service, dto, entity: Review; repository)
│   │   ├── ai/             (controller: AiController; client: AiServiceClient; dto; entity: AnomalyAlert, ForecastLog; repository; service: AnomalyDetectionService)
│   │   ├── analytics/     (controller, service, dto)
│   │   └── notification/  (entity, repository, service)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/  (Flyway scripts, V1__init.sql, V2__seed_reference_data.sql, ...)
│   ├── src/test/java/com/agrodairy/...
│   ├── pom.xml
│   └── Dockerfile
├── ai-service/                       # FastAPI
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py                 # reads DATABASE_URL, INTERNAL_API_KEY from env
│   │   ├── security.py               # verifies X-Internal-Api-Key header
│   │   ├── db.py                     # read connection to the same Postgres instance
│   │   ├── schemas.py                # Pydantic request/response models
│   │   ├── routers/
│   │   │   ├── forecast.py           # /predict/demand, /predict/production
│   │   │   ├── anomaly.py            # /detect/anomaly
│   │   │   └── health.py             # /health
│   │   ├── services/
│   │   │   ├── forecasting_service.py
│   │   │   └── anomaly_service.py
│   │   └── models/                   # trained .joblib artifacts live here
│   ├── training/
│   │   ├── generate_seed_data.py
│   │   ├── train_demand_model.py
│   │   └── evaluate_model.py
│   ├── requirements.txt
│   └── Dockerfile
├── frontend/                         # React + TypeScript
│   ├── src/
│   │   ├── api/           (axios instance + typed endpoint functions, one file per module)
│   │   ├── components/    (shared: DataTable, Modal, StatCard, Chart wrappers, ProtectedRoute)
│   │   ├── pages/          (see §10 for exact route list)
│   │   ├── contexts/AuthContext.tsx
│   │   ├── hooks/
│   │   ├── types/          (mirrors backend DTOs)
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml                # postgres + backend + ai-service, for local dev
├── .github/workflows/ci.yml
└── docs/
    └── AgroDairy-AI-Build-Spec.md    # this file
```

---

## 3. Dependencies

**backend/pom.xml** — Spring Boot **4.1.x parent** (current stable as of mid-2026, built on Spring Framework 7 — not 3.x, which is now the previous major line), Java 17+:
- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`
- `org.postgresql:postgresql` (runtime)
- `org.flywaydb:flyway-core` + `flyway-database-postgresql`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` for issuing/parsing JWTs
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` **major version 3.x** (Swagger UI, served at `/swagger-ui.html` or the newer Scalar UI) — springdoc 2.x only supports Spring Boot 3; Boot 4 requires springdoc 3.x, so pin that explicitly or the build will fail on a stale cached version
- `org.projectlombok:lombok`
- `RestClient` (already in `spring-web`) for calling ai-service — no reactive stack needed
- Test scope: `spring-boot-starter-test`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`

**ai-service/requirements.txt**:
```
fastapi
uvicorn[standard]
pydantic
psycopg[binary]
pandas
numpy
scikit-learn
xgboost
joblib
python-dotenv
pytest
httpx
```

**frontend/package.json** (key deps): `react`, `react-dom`, `react-router-dom`, `axios`, `@tanstack/react-query`, `recharts`, `typescript`, `vite`, `tailwindcss`

---

## 4. Configuration

**backend `application.yml`** (env-driven, identical locally and deployed):
```yaml
spring:
  application:
    name: agrodairy-backend
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway owns the schema, never let Hibernate auto-migrate
    open-in-view: false
  flyway:
    enabled: true

server:
  port: ${PORT:8080}

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-ttl-minutes: 15
    refresh-token-ttl-days: 7
  ai-service:
    base-url: ${AI_SERVICE_URL:http://ai-service:8000}
    internal-api-key: ${INTERNAL_API_KEY}
  cors:
    allowed-origins: ${FRONTEND_URL:http://localhost:5173}

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**Environment variables (all services)**:
| Variable | Used by | Example |
|---|---|---|
| `DATABASE_URL` | backend, ai-service | `jdbc:postgresql://host:5432/agrodairy` (backend) / `postgresql://host:5432/agrodairy` (ai-service) |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | backend, ai-service | — |
| `JWT_SECRET` | backend | 256-bit random string, never committed |
| `INTERNAL_API_KEY` | backend, ai-service | shared secret, never committed |
| `AI_SERVICE_URL` | backend | `http://ai-service:8000` locally, internal service URL in prod |
| `FRONTEND_URL` | backend | for CORS allow-list |
| `VITE_API_BASE_URL` | frontend | public backend URL |

**docker-compose.yml** (local dev — three services + Postgres):
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: agrodairy
      POSTGRES_USER: agrodairy
      POSTGRES_PASSWORD: devpassword
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]

  backend:
    build: ./backend
    ports: ["8080:8080"]
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/agrodairy
      DATABASE_USERNAME: agrodairy
      DATABASE_PASSWORD: devpassword
      JWT_SECRET: dev-only-secret-change-me
      AI_SERVICE_URL: http://ai-service:8000
      INTERNAL_API_KEY: dev-only-internal-key
      FRONTEND_URL: http://localhost:5173
    depends_on: [postgres]

  ai-service:
    build: ./ai-service
    ports: ["8000:8000"]     # exposed locally for dev/debugging only — NEVER exposed in prod, see §14
    environment:
      DATABASE_URL: postgresql://agrodairy:devpassword@postgres:5432/agrodairy
      INTERNAL_API_KEY: dev-only-internal-key
    depends_on: [postgres]

volumes:
  pgdata:
```

Both `backend/Dockerfile` and `ai-service/Dockerfile` should be multi-stage (build stage + slim runtime stage) to keep images small.

---

## 5. Database Schema (Flyway `V1__init.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','STAFF','CUSTOMER')),
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE animals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag VARCHAR(20) UNIQUE NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('COW','BUFFALO')),
    breed VARCHAR(100),
    date_of_birth DATE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','MILKING','PREGNANT','DRY_PERIOD','UNDER_OBSERVATION','SOLD','DECEASED')),
    lactation_status VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE milk_production_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    production_date DATE NOT NULL,
    session VARCHAR(10) NOT NULL CHECK (session IN ('MORNING','EVENING')),
    quantity_litres NUMERIC(6,2) NOT NULL CHECK (quantity_litres >= 0),
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (animal_id, production_date, session)
);
CREATE INDEX idx_milk_records_animal_date ON milk_production_records(animal_id, production_date);

CREATE TABLE product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('DAIRY','AGRICULTURE'))
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES product_categories(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    unit VARCHAR(20) NOT NULL,
    shelf_life_days INTEGER,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inventory_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    batch_code VARCHAR(50) UNIQUE NOT NULL,
    manufacture_date DATE NOT NULL,
    expiry_date DATE,
    quantity_produced INTEGER NOT NULL,
    quantity_available INTEGER NOT NULL CHECK (quantity_available >= 0),
    quantity_sold INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','EXPIRED','SOLD_OUT','DISCARDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_product_expiry ON inventory_batches(product_id, expiry_date);

CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','PROCESSING','PACKED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REFUNDED')),
    total_amount NUMERIC(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    delivery_address TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user ON orders(user_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    inventory_batch_id UUID REFERENCES inventory_batches(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10,2) NOT NULL
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('DAILY','ALTERNATE_DAY','CUSTOM')),
    weekdays VARCHAR(20),
    delivery_time_slot VARCHAR(20),
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED','CANCELLED','EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);

CREATE TABLE subscription_skip_dates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    skip_date DATE NOT NULL,
    UNIQUE (subscription_id, skip_date)
);

CREATE TABLE deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id),
    subscription_id UUID REFERENCES subscriptions(id),
    delivery_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','OUT_FOR_DELIVERY','DELIVERED','FAILED')),
    delivered_at TIMESTAMPTZ,
    notes TEXT,
    CHECK (order_id IS NOT NULL OR subscription_id IS NOT NULL)
);
CREATE INDEX idx_deliveries_date ON deliveries(delivery_date, status);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, user_id)
);

CREATE TABLE anomaly_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    production_date DATE NOT NULL,
    baseline_mean NUMERIC(6,2) NOT NULL,
    baseline_stddev NUMERIC(6,2) NOT NULL,
    actual_value NUMERIC(6,2) NOT NULL,
    z_score NUMERIC(6,2) NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH')),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE forecast_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    forecast_type VARCHAR(30) NOT NULL CHECK (forecast_type IN ('DEMAND','PRODUCTION')),
    target_id UUID,
    target_date DATE NOT NULL,
    predicted_value NUMERIC(10,2) NOT NULL,
    actual_value NUMERIC(10,2),
    model_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Add `V2__seed_reference_data.sql` inserting default `product_categories` rows (e.g. Cow Milk, Buffalo Milk, Ghee, Paneer, Curd, Butter under `DAIRY`; Wheat, Rice, Vegetables under `AGRICULTURE`) and one `ADMIN` user. **Never commit a plaintext password or a hardcoded bcrypt hash of a known password to a migration file** — generate the hash with a one-off local script and paste only the resulting hash, or better, create the admin via a startup `CommandLineRunner` that reads `ADMIN_SEED_EMAIL` / `ADMIN_SEED_PASSWORD` env vars and only runs if no admin exists yet.

---

## 6. Authentication & Security

- **Password hashing**: BCrypt, strength 10+.
- **JWT claims**: `sub` (user id), `email`, `role`, `iat`, `exp`. Access token TTL 15 minutes, refresh token TTL 7 days.
- **Refresh flow**: refresh tokens are random opaque strings stored (hashed with SHA-256, not bcrypt — they're high-entropy already) in `refresh_tokens` with `expires_at` and `revoked`; rotate on every use (issue new, revoke old).
- **Filter chain**: a `JwtAuthFilter` (`OncePerRequestFilter`) reads `Authorization: Bearer <token>`, validates it, sets the `SecurityContext`. Public endpoints: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `GET /api/products/**`, `GET /api/products/{id}/reviews`. Everything else requires authentication; role checks via `@PreAuthorize("hasRole('ADMIN')")` etc. **Implementation gotcha**: `hasRole('ADMIN')` checks for the granted authority `ROLE_ADMIN`, not `ADMIN` — when building the `SecurityContext` from the JWT's `role` claim in `JwtAuthFilter`, prefix it (`new SimpleGrantedAuthority("ROLE_" + role)`), or every `@PreAuthorize("hasRole(...)")` check will silently 403 everyone. Using `hasAuthority('ADMIN')` throughout instead (no prefix) is an equally valid, arguably less error-prone alternative — pick one convention and apply it everywhere.
- **Ownership checks**: for customer-facing endpoints (own orders, own subscriptions, own cart), the service layer must verify `resource.userId == authenticatedUserId` even for authenticated requests — never trust a path/body `userId` from the client.
- **AI service protection**: every backend → ai-service request includes header `X-Internal-Api-Key: <INTERNAL_API_KEY>`. ai-service rejects requests missing/mismatching this header with `401`. In production ai-service must not be reachable from the public internet at all (§14) — the header is defense in depth, not the only control.
- **CORS**: allow only `FRONTEND_URL`, standard methods, credentials as needed for the Authorization header.

---

## 7. API Conventions

**Success envelope**:
```json
{ "success": true, "data": { }, "meta": null }
```
List endpoints use `meta` for pagination:
```json
{ "success": true, "data": [ ], "meta": { "page": 0, "size": 20, "totalElements": 143, "totalPages": 8 } }
```

**Error envelope** (used by `GlobalExceptionHandler` for validation errors, 404s, 401/403, and uncaught exceptions):
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "quantity must be greater than 0",
    "details": [ { "field": "quantity", "message": "must be greater than 0" } ]
  }
}
```
Standard codes: `VALIDATION_ERROR` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `CONFLICT` (409), `INTERNAL_ERROR` (500).

**Pagination**: `?page=0&size=20&sort=createdAt,desc` on all list endpoints.

---

## 8. Backend API Contract

### Auth
```
POST /api/auth/register
  body: { email, password, fullName, phone? }         # public registration is always CUSTOMER
  201 -> { user: {...}, accessToken, refreshToken }

POST /api/auth/login
  body: { email, password }
  200 -> { user: {...}, accessToken, refreshToken }

POST /api/auth/refresh
  body: { refreshToken }
  200 -> { accessToken, refreshToken }

POST /api/auth/logout
  body: { refreshToken }        # revokes it
  204

GET  /api/auth/me                     [auth required]
  200 -> { id, email, fullName, role, phone, isActive, createdAt }

POST /api/auth/staff                  [ADMIN only]
  body: { email, password, fullName, phone?, role }    # role: STAFF or ADMIN
  201 -> { user: {...} }
```

### Animals
```
GET    /api/animals                   [STAFF, ADMIN]     ?status=&type=&page=&size=
GET    /api/animals/{id}              [STAFF, ADMIN]
POST   /api/animals                   [STAFF, ADMIN]     { tag, type, breed?, dateOfBirth?, status, notes? }
PATCH  /api/animals/{id}               [STAFF, ADMIN]     partial update, any subset of the above fields
DELETE /api/animals/{id}              [ADMIN only]        soft check: reject if milk_production_records exist; suggest status=SOLD/DECEASED instead

GET    /api/animals/{id}/production   [STAFF, ADMIN]     ?from=&to=&page=&size=
POST   /api/animals/{id}/production   [STAFF, ADMIN]     { productionDate, session, quantityLitres }
                                                            -> triggers AnomalyDetectionService check (§9.2) after save
```

### Products & Categories
```
GET    /api/categories                [public]
POST   /api/categories                [ADMIN]            { name, kind }

GET    /api/products                  [public]           ?categoryId=&kind=&active=&search=&page=&size=
GET    /api/products/{id}             [public]
POST   /api/products                  [STAFF, ADMIN]      { categoryId, name, description?, price, unit, shelfLifeDays?, imageUrl? }
PATCH  /api/products/{id}              [STAFF, ADMIN]
DELETE /api/products/{id}             [ADMIN]             sets active=false, never hard-deletes (order_items reference it)
```

### Inventory
```
GET    /api/inventory/batches         [STAFF, ADMIN]     ?productId=&status=&expiringWithinDays=&page=&size=
POST   /api/inventory/batches         [STAFF, ADMIN]      { productId, batchCode, manufactureDate, expiryDate?, quantityProduced }
                                                            -> quantityAvailable initialized = quantityProduced
PATCH  /api/inventory/batches/{id}     [STAFF, ADMIN]      { status? }  # e.g. mark DISCARDED
GET    /api/inventory/alerts          [STAFF, ADMIN]      low-stock + expiring-soon + out-of-stock, computed on read (see §11.1)
```

### Cart & Orders
```
GET    /api/cart                      [CUSTOMER]
POST   /api/cart/items                [CUSTOMER]          { productId, quantity }
PATCH  /api/cart/items/{productId}     [CUSTOMER]          { quantity }
DELETE /api/cart/items/{productId}     [CUSTOMER]
DELETE /api/cart                      [CUSTOMER]           empties cart

POST   /api/orders                    [CUSTOMER]           { deliveryAddress }  # builds order from current cart, empties cart, allocates inventory FEFO (§11.1)
GET    /api/orders                    [CUSTOMER: own only; STAFF/ADMIN: all]  ?status=&page=&size=
GET    /api/orders/{id}               [owner, STAFF, ADMIN]
PATCH  /api/orders/{id}/status         [STAFF, ADMIN]       { status }   # enforce forward-only transitions, see §11.2
POST   /api/orders/{id}/cancel        [owner if status=PENDING; STAFF/ADMIN any time before DELIVERED]
```

### Subscriptions
```
GET    /api/subscriptions             [CUSTOMER: own; STAFF/ADMIN: all]  ?status=&page=&size=
GET    /api/subscriptions/{id}        [owner, STAFF, ADMIN]
POST   /api/subscriptions             [CUSTOMER]           { productId, quantity, frequency, weekdays?, deliveryTimeSlot?, startDate, endDate? }
PATCH  /api/subscriptions/{id}         [owner, STAFF, ADMIN] { quantity?, weekdays?, deliveryTimeSlot?, endDate? }
POST   /api/subscriptions/{id}/pause   [owner, STAFF, ADMIN]
POST   /api/subscriptions/{id}/resume  [owner, STAFF, ADMIN]
POST   /api/subscriptions/{id}/cancel  [owner, STAFF, ADMIN]
POST   /api/subscriptions/{id}/skip    [owner, STAFF, ADMIN] { skipDate }
```

### Deliveries
```
GET    /api/deliveries                [STAFF, ADMIN]      ?date=&status=&page=&size=
GET    /api/deliveries/mine           [CUSTOMER]           own deliveries, derived via order/subscription ownership
PATCH  /api/deliveries/{id}/status     [STAFF, ADMIN]       { status, notes? }
GET    /api/deliveries/summary        [STAFF, ADMIN]      ?date=   -> { totalStops, totalItemsByProduct: [{productId,name,totalQuantity}] }
```

### Reviews
```
GET    /api/products/{id}/reviews     [public]            ?page=&size=
POST   /api/products/{id}/reviews     [CUSTOMER]           { rating, comment? }   # one per user per product, enforced by unique constraint
```

### AI
```
GET    /api/ai/anomalies              [STAFF, ADMIN]      ?acknowledged=&severity=&page=&size=
PATCH  /api/ai/anomalies/{id}/ack      [STAFF, ADMIN]
GET    /api/ai/demand-forecast        [STAFF, ADMIN]      ?productId=&days=7   -> calls ai-service, logs to forecast_logs, returns predictions
```

### Analytics
```
GET    /api/analytics/dashboard       [STAFF, ADMIN]
  200 -> {
    todayMilkProductionLitres, activeAnimals, activeSubscriptions,
    monthRevenue, totalOrders, avgOrderValue, inventoryValueTotal,
    productsExpiringSoon, topSellingProducts: [{productId,name,unitsSold}],
    revenueTrend: [{date,amount}], productionTrend: [{date,litres}]
  }
```

### Notifications
```
GET    /api/notifications             [auth required]     ?isRead=&page=&size=
PATCH  /api/notifications/{id}/read    [auth required, owner only]
```

---

## 9. AI Service Contract (FastAPI)

The ai-service is **stateless per-request** except for loading `.joblib` model artifacts at startup. It reads from Postgres directly (read-only role recommended: `CREATE ROLE ai_reader WITH LOGIN PASSWORD '...' NOINHERIT; GRANT SELECT ON ALL TABLES IN SCHEMA public TO ai_reader;`) for feature computation, and returns predictions — it never writes to the database. The backend is the only writer (it persists `forecast_logs` and `anomaly_alerts` itself after receiving the ai-service response).

### 9.1 Demand Forecast
```
POST /predict/demand
  headers: X-Internal-Api-Key: <key>
  body: { "productId": "uuid", "horizonDays": 7 }
  200 -> {
    "productId": "uuid",
    "modelVersion": "demand-xgb-v1",
    "predictions": [
      { "date": "2026-07-16", "predictedQuantity": 428.0, "confidenceLow": 380.0, "confidenceHigh": 470.0 }
    ]
  }
```
**Approach**: train one XGBoost regressor per product (or one multi-output model keyed by `productId` as a categorical feature — simpler to maintain, prefer this for v1). Features per row (one row = one product-day): `dayOfWeek`, `month`, `isWeekend`, `rollingMean7`, `rollingMean14`, `lag1`, `lag7`, `activeSubscriptionQuantitySum` (deterministic, computed from `subscriptions` table — this is a strong signal, use it). Target: `quantitySoldThatDay` (orders + subscription deliveries combined). Train on `training/train_demand_model.py`, reading from `orders`/`order_items`/`deliveries`; if there's insufficient real order history (which there will be, initially), `training/generate_seed_data.py` must generate a synthetic but plausible 6–12 months of daily order history with weekly seasonality and mild trend/noise so the model has something to learn from. Retrain manually for v1 (a scheduled retrain job is a Phase 2 item, §17).

### 9.2 Anomaly Detection
This can run either in ai-service or directly in backend Java — **implement it in backend** (`AnomalyDetectionService`) since it's a pure statistical calculation with no need for Python's ML stack, and keeping it in the write-path service avoids a network hop on every production record insert. ai-service's `/detect/anomaly` route is optional/unused in v1; leave it stubbed and rely on the Java implementation below.

**Algorithm** (triggered synchronously after every `POST /api/animals/{id}/production`):
1. Fetch the animal's last 21 days of records for the same `session` (MORNING vs EVENING compared separately — they have different baselines).
2. Require at least 7 data points; otherwise skip (not enough history yet).
3. Compute `mean` and `stddev` (population stddev) of `quantityLitres` over that window, **excluding today's just-inserted record**.
4. `zScore = (todayValue - mean) / stddev` (if `stddev == 0`, treat any deviation ≥ 1 litre as `HIGH` severity directly, skip z-score).
5. Classify: `|z| < 2` → no alert. `2 ≤ |z| < 3` → `LOW`. `3 ≤ |z| < 4` → `MEDIUM`. `|z| ≥ 4` → `HIGH`. Only alert on **decreases** (`todayValue < mean`) — a positive spike is not an operational concern for v1.
6. On alert, insert into `anomaly_alerts` and create a `notifications` row for all ADMIN/STAFF users: `"Operational Alert: {tag} produced {actual}L vs a {mean}L average — review recent records."` Never phrase this as a diagnosis (see §11.3).

### 9.3 Health
```
GET /health -> { "status": "ok", "modelLoaded": true, "modelVersion": "demand-xgb-v1" }
```

---

## 10. Frontend Structure

**Routing** (`react-router-dom`), gated by a `ProtectedRoute` component checking `AuthContext.role`:

```
/                              public landing / product catalogue
/login, /register              public
/products, /products/:id       public

# Customer
/cart                          CUSTOMER
/checkout                      CUSTOMER
/orders, /orders/:id           CUSTOMER
/subscriptions                 CUSTOMER (list + create)
/subscriptions/:id             CUSTOMER
/account                       CUSTOMER (profile, addresses)

# Staff/Admin — under /admin
/admin                         STAFF, ADMIN   dashboard (StatCards + charts from /api/analytics/dashboard)
/admin/animals                 STAFF, ADMIN
/admin/animals/:id             STAFF, ADMIN   (detail + production history + record-entry form)
/admin/products                STAFF, ADMIN
/admin/inventory               STAFF, ADMIN   (batches table + alerts panel)
/admin/orders                  STAFF, ADMIN
/admin/subscriptions           STAFF, ADMIN
/admin/deliveries              STAFF, ADMIN   (today's delivery summary + per-delivery status update)
/admin/anomalies               STAFF, ADMIN
/admin/forecast                STAFF, ADMIN   (product picker + chart of predicted vs actual)
/admin/users                   ADMIN only     (create STAFF/ADMIN accounts)
```

**State/data**: `@tanstack/react-query` for all server state (no Redux needed at this scope); `AuthContext` holds `{ user, accessToken, login, logout }` and persists the refresh token only (access token stays in memory, not localStorage, to reduce XSS exposure — refresh token in an httpOnly cookie is stronger still but requires backend cookie support; for a portfolio project storing the refresh token in localStorage is an acceptable documented tradeoff, note it in the README).

**Axios instance**: response interceptor catches `401`, attempts one silent `POST /api/auth/refresh`, retries the original request once, and force-logs-out on a second failure.

---

## 11. Business Logic Rules

### 11.1 FEFO Inventory Allocation
On `POST /api/orders`, for each cart item: query `inventory_batches` for that `productId` with `status='ACTIVE' AND quantity_available > 0`, ordered by `expiry_date ASC NULLS LAST`. Deduct from the earliest-expiring batch(es) first, splitting across batches if one doesn't cover the full quantity, creating one `order_items` row per batch consumed. If total available across all batches < requested quantity, reject the whole order with `409 CONFLICT` and code `INSUFFICIENT_STOCK` (no partial orders in v1). When a batch's `quantity_available` hits 0, set its `status='SOLD_OUT'`.

**Low-stock / expiring-soon alerts** (`GET /api/inventory/alerts`) are computed on read, not stored: expiring-soon = `expiry_date <= today + 3 days AND status='ACTIVE'`; low-stock = sum of `quantity_available` across active batches for a product `< 10` (make this threshold configurable per product later; hardcode 10 for v1).

### 11.2 Order Status Transitions
Enforce forward-only in the service layer: `PENDING → CONFIRMED → PROCESSING → PACKED → OUT_FOR_DELIVERY → DELIVERED`. `CANCELLED` is reachable from `PENDING` or `CONFIRMED` only. `REFUNDED` is reachable only from `CANCELLED` or `DELIVERED`. Reject any other transition with `409 CONFLICT` / `INVALID_STATUS_TRANSITION`.

### 11.3 Anomaly Alert Language
Per the original spec's own stated principle: the system flags **operational patterns**, never medical conditions. Alert copy must never use words like "sick," "disease," or "infection" — use "review recommended" / "consider inspection" phrasing only, exactly as in §9.2.

### 11.4 Daily Delivery Generation
A Spring `@Scheduled` job (`DailyDeliveryGeneratorJob`, cron `0 0 20 * * *` — runs at 20:00 the evening before, generating tomorrow's deliveries) iterates all `ACTIVE` subscriptions, checks `frequency`/`weekdays` against tomorrow's date, checks `subscription_skip_dates` for an exclusion, and inserts one `deliveries` row per matching subscription if one doesn't already exist for that date (idempotent — safe to re-run).

---

## 12. Notifications — Trigger List (v1)

| Event | Recipient(s) |
|---|---|
| Anomaly alert created | all STAFF, ADMIN |
| Inventory batch expiring within 3 days | all STAFF, ADMIN |
| Order status changed | order owner |
| Subscription delivery generated for tomorrow | subscription owner |
| New STAFF/ADMIN account created | the new user (welcome) |

In-app only for v1 (a `notifications` table + `GET/PATCH` endpoints already specified in §8). Email is a Phase 2 item (§17).

---

## 13. Testing Strategy

**Backend** (JUnit 5 + Mockito for unit tests; Testcontainers-backed `@SpringBootTest` for integration tests) — minimum required coverage:
- Auth: register, login, refresh rotation, expired/invalid token rejection, role enforcement on a protected endpoint.
- Animal + production: create animal, record production, duplicate (same animal/date/session) rejected by the unique constraint.
- **Anomaly detection**: unit test `AnomalyDetectionService` directly with a fixed 14-day series and assert the exact z-score and severity for a known drop — this is the one piece of "AI" logic that must be deterministic and tested precisely.
- Inventory: FEFO allocation across two batches with different expiry dates; insufficient-stock rejection.
- Orders: full cart → order → status transition happy path; invalid transition rejected.
- Subscriptions: pause/resume/skip; `DailyDeliveryGeneratorJob` produces exactly one delivery per eligible subscription and is idempotent on re-run.

**ai-service** (`pytest` + `httpx`): schema validation on `/predict/demand` (reject malformed body with 422), `/health` returns `modelLoaded: true` after startup, missing/invalid `X-Internal-Api-Key` returns 401.

**Frontend**: not blocking for v1 portfolio scope — a handful of component tests (Vitest + React Testing Library) on the cart and checkout flow is a reasonable minimum if time allows.

---

## 14. Deployment Architecture

```
Internet
   │
   ▼
[Frontend: static hosting, e.g. Vercel/Netlify free tier]
   │  (calls VITE_API_BASE_URL)
   ▼
[Backend: containerized app hosting, e.g. Render/Railway free tier]  ← publicly reachable
   │  (internal network call, X-Internal-Api-Key header)
   ▼
[ai-service: containerized app hosting, SAME provider's internal network only]  ← NOT publicly reachable
   │
   ▼
[Postgres: managed free-tier instance]  ← reachable only by backend + ai-service, not public
```

**Critical**: ai-service must be deployed with no public ingress — most free container hosts (Render, Railway) support internal-only service-to-service networking; use that, don't just rely on the API key. If the chosen provider can't do internal-only networking on the free tier, fall back to the API-key check as the sole gate and document that limitation explicitly in the README rather than silently shipping a public unauthenticated-by-default ML endpoint.

Exact provider names aren't pinned here (per the original spec's own reasoning — free-tier terms change); pick at deploy time. Whatever is chosen, it must support: Docker image deployment, environment variables, and (for backend/ai-service) an internal-only networking mode or equivalent.

Free-tier cold starts (services sleeping after inactivity) are acceptable for a portfolio project — note it in the README so a visitor isn't confused by a slow first load.

---

## 15. CI/CD — `.github/workflows/ci.yml`

Trigger: `push` and `pull_request` on `main`. Jobs:
1. **backend**: checkout → set up JDK 17 → `mvn -B verify` (runs unit + Testcontainers integration tests) → build Docker image (no push on PRs; push to registry + trigger deploy hook only on `main` push).
2. **ai-service**: checkout → set up Python 3.11 → `pip install -r requirements.txt` → `pytest` → build Docker image (same push-on-main-only rule).
3. **frontend**: checkout → set up Node 20 → `npm ci` → `npm run build` → (optional) `npm run test`.

Keep the three jobs independent (`jobs:` top-level, not sequential) so a frontend-only PR doesn't wait on backend tests unnecessarily, but all three must pass before merge (branch protection rule).

---

## 16. Build Order — Implement Phases Sequentially

Each phase has a **definition of done**. Do not proceed to the next phase until it's met.

**Phase 1 — Scaffolding**
Create the repo structure from §2, empty Spring Boot app that starts and responds `200` on `/actuator/health`, empty FastAPI app that responds on `/health`, empty Vite+React+TS app that renders. `docker-compose up` brings up all four services (postgres, backend, ai-service, and frontend via `npm run dev` — frontend doesn't need to be in compose, it's fine to run it outside Docker locally).
*Done when*: all four run simultaneously without errors.

**Phase 2 — Schema & Auth**
Flyway `V1__init.sql` + `V2__seed_reference_data.sql` (§5). Full auth module (§6, §8 Auth section) including refresh rotation and the admin-seed `CommandLineRunner`.
*Done when*: register/login/refresh/me all work via curl or Swagger UI, and role-based `@PreAuthorize` blocks a CUSTOMER from an admin-only stub endpoint.

**Phase 3 — Animals & Production**
Full Animal + MilkProductionRecord CRUD per §8, plus `AnomalyDetectionService` (§9.2, §11.3) wired to trigger on record creation.
*Done when*: the anomaly unit test from §13 passes with an exact expected z-score.

**Phase 4 — Products & Inventory**
Categories, Products, InventoryBatch CRUD; FEFO allocation logic (§11.1) built now even though Orders don't exist yet — write it as a reusable service method, unit-tested directly.
*Done when*: FEFO unit test (two batches, different expiries) passes.

**Phase 5 — Cart, Orders, Reviews**
Full §8 Cart/Orders/Reviews contract, status-transition enforcement (§11.2), FEFO wired into order creation.
*Done when*: end-to-end integration test — register customer, browse products, add to cart, place order, verify inventory decremented correctly and order in `PENDING`.

**Phase 6 — Subscriptions & Deliveries**
Subscription CRUD + pause/resume/skip; `DailyDeliveryGeneratorJob`; Delivery status endpoints and daily summary.
*Done when*: scheduler integration test confirms idempotent, correct delivery generation for a DAILY and a CUSTOM-weekday subscription.

**Phase 7 — AI Service Integration**
Build `training/generate_seed_data.py` + `train_demand_model.py`, stand up `/predict/demand` in ai-service, wire `AiServiceClient` in backend, implement `GET /api/ai/demand-forecast` and `GET /api/ai/anomalies`.
*Done when*: a forecast request round-trips backend → ai-service → backend and is logged to `forecast_logs`.

**Phase 8 — Analytics & Notifications**
`GET /api/analytics/dashboard`; notification triggers from §12 wired into the relevant services; notification endpoints.
*Done when*: dashboard returns correct aggregates against seeded data.

**Phase 9 — Frontend**
Build pages per §10 route list, in this order: auth pages → public catalogue → customer cart/checkout/orders/subscriptions → admin dashboard → admin animals/inventory/orders/deliveries → admin anomalies/forecast charts.
*Done when*: a full manual walkthrough (register → order → admin marks delivered; record production → anomaly appears in admin panel) works end-to-end in the browser.

**Phase 10 — Tests, Docker, CI/CD, Deploy**
Fill out remaining tests from §13, finalize both Dockerfiles, add `.github/workflows/ci.yml` (§15), deploy per §14.
*Done when*: CI is green on `main` and the deployed frontend URL is fully functional against the deployed backend/ai-service.

---

## 17. Roadmap Beyond v1 (do not build now, but design doesn't preclude them)

Vaccination/health records, feed management, QR-code batch traceability, real payment gateway integration, international B2B inquiry + quotation workflow, product recommendations, subscription-churn prediction, natural-language business-intelligence assistant (restricted to predefined read-only analytics functions, never raw DB access — see the original doc's own §7.7 reasoning, which is sound and should be kept for whenever this is built), scheduled automatic model retraining, mobile app, IoT sensor integration.

---

## 18. Resume-Ready Project Description

**AgroDairy AI — AI-Powered Dairy & Agriculture Operations Platform**
*Java, Spring Boot, Spring Security, PostgreSQL, React, TypeScript, Python, FastAPI, XGBoost, Docker, GitHub Actions*

Built and deployed a full-stack platform managing dairy farm operations end-to-end — individual animal and milk-production tracking, batch-level FEFO inventory, e-commerce ordering, and recurring milk subscriptions with automated daily delivery generation. Implemented JWT-based role authentication, a statistical anomaly-detection service flagging abnormal per-animal production drops, and an XGBoost demand-forecasting microservice integrated via a secured internal API, backed by automated tests and CI/CD.
