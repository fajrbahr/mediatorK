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

## Implementing & Registering a handler

Use the `handle` DSL inside a `MediatorRegistrar` to map a request to its implementation:

```kotlin
class AppRegistrar(
    private val db: UserRepository,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.handle<GetUserQuery, User> { request ->
            db.findById(request.id) ?: error("User ${request.id} not found")
        }
        
        registry.handle<CreateOrderCommand, Order> { request ->
            // implementation
            TODO()
        }
        
        registry.handle<DeleteAccountCommand, Unit> { request ->
            // implementation
        }
    }
}
```

The DSL block provides access to the request, and you can access `mediator` and `requestContext` if needed via the block's receiver.

---

## Dispatching a request

```kotlin
val user: User = mediator.send(GetUserQuery("user-1"))
val order: Order = mediator.send(CreateOrderCommand("cart-42"))
mediator.send(DeleteAccountCommand("user-1")) // returns Unit

// trySend — wraps the result in Result instead of throwing
val result: Result<User> = mediator.trySend(GetUserQuery("user-1"))
result.onSuccess { user -> /* handle */ }
result.onFailure { error -> /* handle */ }
```

`send` is a `suspend` function; call it from a coroutine or another `suspend` context.

---

## Rules

| Rule                   | Detail                                                          |
|------------------------|-----------------------------------------------------------------|
| One handler per type   | Registering a second handler silently replaces the first        |
| Missing handler throws | `MissingHandlerException` is thrown if no handler is registered |
| Exactly one response   | Use `Notification` when you need fan-out with no response       |

---

## Next

→ [Notifications](notifications.md): broadcast events to one-or-many handlers
