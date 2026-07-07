# Spring Boot: Prayer Times (Vertical Slice Sample)

A Spring Boot WebFlux REST API demonstrating **vertical slice architecture** with MediatorK.

## Features

Two REST endpoints that fetch data from the [Aladhan API](https://aladhan.com/):

- `GET /prayer-times/{city}` — Prayer times for a given city (cached)
- `GET /islamic-months` — Islamic calendar months (cached)

## Architecture

Pure vertical slices — each feature owns its complete request → handler → model → controller pipeline:

```
src/main/kotlin/com/fajrbahr/mediatork/sample/spring/
  prayertimes/
    PrayerTimes.kt              ← Models (PrayerTime, TodayPrayerTimes)
    GetPrayerTimesQuery.kt      ← Request + Handler + Registrar
    PrayerTimesController.kt    ← REST endpoint
  islamicmonths/
    IslamicMonth.kt             ← Model
    GetIslamicMonthsQuery.kt    ← Request + Handler + Registrar
    IslamicMonthsController.kt  ← REST endpoint
  AladhanCache.kt               ← Shared cache infrastructure
  MediatorConfig.kt             ← Spring bean wiring
```

## Run

```bash
# Build
./gradlew bootRun

# Or build and run the JAR
./gradlew bootJar
java -jar build/libs/sample-spring-*.jar

# Test
curl http://localhost:8080/prayer-times/London
curl http://localhost:8080/islamic-months
```

## Why vertical slices?

- **Feature-focused**: Each feature is fully isolated. Add a new slice without touching existing code.
- **Self-contained**: The handler knows how to fetch, parse, and cache. No separate data layer.
- **Easy to test**: Test an entire feature end-to-end in one test.
- **Spring-native**: Registrars and cache are Spring `@Component`s, wired via constructor injection.

Compare this to the layered approach (before the refactor) — same API, but now with clear ownership per feature and no cross-cutting data layer abstractions.

## Controllers

Controllers are thin — they only dispatch to the mediator:

```kotlin
@RestController
class PrayerTimesController(private val mediator: Mediator) {
    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): TodayPrayerTimes =
        mediator.send(GetPrayerTimesQuery(city = city))
}
```

Handlers and registrars are Spring `@Component`s, automatically discovered and injected:

```kotlin
@Component
class PrayerTimesRegistrar(private val cache: AladhanCache) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetPrayerTimesHandler(cache)
    }
}
```

The `MediatorConfig` collects all registrars and builds the mediator:

```kotlin
@Bean
fun mediator(registrars: List<MediatorRegistrar>): Mediator =
    MediatorFactory.create(registrars = registrars)
```

## Dependencies

- **Spring Boot 3.3.6** — Servlet + WebFlux
- **MediatorK 0.8.1** — Request/handler mediator
- **Jackson** — JSON serialization
- **Logback** — Logging

## Next

→ See [vertical slice architecture](../../website/docs/vertical-slice.md) for more context.

→ Compare with [sample-ktor](../sample-ktor) (same features, Ktor + direct routing).
