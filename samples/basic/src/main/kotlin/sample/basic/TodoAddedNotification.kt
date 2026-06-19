package sample.basic

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler

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
