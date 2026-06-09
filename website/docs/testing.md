---
id: testing
title: Testing
sidebar_label: Testing
---

# Testing

MediatorK ships a companion test utilities artifact — `mediatork-test` — that helps you catch missing handler registrations at test time instead of as a runtime crash.

---

## Installation

Add `mediatork-test` as a **test** dependency:

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    testImplementation("io.github.fajrbahr:mediatork-test:0.1.5.6")
}
```

**Gradle (Groovy)**
```groovy
dependencies {
    testImplementation 'io.github.fajrbahr:mediatork-test:0.1.5.6'
}
```

:::info
`mediatork-test` is a JVM-only artifact. It works for Android (unit tests), JVM, and KMP projects that have a JVM or Android test source set.
:::

---

## assertAllHandlersRegistered

`MediatorTestUtils.assertAllHandlersRegistered` scans the classpath for every concrete `RequestHandler` implementation and asserts that each one is wired up via your registrars.

If a handler exists but was forgotten in a `MediatorRegistrar`, the test fails immediately with a clear message instead of crashing at runtime when the request is first dispatched.

### Basic usage

Scan the entire classpath — simplest setup, works for most projects:

```kotlin
import com.fajrbahr.mediatork.test.MediatorTestUtils
import kotlin.test.Test

class HandlerCoverageTest {

    @Test
    fun `all handlers are registered`() {
        MediatorTestUtils.assertAllHandlersRegistered(
            registrars = listOf(
                OrderRegistrar(),
                UserRegistrar(),
            ),
        )
    }
}
```

### Narrow the scan to specific packages

If third-party libraries on your classpath also implement `RequestHandler`, scanning everything may produce false positives. Pass `packages` to restrict the scan:

```kotlin
MediatorTestUtils.assertAllHandlersRegistered(
    registrars = listOf(
        OrderRegistrar(),
        UserRegistrar(),
    ),
    packages = listOf("com.myapp.order", "com.myapp.user"),
)
```

---

## Failure message

When a handler is missing the test fails with a descriptive message:

```
Unregistered handlers found:
  - CreateOrderHandler handles CreateOrderCommand — not registered
  - FetchUserHandler handles FetchUserQuery — not registered
```

---

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `registrars` | `List<MediatorRegistrar>` | — | The same registrars you pass to `MediatorFactory.create`. |
| `packages` | `List<String>` | `emptyList()` | Packages to scan. Empty list scans the entire classpath. |
