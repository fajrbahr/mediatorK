package sample.basic

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.feature.notificationHandler

data class TodoAddedNotification(val todo: Todo) : Notification

val logTodoAddedHandler = notificationHandler<TodoAddedNotification> { notification ->
    println("[Log] Todo added: '${notification.todo.title}' (id=${notification.todo.id})")
}

val syncTodoAddedHandler = notificationHandler<TodoAddedNotification> { notification ->
    println("[Sync] Syncing todo '${notification.todo.id}' to remote...")
}
