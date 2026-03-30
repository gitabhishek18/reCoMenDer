# Movie Recommendation System

A backend-centric movie recommendation system built with Spring Boot, JPA/Hibernate, MySQL, JWT authentication, and a lightweight HTML/CSS/JavaScript frontend served directly from the backend.

This repository is organized for clean GitHub presentation while keeping the current backend implementation and `application.properties` layout intact.

## Repository Layout

```text
movie-recommendation-system/
|-- README.md
|-- .gitignore
|-- .gitattributes
|-- backend/
|   |-- .mvn/
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       |   |-- java/com/example/recomender/
|       |   `-- resources/
|       |       |-- application.properties
|       |       |-- artifacts/
|       |       `-- static/
|       `-- test/
|-- trainer/
|   |-- requirements.txt
|   `-- scripts/
|-- data/
|   `-- raw/
|       `-- README.txt
|-- artifacts/                         # Local generated trainer output; excluded from Git
|-- db/
|   `-- README.md
|-- docs/
|   `-- architecture-notes.md
`-- screenshots/
    |-- 01-login-page.png
    |-- 02-dashboard-page.png
    |-- 03-search-page.png
    |-- 04-movie-details-page.png
    |-- 05-ratings-page.png
    `-- 06-recommendations-page.png
```

## What Lives Where

- `backend/`: Spring Boot API, JWT auth, JPA entities/repositories/services/controllers, runtime recommendation artifacts, and the frontend actually served by Spring Boot.
- `trainer/`: Python scripts for popularity, collaborative filtering, content-based recommendations, hybrid evaluation, and MySQL loading utilities.
- `data/raw/`: local raw dataset drop location for MovieLens-style source files. Keep the `README.txt`; exclude the CSVs from Git.
- `artifacts/`: local generated offline outputs such as sparse matrices, similarity files, metrics, and evaluation assets. Exclude this folder from Git.
- `db/`: reserved for schema notes, SQL exports, seed scripts, or migrations if you add them later.
- `docs/`: short project-facing documentation for architecture and maintenance notes.
- `screenshots/`: replace the placeholder filenames with actual UI screenshots before publishing.

## Key Features

- User registration and login
- JWT-based authentication
- Protected `/me` endpoints
- Movie search and movie detail flows
- Ratings capture
- Recommendation endpoints for:
  - popularity-based recommendations
  - collaborative filtering
  - content-based recommendations
  - hybrid recommendations

## Tech Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- Python 3
- Static HTML, CSS, and JavaScript frontend

## Backend Notes

- The backend module folder is now `backend/`.
- The existing Java package and Maven artifact still use the current `recomender` spelling to avoid unnecessary code changes.
- `backend/src/main/resources/application.properties` is intentionally unchanged.
- The frontend source of truth is `backend/src/main/resources/static/`.

## Running The Backend

From the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Or with Maven already installed:

```powershell
cd backend
mvn spring-boot:run
```

## Offline Training Workflow

Typical local workflow:

1. Put the raw dataset files into `data/raw/`.
2. Install Python dependencies from `trainer/requirements.txt`.
3. Run the scripts inside `trainer/scripts/` from the repository root.
4. Generate trainer outputs into the top-level `artifacts/` folder.
5. Copy the runtime JSON files required by the backend into `backend/src/main/resources/artifacts/` when you refresh recommendation artifacts.

Runtime artifact files currently expected by the backend:

- `popular.json`
- `similar_items_cf.json`
- `similar_items_content.json`

## Before Public Upload

- Review `backend/src/main/resources/application.properties` for any local-only database or JWT values.
- Review `trainer/scripts/config.py` for local database settings.
- Replace the placeholder files in `screenshots/` with actual screenshots.
- Keep generated data and build output out of Git by using the provided `.gitignore`.

## Suggested Repository Name

`movie-recommendation-system`
