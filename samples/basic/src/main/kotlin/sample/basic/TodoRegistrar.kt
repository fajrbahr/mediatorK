package sample.basic

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

// ── Domain model ──────────────────────────────────────────────────────────────

data class Todo(val id: String, val title: String, val done: Boolean = false)

// ── Store ─────────────────────────────────────────────────────────────────────

class TodoStore {
    private val todos = mutableMapOf<String, Todo>()
    fun save(todo: Todo) {
        todos[todo.id] = todo
    }

    fun findById(id: String): Todo? = todos[id]
}

// ── Registrar ─────────────────────────────────────────────────────────────────

class TodoRegistrar(private val store: TodoStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register AddTodoHandler(store)
        registry register GetTodoHandler(store)
        registry registerNotification LogTodoAddedHandler()
        registry registerNotification SyncTodoAddedHandler()
    }
}