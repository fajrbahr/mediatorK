---
id: handler-validation
title: Handler Validation
sidebar_label: Handler Validation
---

# Handler Validation

MediatorK ships a companion test utilities artifact, `mediatork-test`, that helps you catch missing handler
registrations at test time instead of as a runtime crash.

---

## assertAllHandlersRegistered

`MediatorTestUtils.assertAllHandlersRegistered` scans the classpath for every request type and asserts that each one has a handler wired up via your registrars.

If a handler exists but was forgotten in a `MediatorRegistrar`, the test fails immediately with a clear message instead
of crashing at runtime when the request is first dispatched.

### Basic usage

Scan the entire classpath; simplest setup, works for most projects:

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

If third-party libraries on your classpath also define request types, scanning everything may produce false
positives. Pass `packages` to restrict the scan:

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

| Parameter    | Type                      | Default       | Description                                               |
|--------------|---------------------------|---------------|-----------------------------------------------------------|
| `registrars` | `List<MediatorRegistrar>` | required      | The same registrars you pass to `MediatorFactory.create`. |
| `packages`   | `List<String>`            | `emptyList()` | Packages to scan. Empty list scans the entire classpath.  |

---

## Next

→ [Troubleshooting & FAQ](../troubleshooting.md)
