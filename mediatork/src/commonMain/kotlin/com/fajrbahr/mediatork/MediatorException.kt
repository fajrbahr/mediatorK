package com.fajrbahr.mediatork

open class MediatorException(message: String) : Exception(message)

class MissingHandlerException(
    requestTypeName: String,
    registered: Collection<String> = emptyList(),
) : MediatorException(
    buildString {
        append("No handler registered for '$requestTypeName'")
        if (registered.isNotEmpty()) append(". Registered: ${registered.joinToString()}")
    }
)

class MissingStreamHandlerException(
    requestTypeName: String,
    registered: Collection<String> = emptyList(),
) : MediatorException(
    buildString {
        append("No stream handler registered for '$requestTypeName'")
        if (registered.isNotEmpty()) append(". Registered: ${registered.joinToString()}")
    }
)

class MissingNotificationHandlerException(
    notificationTypeName: String,
) : MediatorException("No handler registered for notification '$notificationTypeName'")

class AggregateException(
    exceptions: List<Throwable>,
) : MediatorException("${exceptions.size} handler(s) failed: ${exceptions.joinToString { it.message ?: it::class.simpleName.orEmpty() }}")
