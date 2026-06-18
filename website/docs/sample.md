---
id: sample
title: Sample
sidebar_label: Samples
---

# Sample

MediatorK ships with four runnable samples:

| Sample | Module | Framework |
|--------|--------|-----------|
| [Android](#android-sample--prayer-times) | [`/samples/sample-android`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-android) | Jetpack Compose |
| [Ktor](#ktor-sample--prayer-times) | [`/samples/sample-ktor`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-ktor) | Ktor Server |
| [Spring Boot](#spring-boot-sample--prayer-times) | [`/samples/sample-spring`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-spring) | Spring WebFlux |
| [Kotlin/JVM](#kotlinjvm-sample) | [`/samples/sample`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample) | Plain JVM |

The Android, Ktor, and Spring samples all use the same **before / after / after super** structure against the
[Aladhan prayer-times API](https://aladhan.com/prayer-times-api) so the progression is easy to compare across
platforms.

---

## Android Sample — Prayer Times

The Android sample is a standalone Jetpack Compose app that fetches daily prayer times and Islamic calendar months from
the [Aladhan API](https://aladhan.com/prayer-times-api). It is structured in three layers to show the *before* and
*after* story of adopting MediatorK — and a third *after super* layer that adds every built-in pipeline behavior.

### Open in Android Studio

```
# Clone the repo, then open samples/sample-android/ as its own project:
File → Open → <repo-root>/samples/sample-android
```

The folder contains its own `settings.gradle.kts` and `build.gradle.kts`, so Android Studio treats it as a standalone
project. The library is pulled from Maven Central — no local build of the root project is needed.

---

### Before — standard Android architecture

`ViewModel → UseCase → Repository → DataSource`

```
before/
  data/
    AladhanRemoteDataSource.kt   ← raw Ktor HTTP call
    AladhanCacheDataSource.kt    ← in-memory cache
    AladhanRepository.kt         ← wires remote + cache
  domain/
    GetPrayerTimesUseCase.kt     ← delegates to Repository
  viewmodel/
    BeforePrayerTimesViewModel.kt
  ui/
    BeforePrayerTimesScreen.kt
```

The ViewModel calls `GetPrayerTimesUseCase` directly. Adding a second feature means adding another use-case parameter to
the constructor.

```kotlin
class BeforePrayerTimesViewModel(
    private val getPrayerTimes: GetPrayerTimesUseCase,
) : ViewModel() {
    val uiState = refreshTrigger.flatMapLatest {
        flow {
            emit(BeforeUiState.Loading)
            val result = runCatching { getPrayerTimes() }
            emit(
                result.fold(
                    onSuccess = { BeforeUiState.Success(it) },
                    onFailure = { BeforeUiState.Error(it.message ?: "Error") },
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BeforeUiState.Loading)
}
```

---

### After — MediatorK handlers

`ViewModel → Mediator → Handler`

```
after/
  data/
    AladhanRemoteDataSource.kt   ← same raw Ktor HTTP call
    AladhanCacheDataSource.kt    ← same in-memory cache
  domain/
    GetPrayerTimesRequest.kt     ← Request<TodayPrayerTimes>
    PrayerTimesHandler.kt        ← RequestHandler (inline cache logic)
    PrayerTimesRegistrar.kt      ← MediatorRegistrar
  viewmodel/
    AfterPrayerTimesViewModel.kt
  ui/
    AfterPrayerTimesScreen.kt
```

The ViewModel sends a typed `Request` through the `Mediator`. Adding a second feature means adding a new handler — the
ViewModel constructor stays the same.

```kotlin
class AfterPrayerTimesViewModel(
    private val mediator: Mediator,
) : ViewModel() {
    val uiState = refreshTrigger.flatMapLatest {
        flow {
            emit(AfterUiState.Loading)
            val result = runCatching { mediator.send(GetPrayerTimesRequest()) }
            emit(
                result.fold(
                    onSuccess = { AfterUiState.Success(it) },
                    onFailure = { AfterUiState.Error(it.message ?: "Error") },
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AfterUiState.Loading)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                AfterPrayerTimesViewModel(
                    MediatorFactory.create(registrars = listOf(PrayerTimesRegistrar(cache)))
                )
            }
        }
    }
}
```

---

### After Super — pipeline behaviors

The same `After` setup, extended with all six built-in pipeline behaviors. Logs are captured into a shared buffer and
rendered inside the screen as a terminal-style card.

```kotlin
MediatorFactory.create(
    registrars = listOf(PrayerTimesRegistrar(cache)),
    pipelineBehaviors = listOf(
        RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
        LoggingPipelineBehavior(logger = { msg -> logs.add(msg); Log.d("MediatorK", msg) }, order = -100),
        TimingPipelineBehavior(order = 0) { name, ms ->
            val line = "⏱ $name took ${ms}ms"
            logs.add(line)
            Log.d("MediatorK", line)
        },
        TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
        RequestCounterPipelineBehavior(order = 20),
        ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
            val line = "❌ ${req::class.simpleName}: ${err.message}"
            logs.add(line)
            Log.e("MediatorK", line)
        },
    ),
)
```

**Pipeline execution order** (lower `order` = outermost):

| Order | Behavior                         | Role                                   |
|-------|----------------------------------|----------------------------------------|
| −200  | `RetryPipelineBehavior`          | Retries the entire pipeline on failure |
| −100  | `LoggingPipelineBehavior`        | Logs `→ Request` and `← Response`      |
| 0     | `TimingPipelineBehavior`         | Measures handler execution time        |
| 10    | `TimeoutPipelineBehavior`        | Cancels if handler exceeds 10 s        |
| 20    | `RequestCounterPipelineBehavior` | Counts requests per type               |
| MAX   | `ErrorTrackingPipelineBehavior`  | Captures unhandled exceptions          |

Logs from each pipeline pass appear live in the screen and in Logcat under the tag `MediatorK`.

---

## Ktor Sample — Prayer Times

The [`/samples/sample-ktor`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-ktor) module is a Ktor (Netty) HTTP server
using the same three-layer structure as the Android sample.

### Run

```bash
./gradlew :samples:sample-ktor:run
# Server starts on http://localhost:8080
```

### Before — standard Ktor architecture

`Route → UseCase → Repository → DataSource`

```
before/
  data/
    AladhanRemoteDataSource.kt   ← raw HTTP call
    AladhanCacheDataSource.kt    ← in-memory cache
    AladhanRepository.kt         ← wires remote + cache
  domain/
    GetPrayerTimesUseCase.kt
    GetIslamicMonthsUseCase.kt
  routes/
    BeforeRoutes.kt              ← constructs the chain manually
```

```kotlin
fun Application.configureBeforeRoutes() {
    val getPrayerTimes = GetPrayerTimesUseCase(AladhanRepository(...))
    routing {
        get("/before/prayer-times/{city}") {
            call.respond(getPrayerTimes(call.parameters["city"]!!))
        }
    }
}
```

### After — MediatorK handlers

`Route → Mediator → Handler`

```
after/
  data/
    AladhanCacheDataSource.kt    ← in-memory cache
  domain/
    GetPrayerTimesRequest.kt     ← Request<TodayPrayerTimes> + Handler
    GetIslamicMonthsRequest.kt   ← Request<List<IslamicMonth>> + Handler
    AppRegistrar.kt              ← MediatorRegistrar
  routes/
    AfterRoutes.kt
```

```kotlin
fun Application.configureAfterRoutes() {
    val mediator = MediatorFactory.create(registrars = listOf(AppRegistrar(cache)))
    routing {
        get("/after/prayer-times/{city}") {
            call.respond(mediator.send(GetPrayerTimesRequest(city = call.parameters["city"]!!)))
        }
    }
}
```

### After Super — pipeline behaviors

Same `After` setup, extended with validation, retry, logging, timing, timeout, counter, and error-tracking behaviors.
The `AfterSuperPrayerTimesResponse` wraps the data with the request count from `RequestCounterPipelineBehavior`.

```bash
curl http://localhost:8080/aftersuper/prayer-times/London
# { "prayerTimes": {...}, "requestCount": 1 }
```

---

## Spring Boot Sample — Prayer Times

The [`/samples/sample-spring`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-spring) module is a Spring Boot WebFlux
application using the same three-layer structure.

### Run

```bash
./gradlew :samples:sample-spring:bootRun
# Server starts on http://localhost:8081
```

### Before — standard Spring architecture

`Controller → UseCase → Repository → DataSource`

```
before/
  data/
    AladhanRemoteDataSource.kt   ← @Component, raw HTTP call
    AladhanCacheDataSource.kt    ← @Component, in-memory cache
    AladhanRepository.kt         ← @Repository, wires remote + cache
  domain/
    GetPrayerTimesUseCase.kt     ← @Service
    GetIslamicMonthsUseCase.kt   ← @Service
  controller/
    BeforePrayerTimesController.kt ← @RestController
```

```kotlin
@RestController
@RequestMapping("/before")
class BeforePrayerTimesController(
    private val getPrayerTimes: GetPrayerTimesUseCase,
    private val getIslamicMonths: GetIslamicMonthsUseCase,
) {
    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): TodayPrayerTimes =
        getPrayerTimes(city)
}
```

### After — MediatorK handlers

`Controller → Mediator → Handler`

```
after/
  data/
    AladhanCacheDataSource.kt    ← @Component
  domain/
    GetPrayerTimesRequest.kt     ← Request<TodayPrayerTimes> + Handler
    GetIslamicMonthsRequest.kt   ← Request<List<IslamicMonth>> + Handler
    AppRegistrar.kt              ← @Component, MediatorRegistrar
  config/
    MediatorConfig.kt            ← @Configuration, @Bean mediator
  controller/
    AfterPrayerTimesController.kt ← @RestController
```

```kotlin
@RestController
@RequestMapping("/after")
class AfterPrayerTimesController(@Qualifier("mediator") private val mediator: Mediator) {
    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): TodayPrayerTimes =
        mediator.send(GetPrayerTimesRequest(city = city))
}
```

### After Super — pipeline behaviors

The same `After` setup. `MediatorConfig` also exposes a `mediatorWithBehaviors` bean that includes all six pipeline
behaviors. `AfterSuperPrayerTimesController` injects this bean and responds with the request count.

```bash
curl http://localhost:8081/aftersuper/prayer-times/London
# { "prayerTimes": {...}, "requestCount": 1 }
```

---

## Kotlin/JVM Sample

The [`/samples/sample`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample) module shows commands, queries, notifications,
and pipeline behaviors in plain Kotlin/JVM with no UI framework:

- **ViewModel before & after** — [OrderViewModelBefore.kt](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/main/kotlin/sample/android/OrderViewModelBefore.kt)
  vs [OrderViewModelAfter.kt](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/main/kotlin/sample/android/OrderViewModelAfter.kt)
- **Commands, Queries, Notifications** — [command/](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample/src/main/kotlin/sample/command), [query/](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample/src/main/kotlin/sample/query), [notification/](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample/src/main/kotlin/sample/notification)
- **Pipeline behaviors** — logging, auth, retry, validation, tracing,
  metrics: [behaviors/](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample/src/main/kotlin/sample/behaviors)
- **Invoice slice** — complete vertical slice with transaction, three-layer validation, and streaming: [invoice/](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample/src/main/kotlin/sample/invoice)
- **Tests** — handler and ViewModel tests with no mocking
  library: [SampleHandlerTest.kt](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/test/kotlin/sample/SampleHandlerTest.kt)

### Invoice Slice — Vertical Slice Showcase

The [`invoice/`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample/src/main/kotlin/sample/invoice) package is a self-contained vertical slice that demonstrates three advanced features together:

| File | What it shows |
|------|---------------|
| [`InvoiceDomain.kt`](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/main/kotlin/sample/invoice/InvoiceDomain.kt) | Domain model, requests (`CreateInvoiceCommand`, `StreamInvoicesQuery`), in-memory repo with `TransactionProvider` |
| [`InvoiceHandlers.kt`](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/main/kotlin/sample/invoice/InvoiceHandlers.kt) | `RequestHandler` for commands/queries, `StreamRequestHandler` for streaming, `MediatorRegistrar` |
| [`InvoiceValidators.kt`](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/main/kotlin/sample/invoice/InvoiceValidators.kt) | All three `ValidationScope` levels: REQUEST, DOMAIN, PERSISTENCE |
| [`InvoiceDemo.kt`](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/main/kotlin/sample/invoice/InvoiceDemo.kt) | Runnable demos (Test25–28): transaction commit/rollback, validation scopes, streaming |

**Test25** — `TransactionPipelineBehavior` commits on success, **Test26** — rolls back on failure:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(InvoiceRegistrar(repo)),
    pipelineBehaviors = listOf(
        ValidationBehavior(listOf(CreateInvoiceRequestValidator())),
        TransactionPipelineBehavior(transactionProvider = repo.transactionProvider),
    ),
)
mediator.send(CreateInvoiceCommand(id = "INV-001", amount = 500.0))
mediator.send(ApproveInvoiceCommand(id = "INV-001"))
```

**Test27** — `ValidationScope` in action — REQUEST is caught before the handler; DOMAIN is caught inside:

```kotlin
// REQUEST scope — caught in the pipeline before handler entry
mediator.send(CreateInvoiceCommand(id = "BADINPUT", amount = 100.0))  // throws ValidationException

// DOMAIN scope — caught inside the handler after loading state
mediator.send(CreateInvoiceCommand(id = "INV-020", amount = 300.0))   // first call: OK
mediator.send(CreateInvoiceCommand(id = "INV-020", amount = 300.0))   // duplicate: throws ValidationException
```

**Test28** — `StreamRequest<Invoice>` — lazy flow, filtered by status:

```kotlin
mediator.stream(StreamInvoicesQuery(status = InvoiceStatus.APPROVED))
    .collect { invoice -> println("${invoice.id} — $${invoice.amount}") }
```

**Integration tests** — [`InvoiceIntegrationTest.kt`](https://github.com/fajrbahr/MediatorK/blob/main/samples/sample/src/test/kotlin/sample/InvoiceIntegrationTest.kt) uses `buildHandlerTestHarness` to test the full slice end-to-end with real handlers and the real pipeline — no mocks:

```kotlin
val h = buildHandlerTestHarness(
    pipelineBehaviors = listOf(
        ValidationBehavior(listOf(CreateInvoiceRequestValidator())),
        TransactionPipelineBehavior(transactionProvider = repo.transactionProvider),
    ),
) {
    +CreateInvoiceHandler(repo, domainValidator, persistenceValidator)
    +ApproveInvoiceHandler(repo)
    +GetInvoiceHandler(repo)
    registerStream(StreamInvoicesHandler(repo))
}

h.given(CreateInvoiceCommand(id = "INV-200", amount = 500.0))   // arrange
h.send(ApproveInvoiceCommand(id = "INV-200"))                    // act
val invoice = h.query(GetInvoiceQuery(id = "INV-200"))            // assert
assertEquals(InvoiceStatus.APPROVED, invoice.status)
```

Browse the full module on [GitHub →](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample)
