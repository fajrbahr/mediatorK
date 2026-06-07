package com.opentool.mediatork.com.opentool.mediatork

class ContinueOnExceptionNotificationPublisher : com.opentool.mediatork.com.opentool.mediatork.NotificationPublisher {
    override suspend fun <T : com.opentool.mediatork.com.opentool.mediatork.Notification> publish(notification: T, handlers: List<com.opentool.mediatork.com.opentool.mediatork.NotificationHandler<T>>) {
        val errors = mutableListOf<Throwable>()
        handlers.forEach { handler ->
            try {
                handler.handle(notification)
            } catch (e: Throwable) {
                errors.add(e)
            }
        }
        if (errors.isNotEmpty()) throw AggregateException(errors)
    }
}
