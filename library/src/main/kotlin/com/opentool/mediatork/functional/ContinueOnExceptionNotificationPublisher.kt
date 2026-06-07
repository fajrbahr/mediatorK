package com.opentool.mediatork.com.opentool.mediatork.functional

import com.opentool.mediatork.com.opentool.mediatork.functional.AggregateException
import com.opentool.mediatork.com.opentool.mediatork.functional.Notification
import com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler
import com.opentool.mediatork.com.opentool.mediatork.functional.NotificationPublisher

val ContinueOnExceptionNotificationPublisher: com.opentool.mediatork.com.opentool.mediatork.functional.NotificationPublisher = object :
    com.opentool.mediatork.com.opentool.mediatork.functional.NotificationPublisher {
    override suspend fun <T : com.opentool.mediatork.com.opentool.mediatork.functional.Notification> publish(notification: T, handlers: List<com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler<T>>) {
        val errors = mutableListOf<Throwable>()
        handlers.forEach { handler ->
            try {
                handler(notification)
            } catch (e: Throwable) {
                errors.add(e)
            }
        }
        if (errors.isNotEmpty()) throw _root_ide_package_.com.opentool.mediatork.com.opentool.mediatork.functional.AggregateException(errors)
    }
}
