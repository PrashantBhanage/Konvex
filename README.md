# Konvex

Konvex is a small real-time event correlation service built with Java 21 and Spring Boot.

It accepts events from external sources, keeps a short event-time window, and looks for events that are close to each other in both location and time.

The current defaults are:

- Maximum distance: 5 km
- Maximum time gap: 60 seconds

## How it works

```
External event
     |
     v
POST /api/events
     |
     v
EventIngestionService
     |
     v
CorrelationEngine
     |
     +--> EventWindow
     |      |
     |      +--> time-based expiry
     |      +--> geographic candidate lookup
     |
     +--> MatchingService
            |
            +--> Haversine distance
            +--> time-gap check
     |
     +--> PostgreSQL history
     |
     v
Correlation results
```

An event contains:

```json
{
  "source": "camera-a",
  "eventId": "evt-102",
  "latitude": 28.6129,
  "longitude": 77.2295,
  "timestamp": "2026-10-03T17:00:00Z",
  "metadata": {
    "type": "vehicle"
  }
}
```

A match is returned when both configured thresholds are satisfied.

## API

### Health

```bash
curl http://localhost:8080/health
```

Response:

```json
{
  "status": "UP"
}
```

### Ingest an event

Set an API key before starting the service:

```bash
export KONVEX_API_KEY=my-local-key
```

Then:

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -H "X-API-Key: my-local-key" \
  -d '{
    "source": "camera-a",
    "eventId": "evt-101",
    "latitude": 28.6129,
    "longitude": 77.2295,
    "timestamp": "2026-10-03T17:00:00Z",
    "metadata": {}
  }'
```

The response contains the accepted event ID and any matches found in the recent window.

### Match history

Query persisted correlation matches with optional ISO-8601 `from`/`to` timestamps and source filtering. `page` is zero-based and `size` must be between 1 and 100.

```bash
curl "http://localhost:8080/api/matches?from=2026-03-15T10:00:00Z&to=2026-03-15T11:00:00Z&source=camera-a&page=0&size=20" \
  -H "X-API-Key: my-local-key"
```

### Event history

Fetch all persisted observations for a source and event ID, newest first:

```bash
curl "http://localhost:8080/api/events/OpenSky/abc123" \
  -H "X-API-Key: my-local-key"
```

## OpenSky integration

Konvex also includes a live OpenSky integration.

Every poll cycle it:

1. Fetches current aircraft states from OpenSky.
2. Validates latitude, longitude, and aircraft ID.
3. Converts the aircraft state into a Konvex event.
4. Sends the event through the same correlation engine used by the REST API.

OpenSky polling is enabled by default and runs every 30 seconds.

The polling settings can be changed in `src/main/resources/application.properties`:

```properties
konvex.opensky.enabled=true
konvex.opensky.poll-interval-ms=30000
konvex.opensky.initial-delay-ms=5000
konvex.opensky.base-url=https://opensky-network.org
```

The local correlation demo is separate and disabled by default. Enable it with:

```properties
konvex.demo.enabled=true
```

## Configuration

Main matching settings:

```properties
konvex.matching.max-distance-km=5.0
konvex.matching.max-time-gap-seconds=60
```

API-key authentication:

```properties
konvex.security.api-key=${KONVEX_API_KEY:}
```

Do not commit a real API key.

## Run locally

You need Java 21.

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Run the test suite with:

```bash
./mvnw test
```

## Project structure

```
src/main/java/io/konvex
├── config
├── controller
├── engine
├── integration
├── model
├── persistence
│   ├── entity
│   └── repository
├── security
├── service
├── util
└── web
```

The important flow is:

```
OpenSky / REST client
        |
        v
      Event
        |
        v
CorrelationEngine
        |
        +--> EventWindow
        |
        +--> MatchingService
        |
        v
 CorrelationMatch
```

## PostgreSQL history

Konvex persists every accepted event and correlation match in PostgreSQL. The in-memory EventWindow remains the real-time matching fast path; PostgreSQL is used for durable history.

Set the database connection with environment variables:

```bash
export KONVEX_DB_URL=jdbc:postgresql://localhost:5432/konvex
export KONVEX_DB_USERNAME=konvex
export KONVEX_DB_PASSWORD=your-password
```

Flyway owns the schema and Hibernate validates it on startup. The migration creates timestamp indexes for event and match history queries.

## Current scope

Konvex now combines an in-memory spatial-temporal correlation window with PostgreSQL-backed event and match history.

It does not yet provide distributed event processing or shared state across multiple application instances.

## Development

Every change should be covered by tests where practical. GitHub Actions runs the Maven test suite on pushes and pull requests.
