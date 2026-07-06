package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.handler.ThrowMissingRequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler
import kotlinx.coroutines.flow.Flow

/**
 * Default [com.fajrbahr.mediatork.api.Mediator] implementation produced by [MediatorFactory.create].
 *
 * Intended to be used as an application-wide singleton. Thread-safety comes from
 * the fact that all mutable state is confined to a per-call [com.fajrbahr.mediatork.api.RequestContext] that
 * is created fresh inside [executePipeline] — concurrent `send` calls never share
 * context.
 *
 * Why a new [com.fajrbahr.mediatork.api.RequestContext] per request and not a class-level property?
 * [MediatorImpl] is a singleton — if [com.fajrbahr.mediatork.api.RequestContext] were a shared property,
 * concurrent [send] calls (e.g. two ViewModels firing at the same time) would
 * overwrite each other's locale, auth token, or any other bag value. Creating it
 * inside [executePipeline] scopes the context to a single pipeline execution, the
 * same way ASP.NET Core scopes `HttpContext` per HTTP request. Pipeline behaviors
 * (like `LocalePipelineBehavior`) populate it, and handlers consume it — all
 * within one isolated request lifecycle.
 */
internal class MediatorImpl(
    override val registry: HandlerRegistry,
    private val pipelineBehaviors: List<PipelineBehavior>,
    private val streamPipelineBehaviors: List<StreamPipelineBehavior>,
    private val notificationPublisher: NotificationPublishStrategy,
    private val missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
    private val missingRequestHandler: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler(),
) : Mediator {

    /**
     * Resolves the handler for [request] and runs the full pipeline.
     * Falls back to [missingRequestHandler] if no handler is registered.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = registry.resolveHandlerOrNull(request)
            ?: run {
                // Use missingRequestHandler if it was customised, otherwise let
                // resolveHandler throw with the full registered-types list.
                if (missingRequestHandler !is ThrowMissingRequestHandler) {
                    missingRequestHandler as RequestHandler<TRequest, TResult>
                } else {
                    registry.resolveHandler(request) // throws MissingHandlerException with registered list
                }
            }
        return executePipeline(request, handler)
    }

    /**
     * Resolves the stream handler for [request] and returns a cold [Flow].
     *
     * The handler is resolved eagerly; the flow itself is cold — nothing executes
     * until the caller collects it.
     *
     * @throws MissingStreamHandlerException if no handler is registered for the request type.
     */
    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        val handler = registry.resolveStreamHandler(request)
        val requestContext = RequestContext()
        val sorted = streamPipelineBehaviors.filter { it.isEnabled && it.appliesTo(request) }.sortedBy { it.order }

        val finalDelegate: StreamHandlerDelegate<TRequest, T> = { req ->
            handler.handle(this@MediatorImpl, requestContext, req)
        }

        val pipeline: StreamHandlerDelegate<TRequest, T> = sorted.foldRight(finalDelegate) { behavior, next ->
            { req -> behavior.process(requestContext, next, req) }
        }

        return pipeline(request)
    }

    /**
     * Resolves all notification handlers and delivers [notification] via the
     * default [com.fajrbahr.mediatork.notification.NotificationPublishStrategy].
     */
    override suspend fun <T : Notification> publish(notification: T) {
        val handlers = registry.resolveNotificationHandlers(notification).sortedBy { it.order }
        if (handlers.isEmpty()) {
            missingNotificationHandler.handle(notification)
            return
        }
        notificationPublisher.publish(notification, handlers)
    }

    /**
     * Resolves all notification handlers and delivers [notification] via the
     * supplied [publisher], overriding the default for this call only.
     *
     * @param publisher the strategy to use instead of the default
     *   [com.fajrbahr.mediatork.notification.NotificationPublishStrategy].
     */
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) {
        val handlers = registry.resolveNotificationHandlers(notification).sortedBy { it.order }
        publisher.publish(notification, handlers)
    }

    private suspend fun <TRequest : Request<TResult>, TResult> executePipeline(
        request: TRequest,
        handler: RequestHandler<TRequest, TResult>,
    ): TResult {
        val requestContext = RequestContext()
        val sorted = pipelineBehaviors
            .filter { it.isEnabled && it.appliesTo(request) }
            .sortedBy { it.order }

        val finalDelegate: RequestHandlerDelegate<TRequest, TResult> = { req ->
            handler.handle(this@MediatorImpl, requestContext, req)
        }

        val pipeline = sorted.foldRight(finalDelegate) { behavior, next ->
            { req -> behavior.process(requestContext, next, req) }
        }
        return pipeline(request)
    }
}
