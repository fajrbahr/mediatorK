# Pre / Post Processors

Processors are lightweight hooks that run before or after every handler — simpler than pipeline behaviors when you don't need to wrap or short-circuit, just observe or enrich.

---

## Pre-processors

Run **before** the handler. Cannot mutate the response. Throw to abort the pipeline.

Common uses: logging, populating `RequestContext`, auth checks, input sanitization.

```kotlin
class TraceIdPreProcessor : RequestPreProcessor {
    override val order = 0

    override suspend fun process(requestContext: RequestContext, request: Request<*>) {
        requestContext.put("traceId", generateTraceId())
    }
}
```

Multiple pre-processors run in ascending `order`.

---

## Post-processors

Run **after** the handler has returned. For observation only — cannot mutate the response. Throw to signal failure.

Common uses: response logging, cache write-back, metrics, audit trails.

```kotlin
class AuditPostProcessor(private val log: AuditLog) : RequestPostProcessor {
    override val order = 0

    override suspend fun process(
        requestContext: RequestContext,
        request: Request<*>,
        response: Any?,
    ) {
        log.record(request::class.simpleName ?: "Unknown", requestContext.getMetaDate("userId"))
    }
}
```

---

## Registering processors

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    preProcessors  = listOf(TraceIdPreProcessor()),
    postProcessors = listOf(AuditPostProcessor(auditLog)),
)
```

---

## Pre-processor vs Pipeline behavior

| | Pre/Post Processor | Pipeline Behavior |
|---|---|---|
| Can wrap the handler | No | Yes |
| Can short-circuit | Throw only | Yes (return without calling `next`) |
| Can modify response | No | Yes |
| Applies to specific requests | No | Yes (`appliesTo`) |
| Typical use | Enrich context, observe | Retry, cache, auth, transform |

---

## Next

→ [Request Context](context.md)
