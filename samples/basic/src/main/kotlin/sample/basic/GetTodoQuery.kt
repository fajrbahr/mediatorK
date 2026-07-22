package sample.basic

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.api.Request

data class GetTodoQuery(val id: String) : Request<Todo?>

fun getTodoHandler(store: TodoStore): Handler<GetTodoQuery, Todo?> = { request ->
    store.findById(request.id)
}
