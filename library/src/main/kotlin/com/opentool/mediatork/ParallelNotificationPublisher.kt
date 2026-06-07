package com.opentool.mediatork.com.opentool.mediatork

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class ParallelNotificationPublisher : NotificationPublisher {
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        coroutineScope {
            handlers.map { handler -> launch { handler.handle(notification) } }.joinAll()
        }
    }
}
