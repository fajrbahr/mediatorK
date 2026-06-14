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
class InitialViewModel(
    private val applicationMetadata: ApplicationMetadata,
    private val retrieveAndStoreTogglesUseCase: RetrieveAndStoreTogglesUseCase,
    watchTogglesUseCase: WatchTogglesUseCase,
    private val persistCachedInfoUseCase: PersistCachedInfoUseCase,
    private val fetchActiveUserAndStoreUseCase: FetchActiveUserAndStoreUseCase,
    fetchPreferredLocaleUseCase: FetchPreferredLocaleUseCase,
    fetchVisualThemeUseCase: FetchVisualThemeUseCase,
    private val metricsReporterPort: MetricsReporterPort,
    val runtimeSettings: RuntimeSettings,
    val speedMonitor: SpeedMonitor,
    val cloudPerformanceTracker: PerformanceTraceListener,
    val simpleLoggingTracker: SimpleLoggingTracker,
) : ViewModel()
```

To instantiate this in a test you must stub every one of those twelve parameters — even if the test only touches two of
them. Every new use-case added to the ViewModel breaks every existing test that constructs it.

With MediatorK the constructor collapses to one dependency:

```kotlin
class InitialViewModel(
    private val mediator: Mediator,
) : ViewModel()
```

Every test now starts the same way:

```kotlin
val vm = InitialViewModel(DummyMediator())   // never calls send
val vm = InitialViewModel(FakeMediator())    // register handlers as needed
```

The use-cases, metrics reporters, toggle observers, and performance trackers are all moved into individual
`RequestHandler` implementations. Each handler is tested in isolation. The ViewModel test only verifies how the
ViewModel reacts to success or failure — it never needs to know which use-cases exist.

---

## Installation

```kotlin
dependencies {
    testImplementation("io.github.fajrbahr:mediatork-test:0.6.2")
}
```
