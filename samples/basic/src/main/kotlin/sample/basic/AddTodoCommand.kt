package sample.basic

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.api.RequestContext

import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationHandler

data class AddTodoCommand(val id: String, val title: String) : Request<Todo>

class AddTodoHandler(private val store: TodoStore) : RequestHandler<AddTodoCommand, Todo> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: AddTodoCommand): Todo {
        val todo = Todo(id = request.id, title = request.title)
        store.save(todo)
        mediator.publish(TodoAddedNotification(todo))
        return todo
    }
}
