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

When `ValidationBehavior` is in the pipeline, catch `ValidationException` and map each error to its own `StateFlow` by field type — no string matching needed:

```kotlin
class CreateTodoViewModel(private val mediator: Mediator) : ViewModel() {

    val titleError   = MutableStateFlow<String?>(null)
    val dueDateError = MutableStateFlow<String?>(null)
    val generalError = MutableStateFlow<String?>(null)

    fun submit(title: String, dueDate: LocalDate) {
        viewModelScope.launch {
            try {
                mediator.send(CreateTodoCommand(title, dueDate))
            } catch (e: ValidationException) {
                e.errors.forEach { error ->
                    when (error.field) {
                        CreateTodoFields.TITLE    -> titleError.value   = error.message
                        CreateTodoFields.DUE_DATE -> dueDateError.value = error.message
                        else                      -> generalError.value  = error.message
                    }
                }
            }
        }
    }
}
```

See [Validation](../core/validation.md) for how `ValidationBehavior` and `ValidationException` are set up.
