package sample.basic

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.handler.handler

data class AddTodoCommand(val id: String, val title: String) : Request<Todo>

fun addTodoHandler(store: TodoStore) = handler<AddTodoCommand, Todo> { request ->
    val todo = Todo(id = request.id, title = request.title)
    store.save(todo)
    publish(TodoAddedNotification(todo))
    todo
}
