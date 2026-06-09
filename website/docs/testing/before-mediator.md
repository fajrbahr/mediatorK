---
id: before-mediator
title: Life Before MediatorK
sidebar_label: Life Before MediatorK
---

# Life Before MediatorK

## Testing without a mocking library

The biggest testing win from MediatorK is what it does to your ViewModel constructor.

A typical ViewModel that manages its own dependencies directly ends up looking like this:

```kotlin
class SplashViewModel(
    private val appInfo: AppInfo,
    private val fetchAndCacheFeaturesFlagsUseCase: FetchAndCacheFeaturesFlagsUseCase,
    observeFeatureFlagsUseCase: ObserveFeatureFlagsUseCase,
    private val saveCacheDataUseCase: SaveCacheDataUseCase,
    private val getCurrentUserAndCacheUseCase: GetCurrentUserAndCacheUseCase,
    getPrefLanguageUseCase: GetPrefLanguageUseCase,
    getThemeConfigUseCase: GetThemeConfigUseCase,
    private val analyticsTrackerPort: AnalyticsTrackerPort,
    val environmentConfiguration: EnvironmentConfig,
    val performanceTracker: PerformanceTracker,
    val firebasePerformanceTracker: TraceListener,
    val basicLoggerTracker: BasicLoggerTracker,
) : ViewModel()
```

To instantiate this in a test you must stub every one of those twelve parameters — even if the test only touches two of them. Every new use-case added to the ViewModel breaks every existing test that constructs it.

With MediatorK the constructor collapses to one dependency:

```kotlin
class SplashViewModel(
    private val mediator: Mediator,
) : ViewModel()
```

Every test now starts the same way:

```kotlin
val vm = SplashViewModel(DummyMediator())   // never calls send
val vm = SplashViewModel(FakeMediator())    // register handlers as needed
```

The use-cases, analytics trackers, feature-flag observers, and performance trackers are all moved into individual `RequestHandler` implementations. Each handler is tested in isolation. The ViewModel test only verifies how the ViewModel reacts to success or failure — it never needs to know which use-cases exist.

---

## Installation

```kotlin
dependencies {
    testImplementation("io.github.fajrbahr:mediatork-test:0.6.0")
}
```
