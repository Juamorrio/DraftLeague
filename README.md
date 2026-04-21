# DraftLeague

> **Plataforma Fantasy Football para La Liga** — Trabajo Fin de Grado.
>
> Aplicación full-stack que permite a los usuarios crear ligas privadas, fichar jugadores reales de La Liga, gestionar plantillas, pujar en un mercado dinámico y competir por puntos basados en el rendimiento real de cada jornada, con predicciones de Machine Learning sobre rendimiento futuro.

---

## Tabla de contenidos

1. [Descripción](#descripción)
2. [Stack tecnológico](#stack-tecnológico)
3. [Arquitectura](#arquitectura)
4. [Estructura del monorepo](#estructura-del-monorepo)
5. [Quick start](#quick-start)
6. [Variables de entorno](#variables-de-entorno)
7. [Endpoints principales](#endpoints-principales)
8. [Tests y cobertura](#tests-y-cobertura)
9. [CI/CD](#cicd)
10. [Documentación adicional](#documentación-adicional)
11. [Autor](#autor)

---

## Descripción

DraftLeague es un sistema completo de Fantasy Football centrado en La Liga española. Cada usuario puede:

- **Crear o unirse a ligas privadas** mediante código de invitación.
- **Fichar jugadores reales** desde un mercado de subastas con valor de mercado dinámico.
- **Gestionar su plantilla** con formación táctica, capitán y chips especiales (Triple Captain, Bench Boost).
- **Recibir puntos automáticamente** tras cada jornada en función del rendimiento real (goles, asistencias, minutos, tarjetas, rating).
- **Consultar predicciones de Machine Learning** sobre el rendimiento esperado de jugadores y equipos en próximas jornadas.
- **Competir en clasificación** dentro de su liga jornada a jornada.

Los datos de jugadores, partidos y estadísticas se sincronizan automáticamente desde [API-Football](https://www.api-football.com/) (La Liga, temporada 2025-26).

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| **Backend** | Spring Boot 3.5.6 · Java 21 · Maven · Spring Security · JWT (JJWT 0.11.5) · Spring Data JPA |
| **Base de datos** | MariaDB 11 (producción) · H2 (tests) |
| **Frontend** | React Native 0.81.5 · Expo 54 · React Navigation · Context API |
| **ML Service** | Python · FastAPI · XGBoost |
| **Infraestructura** | Docker · Docker Compose |
| **Testing** | JUnit 5 · Mockito · Spring Security Test · Jest · JaCoCo |
| **CI/CD** | GitHub Actions · Codacy |

---

## Arquitectura

```
┌─────────────────┐      ┌──────────────────┐      ┌──────────────┐
│   Mobile App    │      │   Spring Boot    │      │   MariaDB    │
│ (React Native)  │ ───► │   REST API       │ ───► │   Database   │
│   Expo 54       │ ◄─── │   JWT Auth       │ ◄─── │              │
└─────────────────┘      └────────┬─────────┘      └──────────────┘
                                  │
                                  │ HTTP
                                  ▼
                         ┌──────────────────┐      ┌──────────────┐
                         │   ML Service     │ ◄─── │  API-Football│
                         │   (FastAPI +     │      │  (La Liga    │
                         │    XGBoost)      │      │   data)      │
                         └──────────────────┘      └──────────────┘
```

- **Backend Spring Boot**: API REST stateless con autenticación JWT. Capas estrictas (`controllers → services → repositories → models`). Implementa el ciclo completo de jornada: importación de estadísticas, cálculo de puntos, recálculo de valores de mercado, finalización de subastas.
- **Frontend Expo**: aplicación móvil universal (Android, iOS, web). Navegación con tabs personalizadas, gestión de estado mediante Context API, sistema de diseño centralizado.
- **ML Service**: microservicio Python que sirve predicciones XGBoost de puntos esperados por jugador y por equipo.

Diagrama detallado del sistema en [docs/uml-sistema.md](docs/uml-sistema.md).

---

## Estructura del monorepo

```
DraftLeague/
├── backend/                  # Spring Boot API (Java 21)
│   └── src/
│       ├── main/java/com/DraftLeague/
│       │   ├── controllers/  # REST endpoints
│       │   ├── services/     # Lógica de negocio
│       │   ├── repositories/ # Spring Data JPA
│       │   ├── models/       # Entidades JPA
│       │   ├── dto/          # DTOs de request/response
│       │   ├── scraping/     # Cliente API-Football
│       │   └── config/       # Security, JWT
│       └── test/             # Tests unitarios y de integración
├── frontend/                 # React Native / Expo
│   ├── pages/                # Pantallas (Home, Leagues, Team, Market, Admin, AI…)
│   ├── components/           # Componentes reutilizables
│   ├── services/             # Cliente HTTP autenticado
│   ├── navigation/           # React Navigation
│   ├── context/              # Context API (LeagueContext, MatchesContext)
│   ├── utils/theme.js        # Sistema de diseño
│   └── assets/               # Imágenes y recursos
├── ml-service/               # Microservicio Python ML
│   ├── main.py               # FastAPI app
│   ├── model/                # Modelo XGBoost serializado
│   └── tests/
├── docs/                     # Documentación TFG
├── .github/workflows/        # Pipelines CI/CD
├── docker-compose.yml        # Orquestación full stack
├── Dockerfile.backend
├── Dockerfile.frontend
└── pom.xml                   # Build Maven raíz
```

---

## Quick start

### Opción A — Docker Compose (recomendado)

Levanta todo el stack (MariaDB + backend + ml-service + frontend) con un solo comando.

```bash
# 1. Clonar el repositorio
git clone https://github.com/juamorrio/DraftLeague.git
cd DraftLeague

# 2. Copiar y rellenar el fichero de variables de entorno
cp .env.example .env
# Editar .env con tus secretos (DB_PASSWORD, JWT_SECRET, API_FOOTBALL_KEY)

# 3. Levantar el stack completo
docker-compose up --build
```

Servicios disponibles:
- **Frontend (web)**: http://localhost:8081
- **Backend API**: http://localhost:9090
- **ML Service**: http://localhost:8000
- **MariaDB**: `localhost:3306`

### Opción B — Manual (desarrollo)

**Requisitos**: Java 21, Maven 3.9+, Node.js 18+, MariaDB 11, Python 3.10+

```bash
# Backend
./mvnw spring-boot:run

# Frontend (en otra terminal)
cd frontend
npm install
npm start             # Expo dev server
npm run android       # emulador Android
npm run ios           # simulador iOS
npm run web           # navegador

# ML Service (en otra terminal)
cd ml-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

---

## Variables de entorno

| Variable | Servicio | Descripción |
|---|---|---|
| `DB_URL` | backend | JDBC URL de MariaDB |
| `DB_USERNAME` | backend / scripts | Usuario de la base de datos |
| `DB_PASSWORD` | backend / scripts | Contraseña de la base de datos |
| `JWT_SECRET` | backend | Secreto de firma JWT (mín. 64 caracteres) |
| `API_FOOTBALL_KEY` | backend | API key de API-Football |
| `INTERNAL_API_KEY` | backend ↔ ml-service | Clave compartida para llamadas internas |
| `ML_XGBOOST_URL` | backend | URL del microservicio ML |
| `EXPO_PUBLIC_API_BASE` | frontend | URL pública del backend |
| `APP_VARIANT` | frontend (build) | `dev` / `preview` / `production` |

Genera un JWT_SECRET seguro con: `openssl rand -base64 64`.

---

## Endpoints principales

Todos los endpoints están bajo `/api/v1/` salvo autenticación. Las rutas marcadas con 🔒 requieren JWT en `Authorization: Bearer <token>`.

| Recurso | Endpoints |
|---|---|
| **Auth** | `POST /auth/register` · `POST /auth/login` · `GET /auth/me` 🔒 |
| **Players** | `GET /api/v1/players` · `GET /api/v1/players/{id}` · `POST /api/v1/players/purchase` 🔒 · `GET /api/v1/players/{id}/market-value-history` |
| **Leagues** | `POST /api/v1/leagues` 🔒 · `GET /api/v1/leagues/mine` 🔒 · `POST /api/v1/leagues/join` 🔒 |
| **Teams** | `GET /api/v1/teams/{leagueId}` 🔒 · `PUT /api/v1/teams/{id}/players` 🔒 · `POST /api/v1/teams/{id}/captain` 🔒 |
| **Market** | `GET /api/v1/market/{leagueId}` 🔒 · `POST /api/v1/market/bid` 🔒 |
| **Matches** | `GET /api/v1/matches/gameweek/{n}` · `GET /api/v1/matches/upcoming` |
| **Statistics** | `GET /api/statistics/player/{id}` · `GET /api/statistics/team/{id}` |
| **ML Predictions** | `GET /api/ml/predict/player/{id}` · `GET /api/ml/predict/team/{id}` |
| **Admin** | `POST /api/v1/admin/sync-gameweek/{n}` 🔒 ADMIN · `POST /api/v1/admin/market/{leagueId}/refresh` 🔒 ADMIN |

---

## Tests y cobertura

### Backend

```bash
./mvnw test                          # Ejecuta todos los tests
./mvnw test -Dtest=NombreClase       # Ejecuta una clase concreta
./mvnw verify                        # Tests + reporte JaCoCo
```

JaCoCo enforce: **70 % de cobertura de líneas en la capa `services`** (DTOs, models, config y main excluidos).

### Frontend

```bash
cd frontend
npm test                             # Jest
npm test -- --coverage               # Con cobertura
```

### ML Service

```bash
cd ml-service
pytest
```

---

## CI/CD

Workflows de GitHub Actions configurados en [.github/workflows/](.github/workflows/):

- **`backend-tests.yml`** — Ejecuta `mvnw test` en cada push y PR.
- **`frontend-tests.yml`** — Ejecuta Jest del frontend.
- **`CI_pullRequest.yml`** — Validación general en PRs.
- **`CI_conventionalCommits.yml`** — Valida que los mensajes de commit cumplen el estándar Conventional Commits.
- **`CI_Codacy.yml`** — Análisis estático de calidad de código.
- **`CD_docker.yml`** — Build y publicación de imágenes Docker.

---

## Documentación adicional

- [docs/memoria-descripcion-proyecto.md](docs/memoria-descripcion-proyecto.md) — Memoria descriptiva del TFG.
- [docs/uml-sistema.md](docs/uml-sistema.md) — Diagramas UML del sistema.
- [CLAUDE.md](CLAUDE.md) — Guía técnica de la arquitectura para colaboradores.

---

## Autor

**Juan Moreno Ríos** — Trabajo Fin de Grado · Universidad
GitHub: [@juamorrio](https://github.com/juamorrio)

---

## Licencia

Este proyecto es un trabajo académico con fines educativos. Los datos de jugadores y partidos se obtienen mediante la API pública de API-Football respetando sus términos de uso.
