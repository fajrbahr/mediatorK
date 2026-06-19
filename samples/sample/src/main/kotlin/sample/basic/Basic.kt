package sample.basic

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import kotlinx.coroutines.runBlocking

// ── Domain model ──────────────────────────────────────────────────────────────

data class Todo(val id: String, val title: String, val done: Boolean = false)

// ── Command ───────────────────────────────────────────────────────────────────

data class AddTodoCommand(val id: String, val title: String) : Request<Todo>

class AddTodoHandler(private val store: TodoStore) : RequestHandler<AddTodoCommand, Todo> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: AddTodoCommand): Todo {
        val todo = Todo(id = request.id, title = request.title)
        store.save(todo)
        mediator.publish(TodoAddedNotification(todo))
        return todo
    }
}

// ── Query ─────────────────────────────────────────────────────────────────────

data class GetTodoQuery(val id: String) : Request<Todo?>

class GetTodoHandler(private val store: TodoStore) : RequestHandler<GetTodoQuery, Todo?> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: GetTodoQuery): Todo? =
        store.findById(request.id)
}

// ── Notifications ─────────────────────────────────────────────────────────────

data class TodoAddedNotification(val todo: Todo) : Notification

class LogTodoAddedHandler : NotificationHandler<TodoAddedNotification> {
    override suspend fun handle(notification: TodoAddedNotification) {
        println("[Log] Todo added: '${notification.todo.title}' (id=${notification.todo.id})")
    }
}

class SyncTodoAddedHandler : NotificationHandler<TodoAddedNotification> {
    override suspend fun handle(notification: TodoAddedNotification) {
        println("[Sync] Syncing todo '${notification.todo.id}' to remote...")
    }
}

// ── Store ─────────────────────────────────────────────────────────────────────

class TodoStore {
    private val todos = mutableMapOf<String, Todo>()
    fun save(todo: Todo) { todos[todo.id] = todo }
    fun findById(id: String): Todo? = todos[id]
}

// ── Registrar ─────────────────────────────────────────────────────────────────

class TodoRegistrar(private val store: TodoStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +AddTodoHandler(store)
            +GetTodoHandler(store)
            +LogTodoAddedHandler()
            +SyncTodoAddedHandler()
        }
    }
}

// ── Entry point ───────────────────────────────────────────────────────────────

fun main() = runBlocking {
    val store = TodoStore()
    val mediator = MediatorFactory.create(registrars = listOf(TodoRegistrar(store)))

    val todo = mediator.send(AddTodoCommand(id = "1", title = "Buy groceries"))
    println("Created: $todo")

    val found = mediator.send(GetTodoQuery(id = "1"))
    println("Found: $found")

    val missing = mediator.send(GetTodoQuery(id = "999"))
    println("Missing: $missing")
}
