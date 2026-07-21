---
id: troubleshooting
title: Troubleshooting & FAQ
sidebar_label: Troubleshooting
---

# Troubleshooting & FAQ

The most common MediatorK issues, with the exact symptom, the cause, and the fix.

---

## "No handler registered for 'X'"

**Symptom** — `send()` throws `MissingHandlerException`:

```
MissingHandlerException: No handler registered for 'DeleteUserCommand'. Registered: GetUserQuery, CreateOrderCommand
```

The message lists every request type that *does* have a handler — if the list looks right except for your type, the
handler was never registered; if the list is empty, no registrar ran at all.

**Causes & fixes**

1. **The registrar was never passed to `MediatorFactory.create`.** Registering handlers inside a
   `MediatorRegistrar` does nothing unless that registrar is in the `registrars` list:

   ```kotlin
   val mediator = MediatorFactory.create(
       registrars = listOf(UserRegistrar(), OrderRegistrar()),  // <-- every registrar must be here
   )
   ```

2. **The handler is missing from the registrar.** Each request type needs an explicit registration:

   ```kotlin
   override fun register(registry: HandlerRegistry) {
       registry.scope {
           +GetUserHandler(repo)
           +DeleteUserHandler(repo)   // <-- was this line forgotten?
       }
   }
   ```

