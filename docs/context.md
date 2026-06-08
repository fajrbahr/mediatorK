# Request Context

`RequestContext` is a **mutable key-value bag** scoped to a single pipeline execution. A fresh instance is created for every `mediator.send(...)` call — values are never shared between concurrent requests, even when the mediator is a singleton.

This mirrors per-request scoping in web frameworks (`HttpContext`, `CoroutineContext`, etc.).

---

## Writing to the context

Pre-processors and pipeline behaviors populate the context:

```kotlin
class TraceIdPreProcessor : RequestPreProcessor {
    override suspend fun process(requestContext: RequestContext, request: Request<*>) {
        requestContext.put("traceId", generateTraceId())
        requestContext.put("userId", resolveCurrentUser())
    }
}
```

---

## Reading from the context

Handlers and post-processors read values by key:

```kotlin
class CreateOrderHandler : RequestHandler<CreateOrderCommand, Order> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): Order {
        val userId = requestContext.getMetaDate<String>("userId")
            ?: error("userId not set in context")
        return orderService.create(userId, request.cartId)
    }
}
```

`getMetaDate<T>(key)` returns `null` if the key is absent or the stored value can't be cast to `T`.

---

## Avoiding key collisions

Keys are plain strings. Use constants or fully-qualified names to avoid collisions between unrelated components:

```kotlin
object ContextKeys {
    const val TRACE_ID = "com.myapp.traceId"
    const val USER_ID  = "com.myapp.userId"
}

// write
requestContext.put(ContextKeys.TRACE_ID, traceId)

// read
val traceId = requestContext.getMetaDate<String>(ContextKeys.TRACE_ID)
```

---

## Next

→ [Exception Handling](exceptions.md)
