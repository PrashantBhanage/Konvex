# Konvex TODO

## Phase 1 — Correctness and Persistence

- [x] 1.1 Thread safety audit. Review EventWindow concurrency, make it thread-safe, and add a concurrent ingest test with no exceptions, lost state, or duplicate state.
- [x] 1.2 Grid index fix. Make neighbor-cell lookup cover the full max-distance radius at any latitude; handle antimeridian wrap and poles. Add high-latitude and antimeridian tests.
- [ ] 1.3 Out-of-order events. Define and implement behavior for timestamps older than the current window cutoff; never move the cutoff backwards. Document and test the decision.
- [ ] 1.4 Extract EventWindow behind an interface (for example EventStore), keeping the current in-memory implementation and behavior.
- [ ] 1.5 PostgreSQL persistence using Spring Data JPA + Flyway migrations. Persist accepted events and correlation matches while keeping the in-memory window as the matching fast path.
- [ ] 1.6 History API: GET /api/matches?from=&to=&source=&page=&size= and GET /api/events/{source}/{eventId}, with validated ISO-8601 parameters and API-key protection.
- [ ] 1.7 Testcontainers integration tests with real PostgreSQL for persistence and history API.
- [ ] 1.8 Update docs/TODO.md, run the full test suite, verify Phase 1, then stop.

## Phase 2 — Packaging, Observability, and Proof

- [ ] 2.1 Dockerfile with a Java 21 multi-stage build and non-root user; docker-compose.yml with app + PostgreSQL, environment variables, and healthchecks; clean-clone docker compose up.
- [ ] 2.2 Swagger/OpenAPI via springdoc with documented request/response models and API-key header; decide whether docs are public and communicate the tradeoff before implementation.
- [ ] 2.3 Actuator + Micrometer metrics for events processed, matches found, current window size, OpenSky success/failure counts, and ingest latency; expose Prometheus endpoint.
- [ ] 2.4 Configurable in-memory rate limiting on POST /api/events with 429 response and tests.
- [ ] 2.5 Benchmark module comparing brute-force candidate scan vs grid index at 10k, 100k, and 1M events; write measured results, hardware info, and reproduction steps to docs/BENCHMARKS.md.
- [ ] 2.6 JaCoCo coverage report in CI and README badge.
- [ ] 2.7 Update docs/TODO.md, run the full suite, verify docker compose up, then stop.

## Phase 3 — Visualization and Deployment

- [ ] 3.1 Read-only UI endpoints for current events and recent matches; bounded results, CORS, and an explicit UI auth strategy decision before implementation.
- [ ] 3.2 React + Vite + Leaflet frontend in /ui with live map, match visualization, recent-match panel, and polling (SSE only if simple).
- [ ] 3.3 Demo framing that shows matches between different aircraft (different icao24) close in space and time.
- [ ] 3.4 Deployment config for the platform selected later, including Docker/compose changes, environment variable docs, and docs/DEPLOY.md; no secrets in repo.
- [ ] 3.5 README overhaul with pitch, Mermaid architecture, features, quick start, API examples, design decisions, scaling path, limitations, benchmark summary, badges, demo GIF placeholder, and live-link placeholder.
- [ ] 3.6 Final pass: full tests, clean-clone setup verification, TODO update, and final summary.
