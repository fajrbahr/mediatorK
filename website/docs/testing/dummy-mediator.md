---
id: dummy-mediator
title: DummyMediator
sidebar_label: DummyMediator
---

# DummyMediator

`DummyMediator` is a no-op `Mediator` included in `mediatork-test` for use in tests.

- `publish` does nothing; fire and forget, no handlers called.
- `send` returns silently; no exception, no result processing.

No fake class to write, no mocking library needed.

---

## Usage

Use it when a test needs a `Mediator` to satisfy a constructor but never actually calls `send`:

```kotlin
@Test
fun `initial state is empty and not loading`() {
    val vm = OrderViewModel(DummyMediator())
    assertEquals(OrderUiState(), vm.stateFlow.value)
}
```

If your test does call `send` and the result matters — to return a value, simulate a failure, or capture what was
sent — use [`FakeMediator`](fake-mediator.md) instead.

---

## Import

```kotlin
import com.fajrbahr.mediatork.test.DummyMediator
```

---

## Next

→ [FakeMediator](fake-mediator.md)
