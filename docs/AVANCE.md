# Avance del proyecto

## Etapa 0 — Esqueleto del proyecto (08-11-2026)

**Hecho**
- Repositorio git en `C:\ActivoFijo`; documentación ordenada en `docs/` (plan, referencia,
  prototipo del cliente en `docs/cliente/`).
- `docker-compose.yml`: PostgreSQL 17 (BD/usuario/clave `activofijo`, volumen `pgdata`, healthcheck).
- **Backend** (`backend/`, Spring Boot 4.1.2, Java 21, paquete `cl.activofijo`):
  - Perfiles `dev` (por defecto, BD local), `test` (Testcontainers) y `prod` (todo por variables
    de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`).
  - Flyway con `V1__inicial.sql` (tabla `parametro_sistema`); Hibernate en `validate`.
  - `GET /api/v1/salud`: aplicación, versión (de `build-info`), estado de la BD y fecha.
    Responde 200 si la BD está OK y **503 con cuerpo explicativo** si no responde.
  - Manejador global de errores `ManejadorErrores` → `ProblemDetail` con título/detalle en español;
    excepciones base `RecursoNoEncontradoException` (404) y `ReglaNegocioException` (422).
  - Utilidad `Rut` (calcular DV, normalizar, validar, formatear) y anotación `@RutValido`.
  - springdoc-openapi: Swagger UI solo en `dev`.
- **Frontend** (`frontend/`, Angular 22 + Material, zoneless, Vitest):
  - Locale `es-CL`, proxy `/api` → `localhost:8080`, tema con colores Kromas.
  - Layout: barra superior con marca provisoria + menú lateral responsive con los módulos del
    prototipo del cliente (deshabilitados hasta su etapa).
  - Página Inicio con el estado del sistema y mensajes claros si cae la BD o el backend.
- Plan: se agregó la **Etapa 5B — Depreciación** (con preguntas abiertas).

**Decisiones**
- Spring Boot 4.1.2 (última estable) en vez de 3.x.
- PostgreSQL 17 en desarrollo y tests (misma versión en ambos).
- Spring Security, Spring Session, Envers, POI, OpenHTMLtoPDF, ZXing y MapStruct **no** se
  agregaron todavía: se incorporan en la etapa que los usa (evita que Security bloquee `/salud`
  antes de tener login).
- Tests de integración con sufijo `IT` (failsafe, fase `verify`).
- Logo: marca provisoria en CSS hasta recibir el archivo del logo del cliente.

**Tests**
- Backend: `RutTest` (DV, normalización, inválidos, formato, validador), `SaludControllerTest`
  (BD caída → 503) y `SaludControllerIT` (Testcontainers: `/salud` OK y 404 como ProblemDetail).
- Frontend: `app.spec.ts`, `inicio.spec.ts` (OK, BD caída, backend apagado).

**Pendientes**
- Logo del cliente en SVG/PNG.
- Definiciones de depreciación (ver Etapa 5B).

**Cómo probar**
1. `docker compose up -d`
2. `cd backend; .\mvnw.cmd verify` → todo en verde.
3. `.\mvnw.cmd spring-boot:run` y en otra terminal `cd frontend; npm start`.
4. Abrir http://localhost:4200 → "Base de datos: OK".
5. `docker compose stop` → "Volver a consultar" muestra "Base de datos: sin conexión".
6. Detener el backend → muestra "No se pudo contactar al servidor".
