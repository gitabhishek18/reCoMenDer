# Architecture Notes

## Overview

This project is split into two main execution areas:

- `backend/` runs the Spring Boot application, exposes REST APIs, handles authentication, and serves the lightweight browser UI from static resources.
- `trainer/` contains the offline Python pipeline that prepares recommendation artifacts from raw movie and rating data.

## Backend Structure

The backend keeps a standard Spring Boot layout:

- `src/main/java/com/example/recomender/`
  - `auth/`: JWT, login/register, security configuration
  - `movie/`: movie entity, repository, search/list endpoints
  - `rating/`: rating entity, repository, service, and endpoints
  - `reco/`: recommendation loading and recommendation endpoints
  - `user/`: user entity, repository, and profile endpoints
  - `common/`: shared API error handling
- `src/main/resources/application.properties`
  - kept unchanged by request
- `src/main/resources/static/`
  - frontend pages served directly by Spring Boot
- `src/main/resources/artifacts/`
  - runtime recommendation JSON files loaded by the backend

## Data Flow

1. Raw CSV data is stored locally in `data/raw/`.
2. Python scripts in `trainer/scripts/` read that raw data and generate derived outputs into `artifacts/`.
3. Backend runtime recommendation files are copied into `backend/src/main/resources/artifacts/`.
4. The Spring Boot application loads those classpath resources and exposes recommendation endpoints to the frontend/API clients.

## Why The Frontend Stays Inside The Backend

The repository previously had duplicate frontend copies outside the Spring Boot app. Those were removed so there is one clear frontend source of truth:

- `backend/src/main/resources/static/`

This keeps the project simpler to run and easier to present on GitHub.

## Repository Hygiene Decisions

- The old top-level duplicate `frontend/` folder was removed.
- Generated folders such as `backend/target/` and Python cache folders are excluded from Git.
- Raw datasets and trainer-generated artifacts stay outside version control.
- The backend folder was renamed from `recomender/` to `backend/` without changing Java package names or `application.properties`.

## Notes For Future Cleanup

- If you later want a public-safe repository, move database and JWT secrets out of committed config files.
- If the runtime artifact JSON files become too large for convenient Git history, document a regeneration step and exclude them as well.
