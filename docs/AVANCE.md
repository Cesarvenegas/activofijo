# Avance del proyecto

## Etapa 1 — Seguridad y usuarios (08-11-2026)

**Alcance acordado:** login, roles y administración de usuarios **sin multiempresa** (se verá
después; ver nota en CLAUDE.md §4). **El login es con RUN** (no hay username).

**Backend**
- `V2__usuarios_y_auditoria.sql`: tablas `usuario`, `evento_auditoria` y las de Spring Session JDBC.
- `comun.EntidadBase` (id identity + creado/modificado en/por con Spring Data Auditing, el "por"
  es el RUN del usuario en sesión) y `comun.PaginaDto` (`{content, page, size, totalElements, totalPages}`).
- `comun.error.ErrorApi`: errores con `codigo` estable para el frontend (`CUENTA_BLOQUEADA`,
  `DEBE_CAMBIAR_PASSWORD`, `RUT_DUPLICADO`...).
- Seguridad (`config.SeguridadConfig`): sesión en cookie `SESSION` HttpOnly/SameSite=Lax guardada
  en PostgreSQL (expira a las 2 h de inactividad), CSRF con cookie `XSRF-TOKEN` (`csrf.spa()`),
  401/403 como ProblemDetail en español. Nuevo id de sesión y nuevo token CSRF al hacer login.
- `auth/`: `GET /auth/csrf`, `POST /auth/login`, `GET /auth/yo`, `POST /auth/cambiar-password`,
  `POST /auth/logout`. Bloqueo 15 min tras 5 intentos fallidos; un RUN inexistente responde igual
  que una clave mala (y tarda lo mismo). `FiltroCambioPassword`: con clave temporal solo se
  permiten los endpoints de `/auth`.
- `usuario/`: CRUD en `/api/v1/usuarios` (solo ADMIN) con filtros texto/rol/activo, orden por lista
  blanca, creación y reseteo con **clave temporal mostrada una sola vez**. Desactivar, cambiar rol
  o resetear clave **cierra las sesiones abiertas** de ese usuario (Spring Session indexa por RUN).
  Un ADMIN no puede desactivarse ni quitarse el rol de administrador.
- `auditoria/`: eventos LOGIN_OK, LOGIN_FALLIDO, LOGIN_BLOQUEADO, LOGOUT, CAMBIO_PASSWORD,
  RESET_PASSWORD, USUARIO_CREADO/MODIFICADO/ACTIVADO/DESACTIVADO, con IP.
- Usuarios iniciales: en `dev` se crean 3 usuarios demo; en cualquier perfil, si la tabla está
  vacía y existen `APP_ADMIN_RUT` y `APP_ADMIN_PASSWORD`, se crea el primer ADMIN (debe cambiar la clave).

**Frontend**
- Login con RUN (se formatea con puntos al salir del campo), marca Kromas, responsive.
- `AuthService` (usuario en signal), inicialización de la app (CSRF + sesión vigente), guards
  `autenticado` / `invitado` / `rol`, interceptor de errores (401 → login, 403 por clave temporal
  → Mi cuenta, otros 403 → aviso).
- Barra superior con menú de usuario (Mi cuenta, Cerrar sesión); menú lateral según el rol.
- **Mi cuenta**: datos y cambio de contraseña (obligatorio si la clave es temporal).
- **Usuarios** (ADMIN): listado con buscador, filtros por rol/estado, estado (activo, bloqueado,
  clave temporal, inactivo), crear/editar en diálogo, resetear clave y activar/desactivar con confirmación.
- Compartidos: `tabla-filtrable` (paginación/orden en servidor, filtros y acciones proyectados,
  celdas personalizadas con `<ng-template appCelda>`), diálogo `confirmar()`, utilidades de RUT y
  paginador en español.

**Decisiones**
- Login propio (controlador) en vez de `formLogin`: permite respuestas JSON y la lógica de bloqueo.
  Los intentos fallidos se guardan aunque el login falle (`@Transactional(noRollbackFor)`).
- El RUN no se puede editar (es el login). Si se ingresó mal, desactivar y crear otro.
- MapStruct aún no se usa: los DTO de usuario se mapean con métodos `de(...)`; se agregará cuando
  haya entidades grandes (Etapa 3).
- `CookiesIT` prueba el flujo del navegador sobre Tomcat real en un contexto aparte: MockMvc con
  spring-security-test reemplaza el repositorio CSRF del filtro compartido y no sirve para cookies.

**Tests**
- Backend (`mvnw verify`): 135 unitarios + 23 de integración (AuthIT, UsuarioIT, CookiesIT,
  SaludControllerIT) contra PostgreSQL real.
- Frontend (`npm test`): 24 tests (RUT, guards, interceptor, login, tabla-filtrable, inicio).

**Pendientes**
- Multiempresa (empresa/institución, `empresa_id`, SUPERADMIN): decidir antes de la Etapa 2.
- Perfiles de ubicación y de tipo de bien: se asignan en la Etapa 2 (pestaña del formulario de usuario).

**Cómo probar** (`docker compose up -d`, backend y `npm start`)
1. Ir a http://localhost:4200 → redirige a /login.
2. Ingresar con `11.111.111-1` / `Kromas2026` (ADMIN). También `22.222.222-2` (OPERADOR) y
   `33.333.333-3` (CONSULTA), misma clave.
3. Usuarios → Nuevo usuario → anotar la clave temporal. Cerrar sesión e ingresar con ese RUN:
   obliga a cambiar la clave en Mi cuenta.
4. Con OPERADOR o CONSULTA: no aparece Administración; `/usuarios` redirige al inicio y la API responde 403.
5. 5 claves malas seguidas → "Cuenta bloqueada temporalmente". Como ADMIN, el usuario aparece
   "Bloqueado"; Resetear contraseña lo desbloquea.
6. RUN inválido (ej. 11.111.111-2) → "RUN inválido" sin llamar al servidor.
7. Desactivar a un usuario con sesión abierta → su siguiente acción lo envía al login.

---

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
