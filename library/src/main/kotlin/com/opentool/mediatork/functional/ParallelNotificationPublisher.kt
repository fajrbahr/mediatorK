package com.opentool.mediatork.com.opentool.mediatork.functional

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

val ParallelNotificationPublisher: NotificationPublisher = object : NotificationPublisher {
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        coroutineScope {
            handlers.map { handler -> launch { handler(notification) } }.joinAll()
        }
    }
}
