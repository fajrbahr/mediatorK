---
id: ktor
title: Ktor
sidebar_label: Ktor
---

# Ktor

MediatorK works naturally with Ktor; register the mediator as a singleton in your DI module and inject it into your
route configuration.

See [Installation](../installation.md) for dependency coordinates.

---

## Setup

Add the Ktor dependencies alongside MediatorK:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
```

---

## Routing

Pass the mediator into your route configuration function:

```kotlin
fun Application.configureRouting(mediator: Mediator) {
    routing {
        get("/users/{id}") {
            val user = mediator.send(GetUserQuery(call.parameters["id"]!!))
            call.respond(user)
        }

        post("/users") {
            val body = call.receive<CreateUserRequest>()
            val user = mediator.send(CreateUserCommand(body.name, body.email))
            call.respond(HttpStatusCode.Created, user)
        }
    }
}
```

---

## Wiring with Koin

Register the mediator as a singleton in your Koin module:

```kotlin
val appModule = module {
    single { AppRegistrar(get()) }
    single {
        MediatorFactory.create(
            registrars = listOf(get<AppRegistrar>()),
            pipelineBehaviors = listOf(LoggingBehavior()),
        )
    }
}
```

Install Koin and call your route configuration in `Application`:

```kotlin
fun Application.module() {
    install(Koin) { modules(appModule) }
    val mediator by inject<Mediator>()
    configureRouting(mediator)
}
```

---

## Sample: Prayer Times API

The [`/samples/sample-ktor`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-ktor) module is a runnable
Ktor server
that fetches prayer times and Islamic calendar months from the [Aladhan API](https://aladhan.com/prayer-times-api). It
mirrors the [Android sample](../sample.md) structure in three layers:

| Layer           | Routes                                                           | Pattern                                     |
|-----------------|------------------------------------------------------------------|---------------------------------------------|
| **Before**      | `/before/prayer-times/{city}` · `/before/islamic-months`         | `Route → UseCase → Repository → DataSource` |
| **After**       | `/after/prayer-times/{city}` · `/after/islamic-months`           | `Route → Mediator → Handler`                |
| **After Super** | `/aftersuper/prayer-times/{city}` · `/aftersuper/islamic-months` | After + all pipeline behaviors              |

```bash
# Run (from the repo root)
./gradlew :samples:sample-ktor:run

# Try it
curl http://localhost:8080/before/prayer-times/London
curl http://localhost:8080/after/prayer-times/London
curl http://localhost:8080/aftersuper/prayer-times/London
```

---

## Next

→ [Kotlin Multiplatform](kmp.md)
