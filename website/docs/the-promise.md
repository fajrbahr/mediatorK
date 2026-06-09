---
id: the-promise
title: The Promise
sidebar_label: The Promise
---

# The Promise

> **From a ViewModel with 12 constructor parameters — down to one.**

This page shows the single concrete promise MediatorK makes: your ViewModel shrinks to a thin dispatcher, every piece of logic moves into a focused handler, and your tests need zero mocking libraries.

---

## The Problem — an XXL ViewModel

Real-world ViewModels tend to grow. Each new feature pulls in another dependency until the constructor looks like this:

```kotlin
class InitialViewModel(
    private val applicationMetadata: ApplicationMetadata,
    private val retrieveAndStoreTogglesUseCase: RetrieveAndStoreTogglesUseCase,
    watchTogglesUseCase: WatchTogglesUseCase,
    private val persistCachedInfoUseCase: PersistCachedInfoUseCase,
    private val fetchActiveUserAndStoreUseCase: FetchActiveUserAndStoreUseCase,
    fetchPreferredLocaleUseCase: FetchPreferredLocaleUseCase,
    fetchVisualThemeUseCase: FetchVisualThemeUseCase,
    private val metricsReporterPort: MetricsReporterPort,
    val runtimeSettings: RuntimeSettings,
    val speedMonitor: SpeedMonitor,
    val cloudPerformanceTracker: PerformanceTraceListener,
    val simpleLoggingTracker: SimpleLoggingTracker,
) : ViewModel()
```

Twelve dependencies. Testing this requires constructing or mocking all twelve, even for a test that only cares about one use-case.

---

## The Solution — an XXS ViewModel

With MediatorK the ViewModel has exactly one dependency:

```kotlin
class InitialViewModel(
    private val mediator: Mediator,
) : ViewModel()
```

Every action becomes a `mediator.send(...)` call. The ViewModel no longer knows which use-case, repository, or data source handles the request — it just dispatches.

---

## How it fits together

The diagram below maps the layers from the image at the top of this page onto MediatorK concepts.

```
UI
 └─▶ ViewModel          (sends Requests via Mediator)
       └─▶ Handler      (one handler per use-case / command / query)
             ├─▶ Repository / UseCase
             │     ├─▶ LocalDataSource  ──▶ Database
             │     └─▶ RemoteDataSource ──▶ API
             └─▶ (optional) Pipeline Behaviors  e.g. logging, metrics, validation
```

Each arrow in the old architecture is now a `Request` → `Handler` pair. The ViewModel never imports a `Repository` or a `DataSource` again.

---

## A single handler combining everything

Here is how a handler that previously required a ViewModel to hold five dependencies now lives in one focused class:

```kotlin
// The request — a plain data class
data class InitializeAppRequest(
    val userId: String,
) : Request<InitialState>

// The response
data class InitialState(
    val toggles: List<Toggle>,
    val user: User,
    val locale: Locale,
    val theme: VisualTheme,
)

// The handler — owns all the dependencies that used to live in the ViewModel
class InitializeAppHandler(
    private val applicationMetadata: ApplicationMetadata,
    private val retrieveAndStoreTogglesUseCase: RetrieveAndStoreTogglesUseCase,
    private val persistCachedInfoUseCase: PersistCachedInfoUseCase,
    private val fetchActiveUserAndStoreUseCase: FetchActiveUserAndStoreUseCase,
    private val fetchPreferredLocaleUseCase: FetchPreferredLocaleUseCase,
    private val fetchVisualThemeUseCase: FetchVisualThemeUseCase,
) : RequestHandler<InitializeAppRequest, InitialState> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: InitializeAppRequest,
    ): InitialState {
        val toggles = retrieveAndStoreTogglesUseCase.execute()
        val user    = fetchActiveUserAndStoreUseCase.execute(request.userId)
        val locale  = fetchPreferredLocaleUseCase.execute()
        val theme   = fetchVisualThemeUseCase.execute()

        persistCachedInfoUseCase.execute(applicationMetadata)

        return InitialState(toggles, user, locale, theme)
    }
}
```

The ViewModel now reads:

```kotlin
class InitialViewModel(
    private val mediator: Mediator,
) : ViewModel() {

    val state = MutableStateFlow<InitialState?>(null)

    fun initialize(userId: String) {
        viewModelScope.launch {
            state.value = mediator.send(InitializeAppRequest(userId))
        }
    }
}
```

---

## Testing the ViewModel — zero mocking libraries

Because the ViewModel only depends on `Mediator`, tests use `FakeMediator` from `mediatork-test`. There is nothing to mock:

```kotlin
@Test
fun `initialize emits loaded state`() = runTest {
    val expectedState = InitialState(
        toggles = listOf(Toggle("dark_mode", enabled = true)),
        user    = User(id = "u1", name = "Alice"),
        locale  = Locale.ENGLISH,
        theme   = VisualTheme.DARK,
    )

    val mediator = FakeMediator()
    mediator.register(
        fakeHandler<InitializeAppRequest, InitialState> { _, _, _ -> expectedState }
    )

    val vm = InitialViewModel(mediator)
    vm.initialize("u1")
    advanceUntilIdle()

    assertEquals(expectedState, vm.state.value)
}
```

No `mockk {}`. No `Mockito.mock(...)`. No `every { ... } returns ...`.
One `FakeMediator`, one registered handler, done.

---

## Testing the handler — also without mocks

The handler itself is a plain class. Pass real or fake implementations of its dependencies directly:

```kotlin
@Test
fun `handler returns combined initial state`() = runTest {
    val handler = InitializeAppHandler(
        applicationMetadata             = FakeApplicationMetadata(),
        retrieveAndStoreTogglesUseCase  = FakeRetrieveTogglesUseCase(listOf(Toggle("dark_mode", true))),
        persistCachedInfoUseCase        = NoOpPersistCachedInfoUseCase(),
        fetchActiveUserAndStoreUseCase  = FakeFetchUserUseCase(User("u1", "Alice")),
        fetchPreferredLocaleUseCase     = FakeFetchLocaleUseCase(Locale.ENGLISH),
        fetchVisualThemeUseCase         = FakeFetchThemeUseCase(VisualTheme.DARK),
    )

    val result = handler.handle(
        mediator       = DummyMediator(),
        requestContext = RequestContext.empty(),
        request        = InitializeAppRequest(userId = "u1"),
    )

    assertEquals(listOf(Toggle("dark_mode", true)), result.toggles)
    assertEquals(User("u1", "Alice"), result.user)
    assertEquals(VisualTheme.DARK, result.theme)
}
```

Each fake is a small `class` or `object` that implements the interface — typically three to five lines. No mocking framework, no `verify(...)`, no argument captors.

---

## Summary

| Before | After |
|---|---|
| ViewModel with 12 constructor params | ViewModel with 1 param (`Mediator`) |
| Testing requires constructing / mocking 12 dependencies | Testing requires one `FakeMediator` |
| Business logic scattered across the ViewModel | Business logic isolated in focused `Handler` classes |
| Adding a feature touches the ViewModel constructor | Adding a feature adds a new `Request` + `Handler` |

That is the promise.
