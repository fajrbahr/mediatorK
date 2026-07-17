package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow

internal class MediatorImpl(
    private val config: MediatorConfig,
) : Mediator {

    private fun scopeFor(context: RequestContext): HandlerScope =
        object : HandlerScope, Mediator by this {
            override val context = context
        }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = config.handlers[request::class]
            ?: config.onMissingHandler(request)

        val context = RequestContext()
        val scope = scopeFor(context)
        val finalDelegate: suspend () -> Any? = { handler(scope, request) }

        val pipeline = config.behaviors
            .filter { it.isEnabled && it.appliesTo(request) }
            .sortedBy { it.order }
            .foldRight(finalDelegate) { b, next ->
                suspend { b.process(request, context, next) }
            }
        return pipeline() as TResult
    }

    @Suppress("UNCHECKED_CAST")
    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        val handler = config.streamHandlers[request::class]
            ?: throw MissingStreamHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = config.streamHandlers.keys.mapNotNull { it.simpleName },
            )

        val context = RequestContext()
        val scope = scopeFor(context)
        val finalDelegate: () -> Flow<*> = { handler(scope, request) }

        val pipeline = config.streamBehaviors
            .filter { it.isEnabled && it.appliesTo(request) }
            .sortedBy { it.order }
            .foldRight(finalDelegate) { b, next ->
                { b.process(request, context, next) }
            }
        return pipeline() as Flow<T>
    }

    override suspend fun <T : Notification> publish(notification: T) {
        val listeners = orderedListeners(notification)
        if (listeners.isEmpty()) {
            config.onMissingNotificationHandler(notification)
            return
        }
        config.notificationPublisher.publish(notification, listeners)
    }

    override suspend fun <T : Notification> publish(notification: T, strategy: NotificationPublishStrategy) {
        val listeners = orderedListeners(notification)
        if (listeners.isEmpty()) {
            config.onMissingNotificationHandler(notification)
            return
        }
        strategy.publish(notification, listeners)
    }

    private fun orderedListeners(notification: Notification): List<suspend (Any) -> Unit> =
        config.notificationListeners[notification::class]
            ?.sortedBy { it.first }
            ?.map { it.second }
            ?: emptyList()
}
