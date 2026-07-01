---
id: stream
title: Stream Handler
sidebar_label: Stream Handler
---

# Stream Handler

Use `StreamRequest<T>` when the response is a sequence of items: large result sets, live feeds, cursor-based exports,
or anything better consumed incrementally rather than loaded into a `List` all at once.

```kotlin
// 1. Define — implements StreamRequest instead of Request
data class StreamInvoicesQuery(val status: InvoiceStatus? = null) : StreamRequest<Invoice>

// 2. Handle — returns a cold Flow<T>, not suspend
class StreamInvoicesHandler(private val repo: InvoiceRepository)
    : StreamRequestHandler<StreamInvoicesQuery, Invoice> {

    override fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: StreamInvoicesQuery,
    ): Flow<Invoice> = repo.all().asFlow().let { flow ->
        if (request.status != null) flow.filter { it.status == request.status } else flow
    }
}

// 3. Register — use registerStream(), not the regular register()
class InvoiceRegistrar(private val repo: InvoiceRepository) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateInvoiceHandler(repo)
            registerStream(StreamInvoicesHandler(repo))  // <-- registerStream for stream handlers
        }
    }
}

// 4. Dispatch — mediator.stream() returns a cold Flow
mediator.stream(StreamInvoicesQuery(status = InvoiceStatus.APPROVED))
    .collect { invoice -> process(invoice) }

// Or collect to a list in tests
val all = mediator.stream(StreamInvoicesQuery()).toList()
```

`stream()` is **non-suspend**; it resolves the handler and returns the cold `Flow` immediately. The handler's work
begins only when the caller collects. Each collection creates a fresh `RequestContext`.

Dispatching with no registered stream handler throws `MissingStreamHandlerException`.

**See also:** [Stream Pipeline Behaviors](stream-behaviors.md) — wrap stream handlers with logging, throttling, and other cross-cutting concerns.

---

## Next

→ [Stream Pipeline Behaviors](stream-behaviors.md)
