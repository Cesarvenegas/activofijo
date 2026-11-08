# CLAUDE.md — Kromas Inventa Gov (Sistema de Activo Fijo)

> Este archivo le da contexto a Claude Code en cada sesión. Léelo completo antes de trabajar.
> El plan de trabajo está en `docs/PLAN_ETAPAS.md`. El avance se registra en `docs/AVANCE.md`.

## 1. Qué es este proyecto

Aplicación **web multiempresa** para administrar el inventario de activo fijo (equipos, bienes,
partes, software y licencias) de pymes. Un distribuidor la vende a varias empresas; todas usan
la misma instalación y **cada empresa solo ve sus propios datos**.

La funcionalidad se inspira en un sistema de escritorio existente. Sus pantallas están en
`docs/referencia/*.png` **solo como guía funcional**. Reglas:

- NO copiar nombre, logo, textos, diseño visual ni código del sistema de referencia.
- El diseño debe ser una aplicación web moderna y limpia (Angular Material), no una imitación.
- Se pueden simplificar pantallas (por ejemplo, unificar los tipos de movimiento en un formulario).
- **No hay migración de datos.** La carga inicial se hace con importación desde Excel.

**Cliente y marca:** el producto es **KROMAS — INVENTA GOV**, "Gestión inteligente de activos
públicos" (lema: *Control · Inventario · Trazabilidad*). Colores: azul marino `#173a5e` y naranjo
`#ee7d2b`. Orientado a **instituciones públicas**. El prototipo del cliente (Excel) está en
`docs/cliente/` y **manda sobre las capturas de referencia** cuando difieren:
- Menú principal: Ingreso, Alta, Baja, Traspaso, Mantención, Consultas, Informes.
- Ingreso: tipo de ingreso (Adquisición, Arriendo, Comodato, Donación), N° resolución, cuenta
  contable, N° orden de compra / factura / guía de despacho, cada uno con su fecha; ítems con
  descripción, marca, modelo, cantidad, valor unitario con IVA y foto.
- **La depreciación es parte central del producto** (vidas útiles según la tabla del SII).

## 2. Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 (LTS), Spring Boot (última versión estable al iniciar), Maven |
| Persistencia | PostgreSQL 16+, Spring Data JPA / Hibernate 6, Flyway |
| Seguridad | Spring Security con sesión (cookie `HttpOnly`) + Spring Session JDBC + CSRF con `CookieCsrfTokenRepository` |
| Auditoría / historial | Hibernate Envers + tabla propia de eventos (login, exportaciones) |
| PDF | Thymeleaf (plantillas HTML) + OpenHTMLtoPDF |
| Excel | Apache POI (usar `SXSSFWorkbook` para exportaciones grandes) |
| Códigos de barra / QR | ZXing (generación en backend) |
| Mapeo DTO | MapStruct |
| API docs | springdoc-openapi (Swagger UI solo en perfil `dev`) |
| Frontend | Angular (última estable), standalone components, signals, Angular Material + CDK |
| Gráficos | ng2-charts (Chart.js) |
| Escaneo en celular | `@zxing/browser` (cámara del navegador) |
| Tests backend | JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), MockMvc |
| Tests frontend | El runner que trae por defecto Angular CLI |
| Entorno local | Docker Compose (PostgreSQL) |
| Producción | Linux + Nginx/Apache como proxy inverso + backend como servicio systemd o Docker |

Cuando fijes versiones en la Etapa 0, anótalas en la sección 9 de este archivo.

## 3. Estructura del repositorio

