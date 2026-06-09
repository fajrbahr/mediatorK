---
id: factory
title: MediatorFactory
sidebar_label: MediatorFactory
---

# MediatorFactory

`MediatorFactory.create` is the single entry point for wiring everything together — registrars, pipeline behaviors, processors, validators, and the notification strategy all go here.

---

## Basic wiring

The simplest setup: one registrar, no pipeline.

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
)
```

---

## Full wiring

A production setup with all extension points populated:

```kotlin
val validators: List<RequestValidator<*>> = listOf(
    FetchBookingsByEmailQueryValidator(),
    GetOrderQueryValidator(),
)

val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(),
        OrderRegistrar(),
        OrderNotificationRegistrar(),
        FetchUserHandlerRegistrar(),
        GetOrderRegistrar(),
    ),
    pipelineBehaviors = listOf(
        LoggingBehavior(),
        MeasurePipelineBehaviour(),
        RetryPipelineBehavior(maxRetries = 2),
        TracingPipelineBehavior(),
        ValidationBehavior(validators),
    ),
    preProcessors = listOf(
        AuthPreProcessor(),
        LocalePreProcessor(),
    ),
    postProcessors = listOf(
        MetricsPostProcessor(),
    ),
    notificationPublisher = ParallelNotificationPublisher(),
)
```

---

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `registrars` | `List<MediatorRegistrar>` | `emptyList()` | Contribute handlers to the registry at startup |
| `pipelineBehaviors` | `List<PipelineBehavior>` | `emptyList()` | Cross-cutting decorators; sorted by `order` at dispatch time |
| `preProcessors` | `List<RequestPreProcessor>` | `emptyList()` | Run before the handler; sorted by `order` |
| `postProcessors` | `List<RequestPostProcessor>` | `emptyList()` | Run after the handler; sorted by `order` |
| `notificationPublisher` | `NotificationPublisher` | `ParallelNotificationPublisher()` | Strategy for delivering notifications to handlers |

---

## Pipeline execution order

For every `mediator.send(request)` call, the execution flows like this:

```
PreProcessors  (ascending order)
  └─ PipelineBehaviors  (ascending order, outermost first)
       └─ Handler
  └─ PipelineBehaviors  (unwinding, innermost first)
PostProcessors (ascending order)
```

`ValidationBehavior` runs at `order = -50` by default, so it executes before logging, tracing, and retry behaviors that typically sit at `order = 0` or above.

---

## Notification publishers

| Publisher | Behaviour |
|---|---|
| `ParallelNotificationPublisher` | All handlers run concurrently *(default)* |
| `SequentialNotificationPublisher` | Handlers run one-by-one; stops on first error |
| `ContinueOnExceptionNotificationPublisher` | All handlers run; errors collected into `AggregateException` |
| `FireAndForgetNotificationPublisher` | Returns immediately; handlers run in the background |

---

## Next

→ [Kotlin JVM](../integration/jvm.md)
