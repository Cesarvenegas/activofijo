# Kromas Inventa Gov

Gestión inteligente de activos públicos — *Control · Inventario · Trazabilidad*.

Aplicación web multiempresa de activo fijo: Spring Boot 4 + Angular 22 + PostgreSQL 17.
Contexto para desarrollo en [`CLAUDE.md`](CLAUDE.md); plan en [`docs/PLAN_ETAPAS.md`](docs/PLAN_ETAPAS.md);
avance en [`docs/AVANCE.md`](docs/AVANCE.md).

## Requisitos

- Java 21
- Node 22.22+ (o 24.15+)
- Docker Desktop (PostgreSQL local y tests de integración)

## Desarrollo

```powershell
docker compose up -d                 # PostgreSQL en localhost:5432

cd backend
.\mvnw.cmd spring-boot:run           # API en http://localhost:8080
.\mvnw.cmd verify                    # tests (unitarios + integración con Testcontainers)

cd frontend
npm install                          # solo la primera vez
npm start                            # UI en http://localhost:4200
npm test -- --watch=false
npm run build
```

En Linux/macOS usar `./mvnw` en lugar de `.\mvnw.cmd`.

- Estado del sistema: `GET http://localhost:8080/api/v1/salud`
- Swagger UI (solo perfil `dev`): http://localhost:8080/swagger-ui.html
