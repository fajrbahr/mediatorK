---
id: jvm
title: Kotlin JVM
sidebar_label: Kotlin JVM
---

# Kotlin JVM

Use MediatorK in any JVM project — Spring Boot, Ktor, CLI tools, or plain Kotlin.

See [Installation](../installation.md) for dependency coordinates.

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
