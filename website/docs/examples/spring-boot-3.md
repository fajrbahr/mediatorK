---
id: spring-boot-3
title: Spring Boot Example
sidebar_label: Spring Boot
---

# Spring Boot Example

A complete CRUD API using MediatorK with Spring Boot (WebFlux + coroutines).

## 1. Add dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.6.2")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
}
```

---

## 2. Define the domain model

```kotlin
data class Todo(val id: Int, val title: String, val completed: Boolean)
```

---

## 3. Define requests

```kotlin
data class GetTodosQuery : Request<List<Todo>>
data class GetTodoQuery(val id: Int) : Request<Todo>
data class CreateTodoCommand(val title: String) : Request<Todo>
data class CompleteTodoCommand(val id: Int) : Request.Unit
data class DeleteTodoCommand(val id: Int) : Request.Unit
```

---

## 4. Implement handlers

```kotlin
@Service
class GetTodosHandler(private val repo: TodoRepository) : RequestHandler<GetTodosQuery, List<Todo>> {
    override suspend fun handle(mediator: Mediator, ctx: RequestContext, request: GetTodosQuery) =
        repo.findAll()
}

@Service
class GetTodoHandler(private val repo: TodoRepository) : RequestHandler<GetTodoQuery, Todo> {
    override suspend fun handle(mediator: Mediator, ctx: RequestContext, request: GetTodoQuery) =
        repo.findById(request.id) ?: error("Todo ${request.id} not found")
}

@Service
class CreateTodoHandler(private val repo: TodoRepository) : RequestHandler<CreateTodoCommand, Todo> {
    override suspend fun handle(mediator: Mediator, ctx: RequestContext, request: CreateTodoCommand) =
        repo.save(Todo(id = 0, title = request.title, completed = false))
}

@Service
class CompleteTodoHandler(private val repo: TodoRepository) : RequestHandler<CompleteTodoCommand, Unit> {
    override suspend fun handle(mediator: Mediator, ctx: RequestContext, request: CompleteTodoCommand) {
        val todo = repo.findById(request.id) ?: return
        repo.save(todo.copy(completed = true))
    }
}

@Service
class DeleteTodoHandler(private val repo: TodoRepository) : RequestHandler<DeleteTodoCommand, Unit> {
    override suspend fun handle(mediator: Mediator, ctx: RequestContext, request: DeleteTodoCommand) {
        repo.deleteById(request.id)
    }
}
```

---

## 5. Register handlers

```kotlin
@Component
class TodoRegistrar(
    private val getTodos: GetTodosHandler,
    private val getTodo: GetTodoHandler,
    private val create: CreateTodoHandler,
    private val complete: CompleteTodoHandler,
    private val delete: DeleteTodoHandler,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +getTodos
            +getTodo
            +create
            +complete
            +delete
        }
    }
}
```

---

## 6. Create the Mediator bean

```kotlin
@Configuration
class MediatorConfig(private val registrars: List<MediatorRegistrar>) {
    @Bean
    fun mediator(): Mediator = MediatorFactory.create(registrars = registrars)
}
```

:::tip
Spring injects all `MediatorRegistrar` beans automatically — no manual wiring needed.
:::

---

## 7. Controller

```kotlin
@RestController
@RequestMapping("/todos")
class TodoController(private val mediator: Mediator) {

    @GetMapping
    suspend fun getAll() = mediator.send(GetTodosQuery())

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Int) = mediator.send(GetTodoQuery(id))

    @PostMapping
    suspend fun create(@RequestBody body: CreateTodoCommand) = mediator.send(body)

    @PatchMapping("/{id}/complete")
    suspend fun complete(@PathVariable id: Int) = mediator.send(CompleteTodoCommand(id))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(@PathVariable id: Int) = mediator.send(DeleteTodoCommand(id))
}
```

---

## 8. Run

```bash
./gradlew bootRun
```

Test it:

```bash
# Create
curl -X POST http://localhost:8080/todos -H 'Content-Type: application/json' -d '{"title":"Buy milk"}'

# List
curl http://localhost:8080/todos

# Complete
curl -X PATCH http://localhost:8080/todos/1/complete

# Delete
curl -X DELETE http://localhost:8080/todos/1
```
