---
id: spring
title: Spring Boot
sidebar_label: Spring Boot
---

# Spring Boot

MediatorK integrates with Spring Boot without any special plugin — just register handlers as beans and create the
mediator in a `@Configuration` class.

See the [full Spring Boot example](../examples/spring-boot-3.md) for a complete WebFlux CRUD API.

---

## Setup

For the MediatorK dependency see [Installation](../installation.md). For Spring Boot itself, follow
the [official Spring Initializr setup](https://start.spring.io/) — select **Kotlin**, **Gradle**, and the **Spring
WebFlux** starter.

---

## Pattern

### 1. Handler beans

```kotlin
@Service
class GetUserHandler(private val repo: UserRepository) : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(mediator: Mediator, ctx: RequestContext, request: GetUserQuery) =
        repo.findById(request.id) ?: error("Not found")
}
```

### 2. Registrar bean

Group all handlers into a `MediatorRegistrar`:

```kotlin
@Component
class UserRegistrar(
    private val getUser: GetUserHandler,
    private val createUser: CreateUserHandler,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +getUser
            +createUser
        }
    }
}
```

### 3. Mediator bean

Spring collects all `MediatorRegistrar` beans automatically:

```kotlin
@Configuration
class MediatorConfig(private val registrars: List<MediatorRegistrar>) {
    @Bean
    fun mediator(): Mediator = MediatorFactory.create(
        registrars = registrars,
        pipelineBehaviors = listOf(LoggingBehavior()),
    )
}
```

### 4. Controller

```kotlin
@RestController
@RequestMapping("/users")
class UserController(private val mediator: Mediator) {

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String) = mediator.send(GetUserQuery(id))

    @PostMapping
    suspend fun create(@RequestBody body: CreateUserRequest) =
        mediator.send(CreateUserCommand(body.name, body.email))
}
```

---

## Sample — Prayer Times API

The [`/samples/sample-spring`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-spring) module is a
runnable Spring Boot
WebFlux application that fetches prayer times and Islamic calendar months from the
[Aladhan API](https://aladhan.com/prayer-times-api). It mirrors the [Android sample](../sample.md) structure in three
layers:

| Layer           | Routes                                                           | Pattern                                          |
|-----------------|------------------------------------------------------------------|--------------------------------------------------|
| **Before**      | `/before/prayer-times/{city}` · `/before/islamic-months`         | `Controller → UseCase → Repository → DataSource` |
| **After**       | `/after/prayer-times/{city}` · `/after/islamic-months`           | `Controller → Mediator → Handler`                |
| **After Super** | `/aftersuper/prayer-times/{city}` · `/aftersuper/islamic-months` | After + all pipeline behaviors                   |

```bash
# Run (from the repo root)
./gradlew :samples:sample-spring:bootRun

# Try it
curl http://localhost:8081/before/prayer-times/London
curl http://localhost:8081/after/prayer-times/London
curl http://localhost:8081/aftersuper/prayer-times/London
```

---

## Next

→ [Koin](koin.md)
