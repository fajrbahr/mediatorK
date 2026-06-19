package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.PipelineBehavior.Tag
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import com.fajrbahr.mediatork.api.StreamHandlerDelegate
import com.fajrbahr.mediatork.api.StreamPipelineBehavior
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
    private val registry: HandlerRegistry,
    private val pipelineBehaviors: List<PipelineBehavior>,
    private val streamPipelineBehaviors: List<StreamPipelineBehavior>,
    private val notificationPublisher: NotificationPublishStrategy,
    private val missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
) : Mediator {

    /**
     * Resolves the handler for [request] and runs the full pipeline.
     *
     * @throws MissingHandlerException if no handler is registered for the request type.
     */
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = registry.resolveHandler(request)
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
     * @param publisher the strategy to use instead of the default [com.fajrbahr.mediatork.notification.NotificationPublishStrategy].
     */
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) {
        val handlers = registry.resolveNotificationHandlers(notification).sortedBy { it.order }
        publisher.publish(notification, handlers)
    }

    /**
     * Composes and executes the full behavior chain for a single request dispatch.
     *
     * Behaviors are grouped by [Tag] phase ([Tag.Pre] → [Tag.Default] → [Tag.Post])
     * and sorted by [PipelineBehavior.order] within each phase. Lower order is outermost
     * within a phase; [Tag.Pre] behaviors always wrap [Tag.Default], which always wrap [Tag.Post].
     *
     * @param request the incoming request.
     * @param handler the resolved handler for this request type.
     * @return the result produced by the handler (possibly transformed by behaviors).
     */
    private suspend fun <TRequest : Request<TResult>, TResult> executePipeline(
        request: TRequest,
        handler: RequestHandler<TRequest, TResult>,
    ): TResult {
        val requestContext = RequestContext()
        val active = pipelineBehaviors.filter { it.isEnabled && it.appliesTo(request) }
        val sortedPre = active.filter { it.tag == Tag.Pre }.sortedBy { it.order }
        val sortedDefault = active.filter { it.tag == Tag.Default }.sortedBy { it.order }
        // POST sorted descending so that lower order = innermost = exits first after handler
        val sortedPost = active.filter { it.tag == Tag.Post }.sortedByDescending { it.order }

        val finalDelegate: RequestHandlerDelegate<TRequest, TResult> = { req ->
            try {
                handler.handle(this@MediatorImpl, requestContext, req)
            } catch (e: Throwable) {
                val actions = registry.resolveExceptionActions(req, e)
                actions.forEach { action ->
                    try { action.execute(requestContext, req, e) } catch (_: Throwable) {}
                }
                val exHandler = registry.resolveExceptionHandler(req, e) ?: throw e
                exHandler.handle(requestContext, req, e)
            }
        }

        val withPost = sortedPost.foldRight(finalDelegate) { behavior, next ->
            { req -> behavior.process(requestContext, next, req) }
        }
        val withDefault = sortedDefault.foldRight(withPost) { behavior, next ->
            { req -> behavior.process(requestContext, next, req) }
        }
        val pipeline = sortedPre.foldRight(withDefault) { behavior, next ->
            { req -> behavior.process(requestContext, next, req) }
        }
        return pipeline(request)
    }
}
