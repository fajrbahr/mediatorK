---
id: the-promise
title: The Promise
sidebar_label: The Promise
---

# The Promise

> **From a ViewModel with 12 constructor parameters — down to one.**

This page shows the single concrete promise MediatorK makes: your ViewModel shrinks to a thin dispatcher, every piece of
logic moves into a focused handler, and your tests need zero mocking libraries.

---

## The Problem — an XXL ViewModel

Real-world ViewModels tend to grow. Each new feature pulls in another dependency until the constructor looks like this:

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

Twelve dependencies. Testing this requires constructing or mocking all twelve, even for a test that only cares about one
use-case.

---

## The Solution — an XXS ViewModel

With MediatorK the ViewModel has exactly one dependency:

```kotlin
class InitialViewModel(
    private val mediator: Mediator,
) : ViewModel()
```

Every action becomes a `mediator.send(...)` call. The ViewModel no longer knows which use-case, repository, or data
source handles the request — it just dispatches.

Next, see how this maps onto [Vertical Slice Architecture →](vertical-slice.md)
