---
id: sample
title: Sample
sidebar_label: Android
---

# Sample

MediatorK ships with two runnable samples: a **Kotlin/JVM** module under [
`/sample`](https://github.com/fajrbahr/MediatorK/tree/main/sample) and a full **Android** app under [
`/sample-android`](https://github.com/fajrbahr/MediatorK/tree/main/sample-android).

---

## Android Sample — Prayer Times

The Android sample is a standalone Jetpack Compose app that fetches daily prayer times and Islamic calendar months from
the [Aladhan API](https://aladhan.com/prayer-times-api). It is structured in three layers to show the *before* and
*after* story of adopting MediatorK — and a third *after super* layer that adds every built-in pipeline behavior.

### Open in Android Studio

```
# Clone the repo, then open sample-android/ as its own project:
File → Open → <repo-root>/sample-android
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

## Kotlin/JVM Sample

The [`/sample`](https://github.com/fajrbahr/MediatorK/tree/main/sample) module shows commands, queries, notifications,
and pipeline behaviors in plain Kotlin/JVM with no UI framework:

- **ViewModel before & after
  ** — [OrderViewModelBefore.kt](https://github.com/fajrbahr/MediatorK/blob/main/sample/src/main/kotlin/sample/android/OrderViewModelBefore.kt)
  vs [OrderViewModelAfter.kt](https://github.com/fajrbahr/MediatorK/blob/main/sample/src/main/kotlin/sample/android/OrderViewModelAfter.kt)
- **Commands, Queries, Notifications
  ** — [command/](https://github.com/fajrbahr/MediatorK/tree/main/sample/src/main/kotlin/sample/command), [query/](https://github.com/fajrbahr/MediatorK/tree/main/sample/src/main/kotlin/sample/query), [notification/](https://github.com/fajrbahr/MediatorK/tree/main/sample/src/main/kotlin/sample/notification)
- **Pipeline behaviors** — logging, auth, retry, validation, tracing,
  metrics: [behaviors/](https://github.com/fajrbahr/MediatorK/tree/main/sample/src/main/kotlin/sample/behaviors)
- **Tests** — handler and ViewModel tests with no mocking
  library: [SampleHandlerTest.kt](https://github.com/fajrbahr/MediatorK/blob/main/sample/src/test/kotlin/sample/SampleHandlerTest.kt)

Browse the full module on [GitHub →](https://github.com/fajrbahr/MediatorK/tree/main/sample)
