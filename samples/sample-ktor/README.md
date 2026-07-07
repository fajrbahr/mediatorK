# Ktor: Prayer Times (Vertical Slice Sample)

A Ktor/Netty HTTP server demonstrating **vertical slice architecture** with MediatorK.

## Features

Two API endpoints that fetch data from the [Aladhan API](https://aladhan.com/):

- `GET /prayer-times/{city}` — Prayer times for a given city (cached)
- `GET /islamic-months` — Islamic calendar months (cached)

## Architecture

Pure vertical slices — each feature owns its complete request → handler → model pipeline:

```
src/main/kotlin/com/fajrbahr/mediatork/sample/ktor/
  prayertimes/
    PrayerTimes.kt              ← Models (PrayerTime, TodayPrayerTimes)
    GetPrayerTimesQuery.kt      ← Request + Handler + Registrar
  islamicmonths/
    IslamicMonth.kt             ← Model
    GetIslamicMonthsQuery.kt    ← Request + Handler + Registrar
  AladhanCache.kt               ← Shared cache infrastructure
  Application.kt                ← HTTP routing + Mediator setup
```

## Run

```bash
# Build
./gradlew build

# Run
./gradlew run

# Or start the server and test
./gradlew run &
curl http://localhost:8080/prayer-times/London
curl http://localhost:8080/islamic-months
```

## Why vertical slices?

- **Feature-focused**: Each feature is fully isolated. Add a new slice without touching existing code.
- **Self-contained**: The handler knows how to fetch, parse, and cache. No separate data layer.
- **Easy to test**: Test an entire feature end-to-end in one test.

Compare this to the layered approach in the main README — same API, but without the data source / repository / use-case abstraction overhead.

## Routing

Routes are configured inline in `Application.kt`:

```kotlin
routing {
    get("/prayer-times/{city}") {
        val city = call.parameters["city"] ?: return@get ...
        call.respond(mediator.send(GetPrayerTimesQuery(city = city)))
    }
    get("/islamic-months") {
        call.respond(mediator.send(GetIslamicMonthsQuery()))
    }
}
```

Each handler is registered via its `Registrar`:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(
        PrayerTimesRegistrar(cache),
        IslamicMonthsRegistrar(cache),
    ),
)
```

## Dependencies

- **Ktor 3.1.3** — HTTP server
- **MediatorK 0.8.1** — Request/handler mediator
- **kotlinx-serialization** — JSON encoding
- **Logback** — Logging

## Next

→ See [vertical slice architecture](../../website/docs/vertical-slice.md) for more context.

→ Compare with [sample-spring](../sample-spring) (same features, Spring Boot + controllers).
