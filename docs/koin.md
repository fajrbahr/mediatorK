# Koin

MediatorK works naturally with [Koin](https://insert-koin.io/) for dependency injection in KMP, Android, and iOS projects.

---

## Setup

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.1")
    implementation("io.insert-koin:koin-core:3.5.6")
    // Android
    implementation("io.insert-koin:koin-android:3.5.6")
}
```

---

## Define a Koin module

```kotlin
val mediatorModule = module {

    // Handlers
    factory { GetUserHandler(get()) }
    factory { CreateOrderHandler(get()) }
    factory { DeleteAccountHandler(get()) }

    // Registrar collects all handlers
    single {
        object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.scope {
                    +get<GetUserHandler>()
                    +get<CreateOrderHandler>()
                    +get<DeleteAccountHandler>()
                }
            }
        }
    }

    // Mediator singleton
    single {
        MediatorFactory.create(
            registrars = listOf(get<MediatorRegistrar>()),
            pipelineBehaviors = listOf(LoggingBehavior()),
        )
    }
}
```

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

→ [API Reference](api.md)
