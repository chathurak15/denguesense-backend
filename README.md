# DengueSense LK Backend

Spring Boot API for **DengueSense LK**, a dengue surveillance platform for Sri Lanka. Citizens submit breeding-site reports; Public Health Inspectors (PHI), MOH officers, epidemiologists, and admins review them, detect spatial clusters, receive alerts, and run weekly district forecasts.

## Stack

- Java 21, Spring Boot 4.1
- PostgreSQL + PostGIS (Hibernate Spatial)
- Flyway migrations
- JWT auth (optional Google OAuth)
- Cloudinary (report images)
- Firebase Cloud Messaging (citizen push)
- Telegram Bot API (PHI outbreak alerts)
- External FastAPI AI service (CNN report classification + LSTM forecasts)
- Open-Meteo archive weather for weekly LSTM features

## Roles

| Role | Typical access |
|---|---|
| `ADMIN` | Users, case import, cluster detect, forecast regenerate |
| `EPIDEMIOLOGIST` | Dashboards, cases, forecasts, reports |
| `MOH` | District oversight, resolutions, cases |
| `PHI` | District reports, resolutions, cluster alerts via Telegram |
| `VOLUNTEER` | Profile / limited staff access |

Citizen report submit and public outbreak endpoints do not require a staff JWT.

## API (v1)

Base URL defaults to `http://localhost:8080`.

| Area | Prefix | Notes |
|---|---|---|
| Auth | `/api/v1/auth` | Register, login, logout |
| Reports | `/api/v1/reports` | Citizen submit (`/save`), staff listing, PHI district views |
| Resolutions | `/api/v1/resolutions` | Mark / read report resolution |
| Users | `/api/v1/user` | Staff directory, profile, Telegram alert status |
| Public citizen | `/api/v1/public` | Outbreak summary, hotspots, alerts, districts, forecasts, district status |
| Weekly cases | `/api/v1/cases` | District staff case listing and summary |
| Admin cases | `/api/v1/admin/cases` | Manual submit and CSV import |
| Forecasts | `/api/v1/forecasts/{rdhsId}/latest` | Latest district forecast |
| Admin forecasts | `/api/v1/admin/forecasts/{rdhsId}/regenerate` | Force regenerate |
| Clusters | `/api/v1/admin/clusters/detect` | Trigger cluster detection |
| Telegram | `/telegram/webhook` | Bot webhook (public) |

Cluster detection uses HIGH_RISK reports within a configurable radius, minimum size, and time window (defaults: 500 m, 5 reports, 24 hours). The weekly forecast job runs Mondays at 02:00 Asia/Colombo unless disabled.

## Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL 14+ with PostGIS
- Running AI service (CNN + LSTM) if you need classification or forecasts

## Local setup

1. Create a database, e.g. `denguesense`, and enable PostGIS.

2. Add `src/main/resources/application-local.properties` (gitignored). Minimum:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/denguesense
spring.datasource.username=postgres
spring.datasource.password=your-password
denguesense.frontend.base.url=http://localhost:3000
ai-service.base-url=http://localhost:5000
```

Optional local keys (mail, Cloudinary, Telegram, Firebase, Google OAuth) follow the same property names as `application-prod.properties`.

3. Run:

```bash
mvn spring-boot:run
```

The `local` profile is active by default (`spring.profiles.active=local`). API listens on port **8080**.

```bash
mvn test
```

## Production

Activate the `prod` profile and supply environment variables. Common ones:

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `FRONTEND_URL` | CORS / frontend origin |
| `AI_SERVICE_URL` | FastAPI classifier + forecast service |
| `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME` | PHI Telegram alerts |
| `FIREBASE_CREDENTIALS_FILE` | FCM service-account path |
| `FORECAST_SCHEDULER_CRON` | Set to `-` to disable the Monday job |
| `SES_SMTP`, `SES_USERNAME`, `SES_PASSWORD`, `FROM_EMAIL` | Transactional mail |
| `AUTH2_CLIENT_ID`, `AUTH2_SECRET` | Google OAuth |

Flyway runs from `classpath:db/migration` (`baseline-on-migrate=true`, baseline version `0`).

## Repository

- Backend: [denguesense-backend](https://github.com/chathurak15/denguesense-backend)
- BSc Design Project — DengueSense LK
