---
id: factory
title: MediatorFactory
sidebar_label: MediatorFactory
---

# MediatorFactory

`MediatorFactory.create` is the single entry point for wiring everything together: registrars, pipeline behaviors,
validators, and the notification strategy all go here.

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
val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(),
        OrderRegistrar(),
        OrderNotificationRegistrar(),
    ),
    pipelineBehaviors = listOf(
        TraceIdBehavior(),                        // Stage.Pre  — outermost
        LoggingPipelineBehavior(),                // Stage.Default, order=-100
        RetryPipelineBehavior(maxRetries = 2),    // Stage.Default, order=0
        CircuitBreakerPipelineBehavior(),         // Stage.Default, order=0
        TimingPipelineBehavior { name, ms ->      // Stage.Default, order=0
            metrics.record(name, ms)
        },
        AuditBehavior(auditLog),                  // Stage.Post — innermost
    ),
    notificationPublisher = ParallelNotificationPublisher(),
    missingNotificationHandler = ThrowMissingNotificationHandler(),
)
```

---

## Parameters

| Parameter                    | Type                                  | Default                             | Description                                                                                                                                                               |
|------------------------------|---------------------------------------|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `registrars`                 | `List<MediatorRegistrar>`             | `emptyList()`                       | Contribute handlers to the registry at startup                                                                                                                            |
| `pipelineBehaviors`          | `List<PipelineBehavior>`              | `emptyList()`                       | Cross-cutting decorators; grouped by `Stage` then sorted by `order` within each stage                                                                                     |
| `streamPipelineBehaviors`    | `List<StreamPipelineBehavior>`        | `emptyList()`                       | Cross-cutting decorators for `StreamRequest` dispatches; sorted by `order`                                                                                                |
| `notificationPublisher`      | `NotificationPublishStrategy`         | `ParallelNotificationPublisher()`   | Strategy for delivering notifications to handlers                                                                                                                         |
| `verifyHandlers`             | `Boolean`                             | `true`                              | When `true`, logs a warning for every request type with no handler after all registrars have run                                                                          |
| `missingNotificationHandler` | `NotificationHandler<Notification>`   | `ThrowMissingNotificationHandler()` | Called when a notification is published with no registered handlers. Built-in: `ThrowMissingNotificationHandler`, `SilentMissingNotificationHandler`, or provide your own |
| `missingRequestHandler`      | `RequestHandler<Request<Any?>, Any?>` | `ThrowMissingRequestHandler()`      | Called when `send()` is called for a request type with no registered handler. Use `SilentMissingRequestHandler` to return a default instead of throwing.                  |

---

## Pipeline execution order

For every `mediator.send(request)` call, the execution flows like this:

```
Stage.Pre behaviors    (ascending order, outermost)
  └─ Stage.Default behaviors  (ascending order)
       └─ Stage.Post behaviors  (ascending order, innermost)
            └─ Handler
       └─ Stage.Post behaviors  (unwinding)
  └─ Stage.Default behaviors  (unwinding)
Stage.Pre behaviors    (unwinding)
```

**Stage always beats `order`**: every `Stage.Pre` behavior wraps every `Stage.Default` behavior, regardless of their
`order` values. `order` only controls sequencing *within* a stage.

`ValidationBehavior` runs at `order = -50` in `Stage.Default` by default, so it executes before logging (`-100`?),
caching, and retry behaviors at `order = 0`.

---

## Notification publishers

| Publisher                                  | Behaviour                                                    |
|--------------------------------------------|--------------------------------------------------------------|
| `ParallelNotificationPublisher`            | All handlers run concurrently *(default)*                    |
| `SequentialNotificationPublisher`          | Handlers run one-by-one; stops on first error                |
| `ContinueOnExceptionNotificationPublisher` | All handlers run; errors collected into `AggregateException` |
| `FireAndForgetNotificationPublisher`       | Returns immediately; handlers run in the background          |

---

## Verifier limitations

`verifyHandlers = true` enables startup verification, but it has known limitations:

:::warning
**The verifier can only see what was registered; it cannot detect what was never registered.**

A missing handler is invisible at startup. It only surfaces at runtime when `send()` or
`publish()` is called for a type with no handler.
:::

| What the verifier can detect                                           | What it cannot detect                                         |
|------------------------------------------------------------------------|---------------------------------------------------------------|
| Duplicate request handler registrations (last one wins silently)       | A `Request` type that was never registered at all             |
| Notification handlers registered for a type with an empty handler list | A `NotificationHandler` whose notification is never published |
| Misconfigured registrars that run but register nothing                 | Unsatisfied constructor dependencies inside a handler         |

**Unsatisfied dependencies** (e.g. a null `Repository` injected into a handler) are outside
MediatorK's scope; that is your DI container's responsibility (Koin, Hilt, etc.).

Use `MediatorTestUtils.assertAllHandlersRegistered(registrars)` from the `mediatork-test` module to catch missing
registrations in tests rather than at runtime.

---

## Next

→ [Kotlin JVM](../integration/jvm.md)
