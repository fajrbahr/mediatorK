---
id: dsl-builder
title: The mediatorK DSL
sidebar_label: The mediatorK DSL
---

# The `mediatorK` DSL

`mediatorK { }` is the smoothest way to build a mediator — one expressive block, no registrar or handler
classes required. It is sugar over [`MediatorFactory.create`](factory): everything the factory accepts is
available in the block, plus inline lambda registration.

```kotlin
val mediator = mediatorK {
    handle<CreateOrderCommand, Order> { request ->
        val order = db.save(Order(request.id, request.amount))
        publish(OrderCreatedEvent(order.id))
        order
    }

    on<OrderCreatedEvent> { event -> emailService.send(event.orderId) }
}

val order = mediator.send(CreateOrderCommand("ORD-1", 150.0))
```

---

## Lambda handlers

`handle<TRequest, TResult>` registers a block as the single handler for a request type:

```kotlin
handle<GetTodoQuery, Todo?> { request -> db.find(request.id) }
```

The lambda runs with a `HandlerScope` receiver, which **is** the mediator (by delegation) and also exposes
the per-request context:

```kotlin
handle<CreateOrderCommand, Order> { request ->
    val traceId: String? = context.getMetadata("traceId")   // RequestContext
    publish(OrderCreatedEvent(request.id))                  // Mediator, directly
    send(ReserveStockCommand(request.id))                   // nested sends too
    db.save(Order(request.id, request.amount))
}
```

Like `HandlerRegistry.register`, registering a second lambda for the same request type replaces the first.

---

## Lambda notification handlers

`on<T>` registers a lambda for a notification type. Multiple handlers per type are allowed; the optional
`order` parameter controls their relative execution order (lower runs first):

```kotlin
on<OrderCreatedEvent> { event -> emailService.send(event.orderId) }
on<OrderCreatedEvent>(order = 10) { event -> analytics.track(event) }
```

---

## Lambda validators

`validate<TRequest>` registers a validator that runs before the handler:

```kotlin
validate<CreateOrderCommand> { request ->
    rules<String> {
        check(request.amount > 0) { "Amount must be positive" }
        check(request.id.isNotBlank()) { "Order id required" }
    }
}
```

---

## Lambda stream handlers

`handleStream<TRequest, T>` registers a cold-`Flow` handler for a [stream request](stream):

```kotlin
handleStream<WatchOrdersQuery, Order> { request -> db.observeOrders(request.filter) }
```

---

## Behaviors and configuration

All `MediatorFactory.create` parameters are available in the block:

```kotlin
val mediator = mediatorK {
    behaviors(
        LoggingPipelineBehavior(),
        TimeoutPipelineBehavior(timeoutMillis = 5_000),
    )
    streamBehaviors(StreamLoggingBehavior())

    notificationPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()
    verifyHandlers = false
}
```

---

## Mixing with registrars

Both standalone DSL registrations and existing registrars (including KSP-generated ones) mix freely in the same block:

```kotlin
val mediator = mediatorK {
    registrars(OrderRegistrar(db), GeneratedMediatorRegistrar())

    handle<CreateOrderCommand, Order> { request -> db.save(Order(request.id, request.amount)) }
    handle<PingQuery, String> { "pong" }
}
```

The same lambda extensions (`handle`, `on`, `validate`, `handleStream`) are also available on
`HandlerRegistry`, so they work inside a classic `MediatorRegistrar.register` implementation too.

---

**See also:** [MediatorFactory](factory) for the parameter-by-parameter reference.
