package com.opentool.mediatork.com.opentool.mediatork.functional

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun FireAndForgetNotificationPublisher(scope: CoroutineScope): com.opentool.mediatork.com.opentool.mediatork.functional.NotificationPublisher = object :
    com.opentool.mediatork.com.opentool.mediatork.functional.NotificationPublisher {
    override suspend fun <T : com.opentool.mediatork.com.opentool.mediatork.functional.Notification> publish(notification: T, handlers: List<com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler<T>>) {
        handlers.forEach { handler ->
            scope.launch { handler(notification) }
        }
    }
}
