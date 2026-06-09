---
id: sample
title: Sample
sidebar_label: Sample
---

# Sample

The [`/sample`](https://github.com/fajrbahr/MediatorK/tree/main/sample) module in the repository contains runnable
Kotlin code that demonstrates the patterns described in the docs.

---

## What's inside

### ViewModel — before & after

See the difference between managing multiple use-cases manually versus delegating through the mediator:

- **Before** — `sample/src/main/kotlin/sample/android/OrderViewModelBefore.kt`
- **After** — `sample/src/main/kotlin/sample/android/OrderViewModelAfter.kt`

### Commands, Queries, Notifications

Real handler implementations wired through a `MediatorRegistrar`:

- `sample/src/main/kotlin/sample/command/CreateOrderCommand.kt`
- `sample/src/main/kotlin/sample/query/FetchUserQuery.kt`
- `sample/src/main/kotlin/sample/notification/`

### Pipeline Behaviors

Working examples of logging, auth, retry, validation, tracing, and metrics behaviors:

- `sample/src/main/kotlin/sample/behaviors/`

### Tests

Handler and ViewModel tests with no mocking library:

- `sample/src/test/kotlin/sample/SampleHandlerTest.kt`
- `sample/src/test/kotlin/sample/OrderViewModelTest.kt`

---

Browse the full module on [GitHub →](https://github.com/fajrbahr/MediatorK/tree/main/sample)
