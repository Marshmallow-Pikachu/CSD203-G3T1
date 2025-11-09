## Quick context for AI coding agents

This repository is a two-part web app: a Spring Boot backend (Java 21, Maven) and a Vite + React + TypeScript frontend.

- Backend: `backend/` (base package `com.ratewise`). Key folders:
  - Controllers: `backend/src/main/java/com/ratewise/restcontrollers` (e.g. `CalculatorController.java` -> POST `/api/v1/calculator/landed-cost`)
  - Services: `backend/src/main/java/com/ratewise/services`
  - DTOs / exceptions / security: `backend/src/main/java/com/ratewise/{dto,exceptions,security}`
  - Properties: `backend/src/main/resources/application_sample.properties` (env-first config using `SPRING_DATASOURCE_*` and `SECURITY_JWT_SECRET_KEY`)

- Frontend: `frontend/` (Vite + React + TypeScript)
  - API client: `frontend/src/api/client.ts` and `frontend/src/api/calculator.ts` (look here to see request shape expected by backend)
  - Start dev server: `npm run dev` (uses Vite, default port 5173)

## How to run locally (concrete steps)
- Backend (development convenience): run `run-local.ps1` from the repo root. What it does:
  - Loads/creates `MarcLocalEnvironment.env` and injects environment variables.
  - Ensures a 32-byte Base64 `SECURITY_JWT_SECRET_KEY` exists (it will persist it to the env file if missing).
  - Forces `SERVER_PORT=8080` and runs `backend/mvnw clean spring-boot:run` with that port.
  - Important env vars referenced in `application_sample.properties`: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_PROFILES_ACTIVE`.

- Frontend (dev):
  - From `frontend/`: `npm install` (if dependencies not installed), then `npm run dev` to start Vite.
  - `frontend/run.sh` kills port 5173 then runs `npm run dev` (useful on Unix/macOS shells).

## Build & packaging
- Build backend: from repo root -> `.ackend\mvnw clean package` (produces `backend/target/*.jar`). Maven wrapper is checked in (`mvnw`, `mvnw.cmd`). Java 21 is expected (pom property `<java.version>21`).
- Build frontend: from `frontend/` -> `npm run build` (runs `tsc -b && vite build`).

## API & conventions worth knowing (concrete patterns)
- API base paths use `/api/v1/*` and controllers live in `restcontrollers` (e.g. `TariffController`, `CountryController`, `AuthController`). Use those files as canonical examples of request/response shapes.
- Controllers sometimes accept and return raw maps (`Map<String,Object>`) for flexible payloads (see `CalculatorController.calculateLandedCost`). When changing controller signatures, update the frontend client (`frontend/src/api/*`) accordingly.
- Security is implemented via Spring Security + JWT. Local dev relies on `SECURITY_JWT_SECRET_KEY` in `MarcLocalEnvironment.env` (see `run-local.ps1` for generation). OAuth2 client is enabled (see `pom.xml` dependency `spring-boot-starter-oauth2-client`).
- OpenAPI UI is available (dependency `springdoc-openapi-starter-webmvc-ui` in `pom.xml`) — check `/swagger-ui/` or `/swagger-ui/index.html` when the backend is running.

## Tests & quick checks
- Backend tests: `.ackend\mvnw test` from repo root.
- Frontend lint: `cd frontend && npm run lint`.

## Useful file examples to consult
- `backend/src/main/java/com/ratewise/restcontrollers/CalculatorController.java` — shows how calculator endpoint expects a request shape and returns a Map result.
- `backend/src/main/resources/application_sample.properties` — canonical env keys and DB hints.
- `run-local.ps1` — how local dev env is bootstrapped (JWT key generation, forced port, runs mvnw).
- `frontend/src/api/client.ts` and `frontend/src/api/calculator.ts` — how frontend calls the backend; mirror changes here when adjusting API.

## Merge guidance for agents
- If editing APIs: update both controller (backend) and the frontend client (frontend/src/api). Add/adjust sample request bodies in accompanying `Test*.http` files (see `backend/http/` for examples).
- Prefer environment-driven configuration: don't hard-code credentials in code; use `application.properties` placeholders or `MarcLocalEnvironment.env` for local runs.

If any section is unclear or you'd like more examples (e.g. common DTO fields, auth flows, SQL migrations), tell me which area to expand and I'll iterate.
