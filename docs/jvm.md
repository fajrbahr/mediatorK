# Kotlin JVM

Use MediatorK in any JVM project — Spring Boot, Ktor, CLI tools, or plain Kotlin.

---

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.1")
}
```

```groovy
// build.gradle
dependencies {
    implementation 'io.github.fajrbahr:mediatork:0.1.1'
}
```

---

## Basic wiring

```kotlin
fun main() {
    val mediator = MediatorFactory.create(
        registrars = listOf(AppRegistrar()),
        pipelineBehaviors = listOf(LoggingBehavior()),
    )

    runBlocking {
        val user = mediator.send(GetUserQuery("user-1"))
        println(user)
    }
}
```

---

## Spring Boot

See the full [Spring Boot example](examples/spring-boot-3.md) for a complete WebFlux CRUD API.

Key points:
- Declare each handler as a `@Service` bean
- Collect them in a `MediatorRegistrar` `@Component`
- Expose a `@Bean fun mediator()` in a `@Configuration` class

```kotlin
@Configuration
class MediatorConfig(private val registrars: List<MediatorRegistrar>) {
    @Bean
    fun mediator(): Mediator = MediatorFactory.create(registrars = registrars)
}
```

Spring auto-injects all `MediatorRegistrar` beans — no manual wiring needed.

---

## Ktor

```kotlin
fun Application.configureRouting(mediator: Mediator) {
    routing {
        get("/users/{id}") {
            val user = mediator.send(GetUserQuery(call.parameters["id"]!!))
            call.respond(user)
        }
    }
}
```

Register the mediator as a singleton in your DI module:

```kotlin
val appModule = module {
    single { AppRegistrar(get()) }
    single {
        MediatorFactory.create(registrars = listOf(get<AppRegistrar>()))
    }
}
```

---

## Next

→ [Kotlin Multiplatform](kmp.md)
