# Spring Boot 3.x Examples

This example demonstrates how to use Kordinator with Spring Boot 3.x.

## Prerequisites

Create a new Spring Boot 3.x project. You can use [Spring Initializr](https://start.spring.io/) to create a new project.

## Step 1: Create Project with Spring Boot 3.x and dependencies

Create Spring Boot project with Reactive Web dependency. You can use [Spring Initializr](https://start.spring.io/) to
create a new project.

## Step 2: Add Kordinator dependency

### Maven

Add Kordinator dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>dev.ceviz</groupId>
    <artifactId>kordinator</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
<groupId>dev.ceviz</groupId>
<artifactId>spring-3x-kordinator</artifactId>
<version>1.0.0</version>
</dependency>
```

### Gradle (Groovy)

Add Kordinator dependency to your `build.gradle`:

```groovy
implementation 'dev.ceviz:kordinator:1.0.0'
implementation 'dev.ceviz:spring-3x-kordinator:1.0.0'
```

### Gradle (Kotlin)

Add Kordinator dependency to your `build.gradle.kts`:

```kotlin
implementation("dev.ceviz:kordinator:1.0.0")
implementation("dev.ceviz:spring-3x-kordinator:1.0.0")
```

## Step 3: Create a simple data modal

Create a simple data model to represent a todo:

```kotlin
data class Todo(
    var id: Int,
    var title: String,
    var description: String,
    var completed: Boolean
)
```

## Step 4: Create commands and queries

Create commands and queries to interact with todos:

### CreateTodoCommand

```kotlin
data class CreateTodoCommand(
    val title: String,
    val description: String
) : Command
```

### UpdateTodoCommand

```kotlin
data class UpdateTodoCommand(
    var id: Int,
    val completed: Boolean
) : Command
```

### DeleteTodoCommand

```kotlin
data class DeleteTodoCommand(
    @PathVariable("id") val id: Int,
) : Command
```

### GetTodoQuery

```kotlin
data class GetTodosQuery : Query<List<Todo>>
```

## Step 5: Create a simple Repository

Create a simple repository to store todos:

```kotlin
interface TodoRepository {
    fun findAll(): List<Todo>
    fun findById(id: Int): Todo?
    fun save(todo: Todo): Todo
    fun update(todo: Todo): Todo
    fun deleteById(id: Int)
}

@Service
class TodoRepositoryImpl : TodoRepository {
    private val todos = mutableListOf<Todo>()

    override fun findAll(): List<Todo> {
        return todos
    }

    override fun findById(id: Int): Todo? {
        return todos.find { it.id == id }
    }

    override fun save(todo: Todo): Todo {
        val id = if (todos.isEmpty()) 1 else todos.last().id + 1
        val newTodo = todo.copy(id = id)
        todos.add(newTodo)
        return newTodo
    }

    override fun update(todo: Todo): Todo {
        val index = todos.indexOfFirst { it.id == todo.id }
        todos[index] = todo
        return todo
    }

    override fun deleteById(id: Int) {
        todos.removeIf { it.id == id }
    }
}
```

## Step 6: Implement Command and Query Handlers

### CreateTodoCommandHandler

```kotlin
@Service
class CreateTodoCommandHandler(
    private val repository: TodoRepository
) : CommandHandler<CreateTodoCommand> {
    override suspend fun handle(command: CreateTodoCommand) {
        repository.save(
            Todo(
                id = 0,
                title = command.title,
                description = command.description,
                completed = false
            )
        )
    }
}
```

### UpdateTodoCommandHandler

```kotlin
@Service
class UpdateTodoCommandHandler(
    private val repository: TodoRepository
) : CommandHandler<UpdateTodoCommand> {
    override suspend fun handle(command: UpdateTodoCommand) {
        val todo = repository.findById(command.id)
        todo?.let {
            it.completed = command.completed
            repository.update(it)
        }
    }
}
```

### DeleteTodoCommandHandler

```kotlin
@Service
class DeleteTodoCommandHandler(
    private val repository: TodoRepository
) : CommandHandler<DeleteTodoCommand> {
    override suspend fun handle(command: DeleteTodoCommand) {
        repository.deleteById(command.id)
    }
}
```

### GetTodosQueryHandler

```kotlin
@Service
class GetTodosQueryHandler(
    private val repository: TodoRepository
) : QueryHandler<GetTodosQuery, List<Todo>> {
    override suspend fun handle(query: GetTodosQuery): List<Todo> {
        return repository.findAll()
    }
}
```

## Step 7: Create a Controller

Create a controller to handle HTTP requests:

```kotlin
@RequestMapping("/todos")
@RestController
class TodosController(private val mediator: Mediator) {
    @GetMapping
    suspend fun getTodos(query: GetTodosQuery) = mediator.send(query)

    @PostMapping
    suspend fun createTodo(@RequestBody command: CreateTodoCommand) = mediator.send(command)

    @PatchMapping("/{id}")
    suspend fun markAsComplete(@PathVariable("id") id: Int, @RequestBody command: UpdateTodoCommand) {
        command.id = id
        mediator.send(command)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(command: DeleteTodoCommand) = mediator.send(command)
}
```

## Step 8: Run the Application

Run the application and test the endpoints using a REST client like Postman or curl.

## Conclusion

In this example, you learned how to use Kordinator with Spring Boot 3.x to implement CQRS and Mediator pattern.