```
/
├── CLAUDE.md
├── docker-compose.yml          # PostgreSQL para desarrollo
├── docs/
│   ├── PLAN_ETAPAS.md
│   ├── AVANCE.md               # bitácora: qué se hizo en cada etapa y decisiones
│   └── referencia/             # capturas del sistema de referencia (solo guía)
├── backend/
│   └── src/main/java/cl/activofijo/
│       ├── config/             # seguridad, multiempresa, web, jackson
│       ├── comun/              # entidades base, errores, paginación, numeradores, utilidades (RUT)
│       ├── empresa/
│       ├── usuario/
│       ├── catalogo/           # tablas maestras genéricas
│       ├── localidad/          # establecimiento → servicio → unidad → localización
│       ├── proveedor/
│       ├── bien/
│       ├── parte/
│       ├── software/
│       ├── recepcion/
│       ├── alta/
│       ├── movimiento/
│       ├── contrato/
│       ├── etiqueta/
│       ├── informe/
│       ├── inventario/         # toma de inventario físico
│       ├── dashboard/
│       └── auditoria/
└── frontend/
    └── src/app/
        ├── core/               # auth, interceptores, guards, layout, servicios base
        ├── shared/             # componentes reutilizables (tabla-filtrable, selector-localidad, confirmación...)
        └── features/           # una carpeta por módulo (bienes, movimientos, ...)
```

Organización **por funcionalidad** (paquete por módulo), no por capa técnica. Dentro de cada
módulo backend: `Entidad`, `Repository`, `Service`, `Controller`, `dto/`, `mapper/`.

## 4. Multiempresa (regla crítica)

- Toda entidad de negocio extiende `EntidadEmpresa`, que tiene `empresa_id` marcado con
  `@TenantId` de Hibernate. El `CurrentTenantIdentifierResolver` toma la empresa del usuario en sesión.
- **Nunca** escribir consultas nativas (`nativeQuery = true`) sin filtrar `empresa_id`
  explícitamente. Preferir JPQL/Criteria, que Hibernate filtra solo.
- Toda tabla de negocio lleva índice que comienza con `empresa_id`. Las restricciones `UNIQUE`
  de negocio incluyen `empresa_id` (ej. `UNIQUE(empresa_id, codigo)`).
- Rol `SUPERADMIN` (del distribuidor): administra empresas y puede "entrar como" una empresa
  para dar soporte. Esa acción queda auditada.
- Cada módulo nuevo debe incluir **un test de aislamiento**: datos creados en empresa A no son
  visibles ni modificables desde empresa B (listar, obtener por id, editar, eliminar → 404).

## 5. Convenciones

**Idioma:** dominio y UI en español (`Bien`, `Movimiento`, `fechaSolicitud`). Sin tildes ni ñ en
identificadores de código (`localizacion`, `anio`). Comentarios en español.

**Base de datos:** `snake_case`; PK `id bigint` generada (identity); columnas de auditoría
`creado_en`, `creado_por`, `modificado_en`, `modificado_por`; borrado lógico con `activo boolean`
en catálogos y datos maestros (no borrar físicamente si hay referencias). Cada cambio de esquema
es una migración Flyway nueva (`V{n}__descripcion.sql`). **Nunca editar una migración ya aplicada.**

**API REST:**
- Prefijo `/api/v1`. Recursos en plural: `/api/v1/bienes`, `/api/v1/movimientos/{id}`.
- Listados paginados en servidor: `?page=0&size=50&sort=codigo,asc` + filtros como query params.
  Respuesta: `{ content, page, size, totalElements, totalPages }`.
- Errores con `ProblemDetail` (RFC 9457) y mensajes en español. Validaciones con Bean Validation.
- Nunca exponer entidades JPA: siempre DTOs (`BienDto`, `BienCrearRequest`, `BienFiltro`).
- Exportaciones: `GET /api/v1/bienes/exportar?formato=xlsx|pdf&<mismos filtros>`.

**Formato chileno:**
- RUT con validación módulo 11, guardado sin puntos y con guion (`12345678-9`), mostrado con puntos.
- Fechas `dd-MM-yyyy` en UI; zona horaria `America/Santiago`; en BD `date` / `timestamptz`.
- Montos en CLP como `BigDecimal` (escala 0) / `numeric(15,0)`; mostrar `$1.234.567`.
- Locale Angular `es-CL`.

