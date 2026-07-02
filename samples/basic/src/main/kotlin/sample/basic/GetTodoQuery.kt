package sample.basic

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

data class GetTodoQuery(val id: String) : Request<Todo?>

class GetTodoHandler(private val store: TodoStore) : RequestHandler<GetTodoQuery, Todo?> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: GetTodoQuery): Todo? =
        store.findById(request.id)
}
