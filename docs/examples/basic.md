# Basic Example

A complete end-to-end example using MediatorK in a plain Kotlin project.

## 1. Add the dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
```

---

## 2. Define your domain

```kotlin
data class User(val id: String, val name: String)
```

---

## 3. Define requests and notifications

```kotlin
// Query — expects exactly one handler, returns a result
data class GetUserQuery(val id: String) : Request<User>

// Command — performs a side effect, returns Unit
data class CreateUserCommand(val id: String, val name: String) : Request<Unit>

// Notification — broadcast to zero or more handlers, no return value
data class UserCreatedEvent(val userId: String) : Notification
```

---

## 4. Implement handlers

```kotlin
class GetUserHandler(private val db: UserRepository) : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetUserQuery,
    ): User = db.findById(request.id) ?: error("User ${request.id} not found")
}

class CreateUserHandler(private val db: UserRepository) : RequestHandler<CreateUserCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateUserCommand,
    ) {
        db.save(User(request.id, request.name))
        mediator.publish(UserCreatedEvent(request.id))
    }
}

// Two handlers react to the same notification independently
class SendWelcomeEmailHandler : NotificationHandler<UserCreatedEvent> {
    override suspend fun handle(notification: UserCreatedEvent) {
        println("Sending welcome email for user ${notification.userId}")
    }
}

class TrackSignupHandler : NotificationHandler<UserCreatedEvent> {
    override suspend fun handle(notification: UserCreatedEvent) {
        println("Tracking signup for user ${notification.userId}")
    }
}
```

---

## 5. Register and wire up

```kotlin
class UserRegistrar(private val db: UserRepository) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +GetUserHandler(db)
            +CreateUserHandler(db)
            +SendWelcomeEmailHandler()
            +TrackSignupHandler()
        }
    }
}
```

---

## 6. Create the mediator and use it

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val db = InMemoryUserRepository()

    val mediator = MediatorFactory.create(
        registrars = listOf(UserRegistrar(db)),
    )

    // Command
    mediator.send(CreateUserCommand(id = "u-1", name = "Alice"))

    // Query
    val user = mediator.send(GetUserQuery(id = "u-1"))
    println("Found: ${user.name}") // Found: Alice
}
```

---

## 7. Add a pipeline behavior (optional)

Behaviors wrap every request — useful for logging, retry, auth, etc.

```kotlin
class LoggingBehavior : PipelineBehavior {
    override val order = 0

    override suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq,
    ): TRes {
        println("→ ${request::class.simpleName}")
        val result = next(request)
        println("← ${request::class.simpleName}")
        return result
    }
}

val mediator = MediatorFactory.create(
    registrars = listOf(UserRegistrar(db)),
    pipelineBehaviors = listOf(LoggingBehavior()),
)
```