**Numeradores (folios):** tabla `numerador(empresa_id, clave, anio, ultimo)` con bloqueo
pesimista (`SELECT ... FOR UPDATE`). Formatos:
- Código de bien: `{PREFIJO_TIPO}{correlativo 5 dígitos}` → `AIR04228` (prefijo definido en el tipo de equipo).
- Folios de alta y movimiento: `{AA}-{correlativo}` por año → `26-47`.

**Frontend:**
- Componentes standalone, `inject()`, signals para estado local; servicios por feature; sin NgRx.
- Componente compartido `tabla-filtrable` (mat-table + paginación/orden en servidor + panel de filtros
  + botones exportar) reutilizado en todos los listados.
- Formularios reactivos tipados. Diálogos de confirmación antes de eliminar.
- Diseño responsive; las pantallas de inventario físico deben funcionar en celular.

## 6. Roles

| Rol | Puede |
|---|---|
| `SUPERADMIN` | Todo, en todas las empresas; crear empresas y su primer administrador |
| `ADMIN` | Todo dentro de su empresa, incluidos usuarios y tablas maestras |
| `OPERADOR` | Registrar y editar bienes, recepciones, altas, movimientos, inventario físico |
| `CONSULTA` | Solo ver, buscar e imprimir/exportar |

Además, cada usuario puede tener **perfil de ubicación** (lista de establecimientos/servicios
visibles) y **perfil de tipo de bien** (tipos visibles). Sin perfil = ve todo lo de su empresa.
Estos perfiles se aplican en el servicio a todas las consultas de bienes, partes y movimientos.

## 7. Comandos

```bash
docker compose up -d                       # PostgreSQL local
cd backend && ./mvnw spring-boot:run       # API en http://localhost:8080 (perfil dev por defecto)
cd backend && ./mvnw verify                # tests unitarios + *IT con Testcontainers (requiere Docker)
cd frontend && npm start                   # UI en http://localhost:4200 (proxy /api → 8080)
cd frontend && npm test -- --watch=false   # Vitest
cd frontend && npm run build
```

En Windows (PowerShell) usar `.\mvnw.cmd` en vez de `./mvnw`. Swagger UI (solo perfil dev):
`http://localhost:8080/swagger-ui.html`.

## 8. Forma de trabajo

1. Trabajar **una etapa a la vez**, según `docs/PLAN_ETAPAS.md`. No adelantar funcionalidades de etapas futuras.
2. Al iniciar una etapa: leer la sección de la etapa, revisar el código existente y **proponer un plan
   antes de escribir código**. Esperar aprobación.
3. Al terminar: `./mvnw verify` y `npm test` deben pasar, y `npm run build` sin errores.
4. Actualizar `docs/AVANCE.md` (qué se hizo, decisiones tomadas, pendientes, cómo probar).
5. Hacer commit con mensaje `etapa N: <resumen>`. No hacer push salvo que se pida.
6. Si algo del plan no tiene sentido o falta información, **preguntar** en vez de suponer.
7. No agregar dependencias fuera del stack sin justificarlo.

## 9. Versiones fijadas

Fijadas en la Etapa 0 (08-11-2026):

- Java: 21 (Temurin 21.0.13)
- Spring Boot: 4.1.2 (Spring Framework 7, Hibernate 7, Jakarta EE 11)
- springdoc-openapi: 3.2.0
- Angular / Angular Material / CDK: 22.2.6 (zoneless; tests con Vitest)
- Node: 22.23 (Angular 22 exige ^22.22.3 || ^24.15 || >=26)
- PostgreSQL: 17 (`postgres:17-alpine`, igual en docker-compose y Testcontainers)

Notas de Spring Boot 4: los starters están modularizados (`spring-boot-starter-webmvc`,
`spring-boot-starter-flyway`, `*-test` por módulo); `@AutoConfigureMockMvc` está en
`org.springframework.boot.webmvc.test.autoconfigure`. Tests de integración: sufijo `IT`, los
ejecuta `maven-failsafe-plugin` en `verify`.
