package com.fajrbahr.mediatork

class ContinueOnExceptionNotificationPublisher : NotificationPublisher {
    override suspend fun <T : Notification> publish(
        notification: T,
        handlers: List<NotificationHandler<T>>
    ) {
        val errors = mutableListOf<Throwable>()
        handlers.forEach { handler ->
            try {
                handler.handle(notification)
            } catch (e: Throwable) {
                errors.add(e)
            }
        }
        if (errors.isNotEmpty()) throw AggregateException(
            errors
        )
    }
}
