package com.opentool.mediatork.com.opentool.mediatork.functional

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

class InvalidMessageException(message: String) :
    MediatorException("Invalid mediator message: $message")

class AggregateException(
    exceptions: List<Throwable>,
) : MediatorException("${exceptions.size} handler(s) failed: ${exceptions.joinToString { it.message ?: it::class.simpleName.orEmpty() }}")
