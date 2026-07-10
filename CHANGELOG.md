# Changelog

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
