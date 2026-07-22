package sample.basic

import com.fajrbahr.mediatork.MediatorBuilder

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

// ── Module ──────────────────────────────────────────────────────────────────
// The FP replacement for a MediatorRegistrar: a MediatorBuilder extension that
// registers this slice's handlers and notification listeners.

fun MediatorBuilder.todoModule(store: TodoStore) {
    handle(addTodoHandler(store))
    handle(getTodoHandler(store))
    notification(notificationHandler = logTodoAddedHandler)
    notification(notificationHandler = syncTodoAddedHandler)
}
