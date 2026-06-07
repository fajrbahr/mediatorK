package com.opentool.mediatork.com.opentool.mediatork

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FireAndForgetNotificationPublisher(
    private val scope: CoroutineScope,
) : NotificationPublisher {
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        handlers.forEach { handler ->
            scope.launch { handler.handle(notification) }
        }
    }
}
