package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.notification.*

/**
 * Base class for all exceptions thrown by the MediatorK library.
 *
 * Catching this type at the application boundary allows callers to distinguish
 * mediator infrastructure errors from domain-level exceptions.
 *
 * @param message a human-readable description of the error.
 */
open class MediatorException(message: String) : Exception(message)

/**
 * Thrown when [Sender.send] is called for a request type that has no registered handler.
 *
 * The message includes the name of the unresolved request type and, when available,
 * the list of request types that do have handlers — making misconfiguration easy to diagnose.
 *
 * @param requestTypeName simple name of the request type that could not be resolved.
 * @param registered simple names of all currently registered request types; included
 *   in the message to aid debugging.
 */
class MissingHandlerException(
    requestTypeName: String,
    registered: Collection<String> = emptyList(),
) : MediatorException(
    buildString {
        append("No handler registered for '$requestTypeName'")
        if (registered.isNotEmpty()) append(". Registered: ${registered.joinToString()}")
    }
)

/**
 * Thrown by [ThrowMissingNotificationHandlerStrategy] when [Publisher.publish] is called
 * for a notification type that has no registered handlers.
 *
 * @param notificationTypeName simple name of the notification type that had no handlers.
 */
class MissingNotificationHandlerException(
    notificationTypeName: String,
) : MediatorException("No handler registered for notification '$notificationTypeName'")

/**
 * Thrown by [ContinueOnExceptionNotificationPublisher] when one or more notification
 * handlers fail. All handler exceptions are collected and included in this single
 * aggregate, so callers can inspect every failure rather than only the first.
 *
 * @param exceptions the non-empty list of exceptions thrown by individual handlers.
 */
class AggregateException(
    exceptions: List<Throwable>,
) : MediatorException("${exceptions.size} handler(s) failed: ${exceptions.joinToString { it.message ?: it::class.simpleName.orEmpty() }}")
