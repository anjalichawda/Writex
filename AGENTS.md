# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project layout
- Root: React + Vite frontend (`src/`, `package.json`).
- `backend/`: Spring Boot backend (`backend/src/main/java`, `backend/src/main/resources/application.properties`).
- Frontend and backend are developed/run separately (different toolchains, ports, and dependency managers).

## Development commands
## Frontend (run from repository root)
- Install deps: `npm install`
- Start dev server: `npm run dev` (Vite default: `http://localhost:5173`)
- Build production assets: `npm run build`
- Lint frontend: `npm run lint`
- Preview production build: `npm run preview`

## Backend (run from `backend/`)
- Run app in dev: `mvn spring-boot:run` (configured port: `8000`)
- Run all backend tests: `mvn test`
- Run a single backend test class: `mvn -Dtest=ClassNameTest test`
- Run a single backend test method: `mvn -Dtest=ClassNameTest#methodName test`
- Build backend JAR: `mvn clean package`

## Testing status in current repo
- No frontend test runner config or frontend test files were found.
- No backend test classes were found under `backend/src/test`.

## Architecture overview
## Frontend flow (React/Vite)
- App bootstraps in `src/main.jsx` with `AuthProvider`, router, and toast notifications.
- Routes are defined in `src/App.jsx` and mostly lazy-loaded.
- Protected pages are wrapped by `src/components/auth/ProtectedRoute.jsx`, which depends on auth state from context.
- Auth/session state is managed in `src/context/AuthContext.jsx` using `localStorage` (`token`, `user`) as source of truth across reloads.
- API endpoint strings are centralized in `src/utils/apiPaths.js`.
- HTTP requests go through `src/utils/axiosinstance.js`, which:
  - injects `Authorization: Bearer <token>` from `localStorage`
  - unwraps backend `ApiResponse<T>` into plain `response.data` while preserving app-level metadata on `response._app`
- Editor workflow is concentrated in `src/pages/EditorPage.jsx` and coordinates:
  - chapter CRUD/reorder in client state
  - persistence via `/api/books/:id`
  - AI generation endpoints
  - export endpoints returning blobs for file download

## Backend flow (Spring Boot)
- Entry point: `backend/src/main/java/com/ebook/backend/BackendApplication.java`
- Security is stateless JWT:
  - `security/SecurityConfiguration.java` defines allowed public endpoints, CORS, and filter chain
  - `security/JwtAuthenticationFilter.java` parses bearer token and sets `SecurityContext`
  - `security/JwtService.java` handles token generation/validation
- Controllers (`controller/`) expose API domains:
  - `AuthController` for register/login/profile
  - `BookController` for CRUD + cover upload/download/delete
  - `AIController` for outline/content/grammar/image generation
  - `ExportController` for book export (`pdf` and generic fallback format)
- Business logic is mainly in `service/BookServiceImpl.java`:
  - owner-scoped book listing
  - chapter list merge/update behavior during book updates
  - cover binary storage handling through `BookCover`
- AI integration is in `service/ai/HuggingFaceAIService.java`:
  - text generation through Hugging Face chat-completions endpoint
  - token from `ai.llm.hf.token` or `HF_API_TOKEN`
  - chapter image generation via Pollinations and returned as data URL

## Persistence and data model
- JPA entities in `model/`:
  - `User` (account)
  - `Book` (owned by `User`, has many `Chapter`, optional one-to-one `BookCover`)
  - `Chapter` (content + ordering, many-to-one to `Book`)
  - `BookCover` (binary image data + MIME type)
- Repositories in `repository/` back the entity access patterns.
- DB and app config are in `backend/src/main/resources/application.properties`:
  - MySQL datasource defaults to `jdbc:mysql://localhost:3306/WriteX` with local credentials
  - JPA `ddl-auto=update`
  - multipart upload size limits configured

## Integration assumptions to preserve
- Frontend expects backend responses to be wrapped as `ApiResponse<T>`; axios interceptor unwraps this shape. If backend response format changes, adjust `src/utils/axiosinstance.js` accordingly.
- Frontend base URL and backend CORS/security defaults assume local dev on `localhost:5173` (frontend) and `localhost:8000` (backend).
