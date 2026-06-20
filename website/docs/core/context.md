---
id: context
title: Request Context
sidebar_label: Request Context
---

# Request Context

`RequestContext` is a **mutable key-value bag** scoped to a single pipeline execution. A fresh instance is created for
every `mediator.send(...)` call — values are never shared between concurrent requests, even when the mediator is a
singleton.

:::info Why a fresh context per request, not a shared property?
`MediatorImpl` is a singleton. If `RequestContext` were a class-level property, concurrent `send()` calls — two
ViewModels firing at the same time, for example — would overwrite each other's locale, auth token, or any other bag
value.

Creating it inside `send()` scopes the context to that single pipeline execution, the same way ASP.NET Core scopes
`HttpContext` per HTTP request. Pipeline behaviors (like `LocalePipelineBehavior`) populate it, and handlers consume
it — all within one isolated request lifecycle.
:::

---

## Writing to the context

Pipeline behaviors (typically `Stage.Pre`) populate the context:

```kotlin
class TraceIdBehavior : PipelineBehavior {
    override val stage = Stage.Pre

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        requestContext.put("traceId", generateTraceId())
        requestContext.put("userId", resolveCurrentUser())
        return next(request)
    }
}
```

---

## Reading from the context

Handlers and `Stage.Post` behaviors read values by key:

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

## Typed extension properties

Instead of raw string keys, declare typed extension properties on `RequestContext` for compile-time safety and IDE
autocomplete:

```kotlin
var RequestContext.locale: String
    get() = getMetaDate("locale") ?: "en"
    set(value) { put("locale", value) }

var RequestContext.userId: String?
    get() = getMetaDate("userId")
    set(value) { if (value != null) put("userId", value) }
```

Usage is then clean and type-safe:

```kotlin
// write (in a pre-processor or behavior)
requestContext.locale = "ar"
requestContext.userId = currentUser.id

// read (in a handler)
val lang = requestContext.locale      // "ar"
val uid  = requestContext.userId      // String?
```

---

## Avoiding key collisions

For teams that prefer raw string keys, use constants or fully-qualified names to avoid collisions between unrelated
components:

```kotlin
object ContextKeys {
    const val TRACE_ID = "com.myapp.traceId"
    const val USER_ID  = "com.myapp.userId"
}

// write
requestContext.put(ContextKeys.TRACE_ID, traceId)
```

**Option 2** — read back with a typed call:

```kotlin
val traceId = requestContext.getMetaDate<String>(ContextKeys.TRACE_ID)
```

---

## Next

→ [Exception Handling](exceptions.md)
