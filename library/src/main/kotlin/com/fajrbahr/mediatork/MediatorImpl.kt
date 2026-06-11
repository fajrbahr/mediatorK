package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationHandler
import com.fajrbahr.mediatork.notification.NotificationPublisher
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate

/**
 * Default [Mediator] implementation produced by [MediatorFactory.create].
 *
 * Intended to be used as an application-wide singleton. Thread-safety comes from
 * the fact that all mutable state is confined to a per-call [RequestContext] that
 * is created fresh inside [executePipeline] — concurrent `send` calls never share
 * context.
 *
 * Why a new [RequestContext] per request and not a class-level property?
 * [MediatorImpl] is a singleton — if [RequestContext] were a shared property,
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
    private val preProcessors: List<RequestPreProcessor>,
    private val postProcessors: List<RequestPostProcessor>,
    private val notificationPublisher: NotificationPublisher,
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
     * Resolves all notification handlers and delivers [notification] via the
     * default [com.fajrbahr.mediatork.notification.NotificationPublisher].
     */
    override suspend fun <T : Notification> publish(notification: T) {
        val handlers = registry.resolveNotificationHandlers(notification)
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
     * @param publisher the strategy to use instead of the default [com.fajrbahr.mediatork.notification.NotificationPublisher].
     */
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher) {
        val handlers = registry.resolveNotificationHandlers(notification)
        publisher.publish(notification, handlers)
    }

    /**
     * Composes and executes the full behavior/pre-processor/handler/post-processor chain
     * for a single request dispatch.
     *
     * Pipeline behaviors are folded right so that lower-[com.fajrbahr.mediatork.pipeline.PipelineBehavior.order] behaviors
     * are outermost (they see the request first and the response last). Pre- and
     * post-processors run inside all behaviors, immediately before and after the handler.
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
        val sorted = pipelineBehaviors.filter { it.isEnabled && it.appliesTo(request) }.sortedBy { it.order }
        val sortedPre = preProcessors.sortedBy { it.order }
        val sortedPost = postProcessors.sortedBy { it.order }

        val finalDelegate: RequestHandlerDelegate<TRequest, TResult> = { req ->
            sortedPre.forEach { it.process(requestContext, req) }
            val result = try {
                handler.handle(this@MediatorImpl, requestContext, req)
            } catch (e: Throwable) {
                val exHandler = registry.resolveExceptionHandler(req, e)
                    ?: throw e
                exHandler.handle(requestContext, req, e)
            }
            sortedPost.forEach { it.process(requestContext, req, result) }
            result
        }

        val pips: RequestHandlerDelegate<TRequest, TResult> = sorted.foldRight(finalDelegate) { processer, next ->
            { req -> processer.process(requestContext, next, req) }
        }

        return pips(request)
    }
}
