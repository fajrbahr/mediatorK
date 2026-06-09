---
id: koin
title: Koin
sidebar_label: Koin
---

# Koin

MediatorK works naturally with [Koin](https://insert-koin.io/) for dependency injection in KMP, Android, and iOS projects.

---

## Setup

Add Koin alongside MediatorK (see [Installation](../installation.md) for the MediatorK coordinate):

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.insert-koin:koin-core:3.5.6")
    // Android
    implementation("io.insert-koin:koin-android:3.5.6")
}
```

---

## Define a Koin module

Declare each registrar and behavior as its own binding, then use `getAll<T>()` to collect them automatically — no manual list construction needed:

```kotlin
val mediatorModule = module {

    // Registrars
    single<MediatorRegistrar> { UserRegistrar(get()) }
    single<MediatorRegistrar> { OrderRegistrar(get()) }

    // Pipeline behaviors
    single<PipelineBehavior> { LoggingBehavior() }
    single<PipelineBehavior> { ValidationBehavior(getAll()) }

    // Mediator singleton — getAll<T>() collects every binding of that type
    single {
        val registrars = getAll<MediatorRegistrar>()
        MediatorFactory.create(
            registrars = registrars,
            pipelineBehaviors = getAll<PipelineBehavior>(),
            notificationPublisher = ParallelNotificationPublisher(),
        )
    }
}
```

`getAll<T>()` resolves every Koin binding for the given type, so adding a new registrar or behavior is a one-line change — the mediator picks it up automatically.

---

## Android — start Koin in Application

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(mediatorModule, repositoryModule)
        }
    }
}
```

---

## Use in ViewModel

```kotlin
class UserViewModel(private val mediator: Mediator) : ViewModel() {

    val user = MutableStateFlow<User?>(null)

    fun load(id: String) {
        viewModelScope.launch {
            user.value = mediator.send(GetUserQuery(id))
        }
    }
}

// koin module
viewModel { UserViewModel(get()) }
```

---

## KMP shared module

Declare the mediator in `commonMain` and inject platform repositories:

```kotlin
// commonMain
val sharedModule = module {
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single {
        MediatorFactory.create(
            registrars = listOf(
                object : MediatorRegistrar {
                    override fun register(registry: HandlerRegistry) {
                        registry register GetProductHandler(get())
                    }
                }
            )
        )
    }
}
```

---

## Next

→ [API Reference](../api.md)
