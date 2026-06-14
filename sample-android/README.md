# MediatorK Android Sample

A standalone Jetpack Compose app demonstrating MediatorK with real data from the [Aladhan API](https://aladhan.com/prayer-times-api) — daily prayer times and Islamic calendar months.

## Open in Android Studio

This folder is a **self-contained Android project** with its own `settings.gradle.kts`. Open it independently, not as a module of the parent project.

1. Launch Android Studio
2. **File → Open**
3. Navigate to `<repo-root>/sample-android/` and click **Open**
4. Let Gradle sync finish
5. Run on a device or emulator

> The library is fetched from Maven Central. No local build of the parent project is required.

## Structure

```
sample-android/
└── src/main/kotlin/.../
    ├── before/          # Standard Android architecture — no MediatorK
    │   ├── data/        #   RemoteDataSource, CacheDataSource, Repository
    │   ├── domain/      #   UseCase
    │   ├── viewmodel/   #   ViewModel calls UseCase directly
    │   └── ui/          #   Compose screens
    ├── after/           # MediatorK handlers — no repository layer
    │   ├── data/        #   RemoteDataSource, CacheDataSource
    │   ├── domain/      #   Request, Handler, Registrar
    │   ├── model/       #   TodayPrayerTimes, IslamicMonth
    │   ├── viewmodel/   #   ViewModel calls Mediator.send()
    │   └── ui/          #   Compose screens
    └── aftersuper/      # After + all six pipeline behaviors
        ├── viewmodel/   #   Logging, Timing, Retry, Timeout, Counter, ErrorTracking
        └── ui/          #   Screens show pipeline logs inline
```

## What each layer shows

| Screen | Architecture |
|--------|--------------|
| **Before** | `ViewModel → UseCase → Repository → DataSource` |
| **After** | `ViewModel → Mediator → Handler` |
| **After Super** | After + `Retry · Logging · Timing · Timeout · Counter · ErrorTracking` pipeline |

## Pipeline behaviors (After Super)

All six built-in `PipelineBehavior` implementations are wired in `AfterSuperPrayerTimesViewModel`:

```kotlin
MediatorFactory.create(
    registrars = listOf(PrayerTimesRegistrar(cache)),
    pipelineBehaviors = listOf(
        RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
        LoggingPipelineBehavior(logger = { msg -> logs.add(msg) }, order = -100),
        TimingPipelineBehavior(order = 0) { name, ms -> logs.add("⏱ $name took ${ms}ms") },
        TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
        RequestCounterPipelineBehavior(order = 20),
        ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err -> ... },
    ),
)
```

Logs appear in the screen and in Logcat under the tag `MediatorK`.

## Requirements

- Android Studio Hedgehog or newer
- Android SDK 26+
- Kotlin 2.x (configured in `build.gradle.kts`)
