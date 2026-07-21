# Changelog

## Unreleased

- **Breaking (`mediatork-test`): replaced `StubMediator` with `testMediator { }`.** Test doubles now run the
  **real** `mediatorK { }` pipeline — register handler bodies with the same `handle`/`handleStream`/`notification`/
  `validate`/`behaviors` DSL as production instead of learning a separate stub API. No mocks, no stubs.
- **New: `RecordingMediator`** — wraps any real `Mediator` and records every `send`/`stream`/`publish` for assertions
  (`sent`, `published`, `sentOf<T>()`, `publishedOf<T>()`). `testMediator { }` returns one. Use it directly to record
  around a mediator assembled from production modules.
- **Breaking (`mediatork-test`): removed `DummyMediator`.** A no-op double whose `send` silently returned
  `Unit` cast to any type could mask bugs. Use an empty `testMediator { }` instead — a real mediator that fails
  loudly with `MissingHandlerException` if `send` is unexpectedly called.

## 0.9.7

- **New: `StubMediator`** — lightweight stub with a clean DSL for ViewModel and integration tests.
  `on<T>() returns value`, `onNotification<T>() answers {}`, `onStream<T>() returns listOf()`.
  Pipeline behaviors supported via `onPipeline(behavior)` with per-stub and global enable/disable.

## 0.9.6

- Lazy handler registration — handlers are resolved on first use, not at startup.

## 0.9.5

- CI build workflow.

## 0.9.4

- Delete query + command pattern in sample app.

## 0.9.3

- Initial public release with full Mediator, pipeline behaviors, validation, streaming, and test utilities.
