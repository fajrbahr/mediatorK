---
id: stream-behaviors
title: Stream Pipeline Behaviors
sidebar_label: Stream Behaviors
---

# Stream Pipeline Behaviors

A **stream pipeline behavior** wraps every `mediator.stream(...)` dispatch in a decorator chain, the same way
[Pipeline Behaviors](pipeline.md) wrap `send()`. The difference: stream behaviors compose **cold `Flow`s** instead of
awaiting a single result, so cross-cutting concerns like logging, throttling, and tracing become `Flow` operator
chains.

```
stream(request)
  └─ StreamBehavior 1 (order=-100, outermost)
       └─ StreamBehavior 2 (order=0)
            └─ StreamRequestHandler → Flow<T>
```

Lower `order` = outermost wrapper. Behaviors are sorted by `order` at dispatch time; ties break in registration order.

---

## How it differs from `PipelineBehavior`

| Aspect          | `PipelineBehavior`                            | `StreamPipelineBehavior`                          |
|-----------------|-----------------------------------------------|---------------------------------------------------|
| Wraps           | `Request<TResult>` via `send()`               | `StreamRequest<T>` via `stream()`                 |
| `process`       | `suspend`, returns `TResult`                  | **not** suspend, returns `Flow<T>` immediately    |
| `next` delegate | `RequestHandlerDelegate` (suspend)            | `StreamHandlerDelegate` (plain function)          |
| Stages          | `Stage.Pre` / `Default` / `Post`              | No stages — `order` alone controls position       |
| Work happens    | Inline, when the behavior runs                | Lazily, when the caller **collects** the flow     |

The interface:

```kotlin
interface StreamPipelineBehavior {
    val order: Int get() = 0
    val isEnabled: Boolean get() = true
    fun appliesTo(request: StreamRequest<*>): Boolean = true

    fun <TRequest : StreamRequest<T>, T> process(
        requestContext: RequestContext,
        next: StreamHandlerDelegate<TRequest, T>,
        request: TRequest,
    ): Flow<T>
}
```

Call `next(request)` to obtain the downstream flow, transform it with `Flow` operators, and return it. Returning a
different flow without calling `next` short-circuits the remaining behaviors and the handler.

:::info `process` is intentionally not suspend
`stream()` resolves the handler and returns the cold `Flow` immediately — nothing suspends until the caller collects.
Your behavior must follow the same contract: build the flow synchronously, do the actual work inside operators like
`onStart`, `onEach`, and `onCompletion`.
:::

---

## Example 1 — Logging stream behavior

Logs when collection starts, how many items were emitted, and whether the stream completed or failed:

```kotlin
class LoggingStreamBehavior(
    private val log: (String) -> Unit = ::println,
) : StreamPipelineBehavior {
    override val order = -100   // outermost — sees the whole stream lifecycle

    override fun <TRequest : StreamRequest<T>, T> process(
        requestContext: RequestContext,
        next: StreamHandlerDelegate<TRequest, T>,
        request: TRequest,
    ): Flow<T> {
        val name = request::class.simpleName
        var emitted = 0
        return next(request)
            .onStart {
                emitted = 0
                log("→ $name collection started")
            }
            .onEach { emitted++ }
            .onCompletion { error ->
                when (error) {
                    null -> log("← $name completed after $emitted item(s)")
                    else -> log("✕ $name failed after $emitted item(s): ${error.message}")
                }
            }
    }
}
```

`onStart` fires on every collection (cold flows can be collected more than once), so the counter resets each time.

---

## Example 2 — Throttling / rate-limiting stream behavior

High-volume streams — price tickers, sensor feeds, live search — can overwhelm collectors and UIs. A throttling
behavior slows the stream down without touching the handler:

```kotlin
class ThrottlingStreamBehavior(
    private val minInterval: Duration = 100.milliseconds,
) : StreamPipelineBehavior {
    override val order = 0

    // Only throttle streams that opt in via a marker interface
    override fun appliesTo(request: StreamRequest<*>): Boolean =
        request is HighVolumeStream

    override fun <TRequest : StreamRequest<T>, T> process(
        requestContext: RequestContext,
        next: StreamHandlerDelegate<TRequest, T>,
        request: TRequest,
    ): Flow<T> = next(request).onEach { delay(minInterval) }
}
```

`onEach { delay(...) }` applies **backpressure**: every item is delivered, at most one per interval — the producer
suspends between emissions. For UI updates where only the freshest value matters, drop stale items instead:

```kotlin
// Latest-value rate limiting — stale items are dropped, never delivered late
override fun <TRequest : StreamRequest<T>, T> process(
    requestContext: RequestContext,
    next: StreamHandlerDelegate<TRequest, T>,
    request: TRequest,
): Flow<T> = next(request).conflate().onEach { delay(minInterval) }
```

| Operator chain              | Semantics                                                       |
|-----------------------------|------------------------------------------------------------------|
| `onEach { delay(t) }`       | Every item delivered; producer suspends — true backpressure     |
| `conflate().onEach { … }`   | At most one item per interval; always the most recent value     |
| `sample(t)`                 | Emits the latest value every `t`; requires `@OptIn(FlowPreview::class)` |

---

## Registering stream behaviors

Pass them via the `streamPipelineBehaviors` parameter of `MediatorFactory.create` — a separate list from regular
`pipelineBehaviors`:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(LoggingBehavior()),               // wraps send()
    streamPipelineBehaviors = listOf(
        LoggingStreamBehavior(),        // order -100, outermost
        ThrottlingStreamBehavior(),     // order 0
    ),
)
```

Regular `PipelineBehavior`s never see stream requests, and `StreamPipelineBehavior`s never see regular requests — the
two chains are fully independent.

---

## Next

→ [Fallback Chains](fallback.md)
