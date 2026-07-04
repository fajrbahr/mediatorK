package sample.basic

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.handler.handler

data class GetTodoQuery(val id: String) : Request<Todo?>

fun getTodoHandler(store: TodoStore) = handler<GetTodoQuery, Todo?> { request ->
    store.findById(request.id)
}
