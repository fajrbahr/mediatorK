# Plan: Native Notification Subscription (Reactive Observation)

## Motivation

Today the only way to react to a `Notification` is to register a `NotificationHandler`
class at mediator-build time. That works for backend-style side effects (logging,
sync, email), but it is the wrong shape for UI layers: a ViewModel wants a
lifecycle-scoped `Flow` it can `collect`, `map`, and `stateIn` — collectors that
attach and detach freely — not a globally registered handler class with no
unregister API.

This feature adds a **native, first-class subscription surface** to the mediator:

```kotlin
// ViewModel / Compose / any consumer
mediator.subscribe<TodoAddedNotification>()
    .map { it.todo }
    .onEach { refreshList(it) }
    .launchIn(viewModelScope)
```

Handlers stay the mechanism for *doing* things when an event happens.
Subscriptions are the mechanism for *observing* that it happened.

## Goals

- `mediator.subscribe<T>(): Flow<T>` — hot, multicast observation of published notifications.
- `mediator.notifications: Flow<Notification>` — the raw firehose (all types).
- Polymorphic matching: subscribing to a supertype receives all subtypes
  (unlike handler resolution, which is exact-class).
- Zero impact on existing handler dispatch, publish strategies, and pipeline behaviors.
- Configurable buffering/replay at mediator creation.
- Ship `FlowNotificationHandler<T>` as a built-in for per-type replay/buffer control.
- All KMP targets (JVM, Android, iOS, macOS, JS, wasmJs) — no platform-specific APIs.

## Non-goals

- Polymorphic **handler** resolution (separate future work).
- Per-subscription buffer configuration (use `FlowNotificationHandler` for that).
- Unregistering `NotificationHandler`s (subscriptions make it unnecessary for UI).

## API design

### New capability interface: `Subscriber`

`notification/Subscriber.kt` — pairs with the existing `Publisher`:

```kotlin
interface Subscriber {
    /** Hot stream of every published notification (the firehose). */
    val notifications: Flow<Notification>

    /** Hot stream of published notifications that are instances of [type]. */
    fun <T : Notification> subscribe(type: KClass<T>): Flow<T>
}

/** Reified convenience: mediator.subscribe<OrderCreated>() */
inline fun <reified T : Notification> Subscriber.subscribe(): Flow<T> = subscribe(T::class)
```

### `Mediator` extends it (BREAKING)

```kotlin
interface Mediator : Sender, IStreamRequest, Publisher, Subscriber
```

### Configuration: `SubscriptionConfig`

`notification/SubscriptionConfig.kt`, new optional parameter on both
`MediatorFactory.create` overloads:

```kotlin
data class SubscriptionConfig(
    val replay: Int = 0,                 // 0 = one-shot events; 1 = sticky last event
    val extraBufferCapacity: Int = 64,
    val onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
)
```

Defaults are chosen so `publish` **never suspends on a slow collector** and
one-shot UI events don't re-fire for late collectors.

### Built-in bridge: `FlowNotificationHandler<T>`

`notification/FlowNotificationHandler.kt` — a `NotificationHandler` that exposes
what it receives as a `SharedFlow`. For users who want per-type replay/buffer
tuning (e.g. `replay = 1` sticky state) while the global bus stays at `replay = 0`.

## Semantics (the decisions that matter)

| Decision | Choice | Why |
|---|---|---|
| Producer mechanism | Single internal `MutableSharedFlow<Notification>` bus inside `MediatorImpl` | No mutable map of per-type flows → no thread-safety hazard in KMP common code |
| Type matching | `bus.filter { type.isInstance(it) }` | Polymorphic for free; avoids the exact-`KClass` limitation of handler resolution |
| Emit order | Bus emission happens **before** handler dispatch | Subscribers are passive taps; they see every published notification even if a handler throws |
| Blocking | `emit` with `DROP_OLDEST` + buffer never suspends | A slow UI collector must never stall `publish` |
| Missing-handler interplay | `missingNotificationHandler` is invoked only when there are **no handlers AND no active subscribers** (`subscriptionCount == 0`) | A notification observed only by UI is not "unhandled". Note: the check is global, not per-type — documented |
| Strategy overload | `publish(notification, strategy)` also feeds the bus | Subscribers see all notifications regardless of delivery strategy |
| Late subscribers | Miss earlier events unless `replay > 0` | Standard hot-flow semantics; `replay = 1` opts into stickiness |

## Breaking changes

1. **`Mediator` gains a supertype `Subscriber`** — any code implementing
   `Mediator` directly must add `notifications` + `subscribe(type)`.
   Affected in-repo: `StubMediator`, `FakeMediator`, `DummyMediator`,
   `MediatorSpy` (mediatork-test), one anonymous impl in `HandlerRegistryTest`.
2. **`publish` with no handlers no longer invokes the missing-notification
   handler when at least one subscriber is actively collecting.** Previously it
   always threw (default `ThrowMissingNotificationHandler`).
3. `MediatorImpl` constructor gains `subscriptionConfig` (internal class — not
   public API).

## Files

### New
- `mediatork/src/commonMain/kotlin/com/fajrbahr/mediatork/notification/Subscriber.kt`
- `mediatork/src/commonMain/kotlin/com/fajrbahr/mediatork/notification/SubscriptionConfig.kt`
- `mediatork/src/commonMain/kotlin/com/fajrbahr/mediatork/notification/FlowNotificationHandler.kt`
- `mediatork/src/commonTest/kotlin/com/fajrbahr/mediatork/SubscribeTest.kt`
- `mediatork/src/commonTest/kotlin/com/fajrbahr/mediatork/FlowNotificationHandlerTest.kt`

### Modified
- `api/Mediator.kt` — extend `Subscriber`
- `MediatorImpl.kt` — internal bus, `subscribe`/`notifications`, publish changes
- `MediatorModule.kt` — `subscriptionConfig` param on both `create` overloads
- `mediatork-test`: `StubMediator` (real bus: `publish` feeds `subscribe`),
  `FakeMediator` + `DummyMediator` (empty flows), `MediatorSpy` (delegates + records subscribed types)
- `HandlerRegistryTest.kt` — anonymous `Mediator` impl
- `StubMediatorTest.kt` — coverage for stub subscription
- `CHANGELOG.md`, `readme.md`

## Test plan

- subscriber receives published notification (handlers registered)
- multiple subscribers all receive the same notification
- supertype subscription receives subtype notifications
- late subscriber misses earlier events (default config)
- `replay = 1` delivers the last event to a late subscriber
- no handlers + active subscriber → no `MissingNotificationHandlerException`
- no handlers + no subscribers → missing handler still invoked (existing behavior kept)
- handlers still run alongside subscribers
- subscriber receives the event even when a handler throws
- `publish(notification, strategy)` overload feeds subscribers
- `notifications` firehose receives all types
- cancelled subscriber stops receiving
- `FlowNotificationHandler`: forwards events, supports replay, respects `order` default
- `StubMediator.publish` feeds `StubMediator.subscribe`

Run: `./gradlew :mediatork:jvmTest :mediatork-test:jvmTest` (JVM as the fast
signal; common source sets keep other targets equivalent).

## Out of scope / future ideas

- Polymorphic handler resolution (`NotificationHandler<Parent>` receiving subtypes).
- Sequential per-handler mailbox delivery strategy (actor-style stateful handlers).
- Auto-populated correlation id in `RequestContext`.
