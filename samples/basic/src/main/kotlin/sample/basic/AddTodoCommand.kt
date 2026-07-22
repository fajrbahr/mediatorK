package sample.basic

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.api.Request

data class AddTodoCommand(val id: String, val title: String) : Request<Todo>

/** Persists the todo and fans out a [TodoAddedNotification]; `publish` comes from the handler scope. */
fun addTodoHandler(store: TodoStore): Handler<AddTodoCommand, Todo> = { request ->
    val todo = Todo(id = request.id, title = request.title)
    store.save(todo)
    publish(TodoAddedNotification(todo))
    todo
}
