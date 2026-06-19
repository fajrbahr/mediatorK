---
id: viewmodel
title: ViewModel
sidebar_label: ViewModel
---

# ViewModel

MediatorK works naturally with Android `ViewModel` — inject the mediator and dispatch requests from `viewModelScope`.

---

## Basic usage

```kotlin
class UserViewModel(private val mediator: Mediator) : ViewModel() {

    val user = MutableStateFlow<User?>(null)

    fun load(id: String) {
        viewModelScope.launch {
            user.value = mediator.send(GetUserQuery(id))
        }
    }
}
```

---

## With Koin

Declare the ViewModel in your Koin module and inject the mediator via `get()`:

```kotlin
// koin module
viewModel { UserViewModel(get()) }
```

---

## Handling validation errors

When `ValidationBehavior` is in the pipeline, catch `ValidationException` and surface its message in your UI state:

```kotlin
class CreateTodoViewModel(private val mediator: Mediator) : ViewModel() {

    val error = MutableStateFlow<String?>(null)

    fun submit(title: String, dueDate: LocalDate) {
        viewModelScope.launch {
            try {
                mediator.send(CreateTodoCommand(title, dueDate))
            } catch (e: ValidationException) {
                error.value = e.message
            }
        }
    }
}
```

See [Validation](../core/validation.md) for how `ValidationBehavior` and `ValidationException` are set up.

---

## Next

→ [Ktor](ktor.md)
