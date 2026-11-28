# Pacific Aqua — Admin Control

Admin web app (Vue 3) + API (Spring Boot 3) + MySQL 8 for Pacific Aqua sales ledger.

## Stack

- **admin-web/** — Vue 3 + Vite + Pinia + Vue Router
- **server/** — Spring Boot 3, JWT auth, Flyway, WebSocket (STOMP)
- **docker-compose.yml** — MySQL 8

REST paths have **no `/api` prefix** (e.g. `POST /auth/login`, `GET /visits`).

## Prerequisites

- Java 17+ (`JAVA_HOME` set)
- Maven 3.8+
- Node 20+
- Docker Desktop (for MySQL)

## Run

```bash
# 1) Database
docker compose up -d

# 2) API
cd server
export JAVA_HOME=/opt/homebrew/Cellar/openjdk/19.0.1/libexec/openjdk.jdk/Contents/Home   # adjust if needed
mvn spring-boot:run

# 3) Admin UI
cd ../admin-web
npm install
npm run dev
```

- Admin UI: http://127.0.0.1:5173  
- API: http://127.0.0.1:8080  

### Seed login

| Field | Value |
| --- | --- |
| Phone | `9999999999` |
| Password | `admin123` |

Helpers: Tikesh / Himanshu / Deepak — phone `9000000001`…`3`, password `helper123`.

## Modules (live)

Login · SUM UP · P&S · BILL · Orders · Customers · Plant trips · Products · Employees

Orders notify helpers via **WebSocket** (`/topic/helpers/{id}/orders`) with **HTTP poll** fallback (`GET /orders/mine`).

## Import Excel data

With MySQL running and products seeded (start API once):

```bash
python3 scripts/import_excel.py
```

Loads customers from the **Customers** sheet, helpers from **ATT.**, and visits from the **BILL** sheet.

## Mockups

Static HTML mockups remain at repo root: `backend.html`, `mobile.html`, `styles.css`.