3. **You rely on KSP-generated registration but the KSP module isn't applied** to the Gradle module that contains
   the handler — see [KSP-generated registrar is missing handlers](#ksp-generated-registrar-is-missing-handlers).

The same applies to streams: `stream()` with no registered handler throws `MissingStreamHandlerException`. Stream
handlers must be registered with `registerStream(...)` (or `+handler` inside `scope { }`), not `register(...)`.

:::info Catch misconfiguration at startup
`MediatorFactory.create(verifyHandlers = true)` (the default) prints
`MEDIATOR WARNING: No handler registered for '<Type>'` at startup for registered types with missing handlers, and
a [`testMediator`](testing/test-mediator.md) integration test turns wiring gaps into failing tests.
:::

---

## My notification handler never fires

**Symptom** — `publish(SomeNotification())` completes (or throws), but your handler's `handle()` is never reached.

**Causes & fixes**

1. **The handler isn't registered.** Notification handlers need `registerNotification(...)` (or `+handler`):

   ```kotlin
   registry.scope {
       +OrderCreatedEmailHandler(mailer)   // registerNotification under the hood
   }
   ```

   With the default `ThrowMissingNotificationHandler`, publishing a type with *zero* handlers throws
   `MissingNotificationHandlerException` — so if you see nothing at all, check whether someone configured
   `SilentMissingNotificationHandler`, which drops unhandled notifications without a trace.

2. **Wrong notification type.** Handlers are resolved by the notification's **exact runtime class** — there is no
   supertype dispatch. A handler registered for `OrderEvent` will *not* receive a published `OrderCreated`, even if
   `OrderCreated : OrderEvent`:

   ```kotlin
   registry registerNotification handler          // registered for T = OrderEvent
   mediator.publish(OrderCreated(id))             // resolves handlers for OrderCreated::class -> none

   // Fix: register for the concrete type you actually publish
   registry.registerNotification<OrderCreated>(handler)
   ```

3. **Another handler's exception cancelled yours.** The default publisher is `ParallelNotificationPublisher`:
   handlers run as child coroutines, and the **first failure cancels the rest**. `SequentialNotificationPublisher`
   similarly stops at the first throw. Use `CONTINUE_ON_EXCEPTION` so every handler runs and failures surface
   together as an `AggregateException`:

   ```kotlin
   mediator.publish(OrderCreated(id), NotificationPublishStrategy.CONTINUE_ON_EXCEPTION)
   ```

4. **Fire-and-forget swallowed the failure.** `FireAndForgetNotificationPublisher` launches handlers on your scope
   and returns immediately — exceptions never propagate to the publisher. They only surface via the scope's
   `CoroutineExceptionHandler`, so install one if handlers seem to vanish:

   ```kotlin
   val scope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, e ->
       logger.error("Notification handler failed", e)
   })
   mediator.publish(event, NotificationPublishStrategy.fireAndForget(scope))
   ```

---

## Validation failures — how to catch and debug

**Symptom** — `send()` throws `ValidationException` and the handler never runs.

**Cause** — `ValidationBehavior` is registered automatically by `MediatorFactory.create` at order `-50`, before most
behaviors and always before the handler. It first runs the request's own `validate()`, then every
`RequestValidator` registered for the request type; the first `ValidationResult.Invalid` throws.

**Fix / debugging** — the exception carries the full error list in `errors`; the message joins them with `"; "`:

```kotlin
try {
    mediator.send(CreateTodoCommand(title = "", dueDate = yesterday))
} catch (e: ValidationException) {
    e.errors.forEach { println("validation: $it") }
    // "Title must not be blank", "Due date must be in the future"
}

// Or without try/catch:
mediator.trySend(CreateTodoCommand(title = "", dueDate = yesterday))
    .onFailure { if (it is ValidationException) showErrors(it.errors) }
```

If only the *first* error appears when you expected several, the request uses `rulesFailFast { }` — switch to
`rules { }` to collect all failures. See [Validation](core/validation.md).

---

## Values put in `RequestContext` are missing

**Symptom** — a behavior calls `requestContext.put("userId", ...)`, but `getMetadata` returns `null` in the handler.

**Cause** — a **fresh `RequestContext` is created for every `send()` call**. Context values only live for one
pipeline execution; they are never shared between two dispatches, and a nested `mediator.send()` inside a handler
gets its **own, empty** context:

```kotlin
class OuterHandler : RequestHandler<OuterCommand, Unit> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: OuterCommand) {
        requestContext.put("userId", "u-1")
        mediator.send(InnerCommand())   // InnerCommand's pipeline gets a NEW context — "userId" is not there
    }
}
```

**Fix** — populate the context in a `Stage.Pre` behavior so it happens for *every* dispatch, or carry the data on the
request itself when it must cross a nested `send()`:

```kotlin
class UserIdBehavior(private val session: Session) : PipelineBehavior {
    override val stage = Stage.Pre

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        requestContext.put("userId", session.currentUserId())
        return next(request)
    }
}
```

Also note that `getMetadata<T>(key)` returns `null` **both** when the key is absent *and* when the stored value can't
be cast to `T` — storing an `Int` and reading a `String` fails silently:

```kotlin
requestContext.put("count", 3)
requestContext.getMetadata<String>("count")   // null — wrong type, no exception
requestContext.getMetadata<Int>("count")      // 3
```

See [Request Context](core/context.md) for typed extension properties that eliminate this class of bug.

---

## Pipeline behavior runs in the wrong order

**Symptom** — you set `order = -1000` but another behavior still wraps yours.

**Cause** — **`stage` always beats `order`.** Behaviors are grouped `Stage.Pre` → `Stage.Default` → `Stage.Post`
first; `order` only sequences behaviors *within* the same stage. A `Stage.Pre` behavior with `order = 999` still runs
outside a `Stage.Default` behavior with `order = -1000`.

**Fix** — put competing behaviors in the same stage, then use `order`:

```kotlin
class TracingBehavior : PipelineBehavior {
    override val stage = Stage.Pre    // move to Pre to wrap outside all Default behaviors
    override val order = 0
    // ...
}
```

Two more rules worth knowing:

- **`Stage.Post` order is inverted**: lower `order` = *innermost* = observes the handler's result first on the way
  out (the opposite of Pre/Default, where lower = outermost).
- Within a stage, behaviors with equal `order` run in **registration order** — the sort is stable.

See [Pipeline stages](core/pipeline.md#pipeline-stages).

---

## KSP-generated registrar is missing handlers

**Symptom** — you use `mediatork-ksp-koin`, but `GeneratedMediatorRegistrar` doesn't include some handlers (or wasn't
generated at all), and dispatching throws `MissingHandlerException`.

**Cause** — the processor scans each Gradle module it is applied to for **concrete classes that directly implement
`RequestHandler` or `NotificationHandler`**. A handler is skipped when it:

- lives in a Gradle module where the `ksp` plugin (with the `mediatork-ksp-koin` dependency) is **not applied** —
  KSP is per-module;
- implements the interface **indirectly** through a base class (`class MyHandler : BaseHandler()`) — only direct
  supertypes are matched;
- is `abstract`, `sealed`, or an `object` — only regular concrete `class` declarations are picked up;
- sits in a test source set or a file matching `*Test.kt` / `*Spec.kt` — test files are excluded by design;
- is a **stream handler or validator** — the processor only scans `RequestHandler` and `NotificationHandler`;
  register `StreamRequestHandler`s and `RequestValidator`s manually.

**Fix** — apply KSP in every module that declares handlers, keep handlers as concrete top-level classes implementing
the interface directly, and include the generated Koin module:

```kotlin
// build.gradle.kts of EACH module containing handlers
plugins { id("com.google.devtools.ksp") }
dependencies { ksp(project(":mediatork-ksp-koin")) }
```

```kotlin
// Koin setup — the generated module provides GeneratedMediatorRegistrar (bound to MediatorRegistrar)
startKoin {
    modules(generatedHandlersModule, appModule)   // com.fajrbahr.mediatork.generated
}
```

After a build, inspect the generated file to see exactly what was found:
`build/generated/ksp/.../com/fajrbahr/mediatork/generated/GeneratedKoinMediatorModule.kt`. The KSP log also reports
`MediatorK Koin KSP: found N request handlers, M notification handlers` — run Gradle with `--info` to see it.

---

## Still stuck?

- [Exception Handling](core/exceptions.md) — every built-in exception and how to customize missing-handler behavior.
- [testMediator](testing/test-mediator.md) — spin up a real mediator in tests to assert handlers behave as expected.
