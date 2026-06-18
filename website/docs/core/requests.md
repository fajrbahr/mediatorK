---
id: requests
title: Requests & Handlers
sidebar_label: Requests & Handlers
---

# Requests & Handlers

A **request** is a message that expects exactly one handler and one response. It models both queries (read data) and
commands (perform an action, optionally return a result).

---

## Defining a request

Implement `Request<TResponse>` to declare what a request returns:

```kotlin
// Query — returns data
data class GetUserQuery(val id: String) : Request<User>

// Command with result
data class CreateOrderCommand(val cartId: String) : Request<Order>

// Command with no result — use Request.Unit instead of Request<Unit>
data class DeleteAccountCommand(val userId: String) : Request.Unit
```

`Request.Unit` is a built-in nested interface that extends `Request<Unit>`, giving commands with no return value a
cleaner declaration.

---

## Implementing a handler

```kotlin
class GetUserHandler(private val db: UserRepository) : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetUserQuery,
    ): User = db.findById(request.id) ?: error("User ${request.id} not found")
}
```

The `mediator` parameter lets a handler dispatch secondary requests or publish notifications without creating direct
dependencies on other handlers.

---

## Registering a handler

Use `HandlerRegistry` inside a `MediatorRegistrar`:

```kotlin
class AppRegistrar(
    private val db: UserRepository,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetUserHandler(db)
        registry register CreateOrderHandler(db)
        registry register DeleteAccountHandler(db)
    }
}
```

The `registry register handler` infix call is the standard way to register a handler. The `+handler` operator inside a
`scope { }` block is a shorthand alias for the same thing.

---

## Dispatching a request

```kotlin
val user: User = mediator.send(GetUserQuery("user-1"))
val order: Order = mediator.send(CreateOrderCommand("cart-42"))
mediator.send(DeleteAccountCommand("user-1")) // returns Unit
```

`send` is a `suspend` function — call it from a coroutine or another `suspend` context.

---

## Streaming requests

Use `StreamRequest<T>` when the response is a sequence of items — large result sets, live feeds, cursor-based exports, or anything better consumed incrementally rather than loaded into a `List` all at once.

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

`stream()` is **non-suspend** — it resolves the handler and returns the cold `Flow` immediately. The handler's work begins only when the caller collects. Each collection creates a fresh `RequestContext`.

Dispatching with no registered stream handler throws `MissingStreamHandlerException`.

---

## Fallback chain

Register multiple handlers for the same request type with the `otherwise` infix operator. Handlers are tried in order;
the first one that succeeds wins.

```kotlin
registry register (
    CreateOrderHandler(liveApi)
        otherwise CreateOrderHandler(stagingApi)
        otherwise CreateOrderStubHandler()
)
```

→ See [Fallback Chains](fallback.md) for the full guide.

---

## Rules

| Rule                   | Detail                                                          |
|------------------------|-----------------------------------------------------------------|
| One handler per type   | Registering a second handler silently replaces the first        |
| Missing handler throws | `MissingHandlerException` is thrown if no handler is registered |
| Exactly one response   | Use `Notification` when you need fan-out with no response       |

---

## Next

→ [Notifications](notifications.md) — broadcast events to zero-or-many handlers  
→ [Validation](validation.md) — REQUEST, DOMAIN, and PERSISTENCE scopes
