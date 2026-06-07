package com.fajrbahr.mediatork

/**
 *  Why a new RequestContext per request and not a class-level property?
 *   MediatorImpl is a singleton-if RequestContext were a shared property,
 *   concurrent send() calls (e.g. two ViewModels firing at the same time)
 *   would overwrite each other's locale, auth token, or any other bag value.
 *   Creating it here scopes the context to this single pipeline execution,
 *   the same way ASP.NET Core scopes HttpContext per HTTP request.
 *   Pipeline behaviors (like LocalePipelineBehavior) populate it,
 *   and handlers consume it-all within one isolated request lifecycle.
 */
internal class MediatorImpl(
    private val registry: HandlerRegistry,
    private val pipelineBehaviors: List<PipelineBehavior>,
    private val preProcessors: List<RequestPreProcessor>,
    private val postProcessors: List<RequestPostProcessor>,
    private val notificationPublisher: NotificationPublisher,
) : Mediator {

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = registry.resolveHandler(request)
        return executePipeline(request, handler)
    }

    override suspend fun <T : Notification> publish(notification: T) {
        val handlers = registry.resolveNotificationHandlers(notification)
        notificationPublisher.publish(notification, handlers)
    }

    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher) {
        val handlers = registry.resolveNotificationHandlers(notification)
        publisher.publish(notification, handlers)
    }

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

            val result = handler.handle(this@MediatorImpl, requestContext, req)

            sortedPost.forEach { it.process(requestContext, req, result) }
            result
        }

        val pips: RequestHandlerDelegate<TRequest, TResult> = sorted.foldRight(finalDelegate) { processer, next ->
            { req -> processer.process(requestContext, next, req) }
        }

        return pips(request)
    }
}
