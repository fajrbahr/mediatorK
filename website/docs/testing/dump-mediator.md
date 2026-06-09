---
id: dump-mediator
title: DumpMediator
sidebar_label: DumpMediator
---

# DumpMediator

`DumpMediator` is a no-op `Mediator` included in the library for use in tests.

- `publish` does nothing — fire and forget, no handlers called.
- `send` throws `NotImplementedError` by default — override it to return whatever the test needs.

No fake class to write, no mocking library needed.

---

## Usage

Override only the behaviour your test cares about:

```kotlin
val mediator = object : DumpMediator() {
    override suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes {
        @Suppress("UNCHECKED_CAST")
        return OrderResult(orderId = "order-123") as TRes
    }
}

val vm = OrderViewModel(mediator)
```

To simulate a failure, throw from `send`:

```kotlin
val mediator = object : DumpMediator() {
    override suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes =
        throw RuntimeException("Network unavailable")
}
```

To capture what was sent:

```kotlin
val captured = mutableListOf<Any>()

val mediator = object : DumpMediator() {
    override suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes {
        captured += request
        @Suppress("UNCHECKED_CAST")
        return Unit as TRes
    }
}
```

---

## Import

`DumpMediator` lives in the main library — no extra dependency needed:

```kotlin
import com.fajrbahr.mediatork.DumpMediator
```
