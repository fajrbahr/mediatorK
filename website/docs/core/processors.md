---
id: processors
title: Pre / Post Behaviors
sidebar_label: Pre / Post Behaviors
---

# Pre / Post Behaviors

MediatorK uses a **three-stage pipeline** to give you precise control over where a cross-cutting concern runs relative
to the handler and other behaviors. Every [PipelineBehavior](pipeline.md) declares a `stage` property:

| Stage           | Position           | Typical use                                             |
|-----------------|--------------------|---------------------------------------------------------|
| `Stage.Pre`     | Outermost wrappers | Auth token injection, locale setup, trace-id population |
| `Stage.Default` | Middle (default)   | Logging, retry, caching, circuit-breaking, timing       |
| `Stage.Post`    | Innermost wrappers | Metrics emission, audit logging, response observation   |

**Stage always wins over `order`.** Every `Stage.Pre` behavior executes before every `Stage.Default` behavior,
regardless of their `order` values. `order` only controls sequencing *within* a stage.

---

## Pre-stage behaviors

Set `override val stage = Stage.Pre` to run before all `Default`-stage behaviors. Common uses: populate
`RequestContext`, inject auth context, set locale.

```kotlin
import com.fajrbahr.mediatork.api.Stage

class TraceIdBehavior : PipelineBehavior {
    override val stage = Stage.Pre
    override val order = 0  // ordering within Stage.Pre

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        requestContext.put("traceId", generateTraceId())
        return next(request)
    }
}
```

---

## Post-stage behaviors

Set `override val stage = Stage.Post` to run innermost — closest to the handler. They wrap the handler on the way in
and unwind last on the way out. Common uses: emit metrics, write audit log entries.

```kotlin
class AuditBehavior(private val log: AuditLog) : PipelineBehavior {
    override val stage = Stage.Post
    override val order = 0

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val result = next(request)
        val userId = requestContext.getMetaDate<String>("userId")
        log.record(request::class.simpleName ?: "Unknown", userId)
        return result
    }
}
```

---

## Registering stage behaviors

Stage behaviors are registered the same way as any other `PipelineBehavior` — just pass them in the
`pipelineBehaviors` list:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        TraceIdBehavior(),           // Stage.Pre  — runs outermost
        LoggingPipelineBehavior(),   // Stage.Default, order=-100
        RetryPipelineBehavior(),     // Stage.Default, order=0
        AuditBehavior(auditLog),     // Stage.Post — runs innermost
    ),
)
```

MediatorK sorts behaviors by stage first, then by `order` within each stage. Registration order does not matter.

---

## Execution order

For a typical setup with one behavior in each stage:

```
Pre  (TraceIdBehavior) ─────────────────────────────────────────────────────┐
  Default (LoggingBehavior, order=-100) ─────────────────────────────────┐  │
    Default (RetryBehavior, order=0) ─────────────────────────────────┐  │  │
      Post  (AuditBehavior) ─────────────────────────────────────┐    │  │  │
                                                                  │    │  │  │
                                                               Handler │  │  │
                                                                  │    │  │  │
      Post  (AuditBehavior) ◄────────────────────────────────────┘    │  │  │
    Default (RetryBehavior) ◄─────────────────────────────────────────┘  │  │
  Default (LoggingBehavior) ◄────────────────────────────────────────────┘  │
Pre  (TraceIdBehavior) ◄────────────────────────────────────────────────────┘
```

---

## Next

→ [Request Context](context.md)
