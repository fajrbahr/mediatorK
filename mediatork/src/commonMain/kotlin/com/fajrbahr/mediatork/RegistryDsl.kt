package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.StreamPipelineBehavior
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.behavior.LambdaPipelineBehavior
import com.fajrbahr.mediatork.behavior.LambdaStreamPipelineBehavior
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.handler.streamHandler
import com.fajrbahr.mediatork.notification.NotificationHandlerScope
import com.fajrbahr.mediatork.validator.LambdaRequestValidator
import com.fajrbahr.mediatork.validator.RequestValidator
import kotlinx.coroutines.flow.Flow

/**
 * DSL function to create and register a request handler inline within [HandlerRegistry.scope].
 *
 * Example:
 * ```
 * mediator.registry.scope {
 *     handler<MyRequest, MyResult> { request ->
 *         request.process()
 *     }
 * }
 * ```
 */
inline fun <reified TRequest : Request<TResult>, TResult> HandlerRegistry.handler(
    noinline block: suspend com.fajrbahr.mediatork.handler.HandlerScope.(TRequest) -> TResult,
): HandlerRegistry = register(handler(block))

/**
 * DSL function to create and register a stream handler inline within [HandlerRegistry.scope].
 *
 * Example:
 * ```
 * mediator.registry.scope {
 *     streamHandler<MyStreamRequest, String> { request ->
 *         flow { emit(request.value) }
 *     }
 * }
 * ```
 */
inline fun <reified TRequest : StreamRequest<T>, T> HandlerRegistry.streamHandler(
    noinline block: com.fajrbahr.mediatork.handler.HandlerScope.(TRequest) -> Flow<T>,
): HandlerRegistry = registerStream(streamHandler(block))

/**
 * DSL function to create and register a request validator inline within [HandlerRegistry.scope].
 *
 * Example:
 * ```
 * mediator.registry.scope {
 *     requestValidator<MyRequest> { request ->
 *         if (request.isValid) Valid else Invalid("error")
 *     }
 * }
 * ```
 */
inline fun <reified TRequest : Request<*>> HandlerRegistry.requestValidator(
    noinline block: (TRequest) -> com.fajrbahr.mediatork.validator.ValidationResult,
): HandlerRegistry = registerValidator(LambdaRequestValidator(block))

/**
 * DSL function to create and register a notification handler inline within [HandlerRegistry.scope].
 *
 * Example:
 * ```
 * mediator.registry.scope {
 *     notificationHandler<OrderCreatedEvent> { notification ->
 *         println("Order created: ${notification.orderId}")
 *     }
 * }
 * ```
 */
inline fun <reified T : Notification> HandlerRegistry.notificationHandler(
    order: Int = 0,
    noinline block: suspend NotificationHandlerScope.(T) -> Unit,
): HandlerRegistry {
    val handler = NotificationHandler { notification ->
        val scope = object : NotificationHandlerScope {
            override val mediator: Mediator? = null
        }
        scope.block(notification)
    }
    return registerNotification(handler)
}

/**
 * DSL function to create and register a pipeline behavior inline within [HandlerRegistry.scope].
 *
 * Example:
 * ```
 * mediator.registry.scope {
 *     behavior<MyRequest> { requestContext, next, request ->
 *         println("Before")
 *         val result = next(request)
 *         println("After")
 *         result
 *     }
 * }
 * ```
 */
inline fun <reified TRequest : Request<TResult>, TResult> HandlerRegistry.behavior(
    stage: com.fajrbahr.mediatork.api.Stage = com.fajrbahr.mediatork.api.Stage.Default,
    order: Int = 0,
    isEnabled: Boolean = true,
    noinline appliesTo: (TRequest) -> Boolean = { true },
    noinline block: suspend (
        requestContext: RequestContext,
        next: suspend (TRequest) -> TResult,
        request: TRequest,
    ) -> TResult,
): HandlerRegistry {
    val behavior = LambdaPipelineBehavior(
        stage = stage,
        order = order,
        isEnabled = isEnabled,
        appliesTo = appliesTo as (Any?) -> Boolean,
        process = block as suspend (RequestContext, suspend (Any?) -> Any?, Any?) -> Any?,
    )
    pipelineBehaviors += behavior
    return this
}

/**
 * DSL function to create and register a stream pipeline behavior inline within [HandlerRegistry.scope].
 *
 * Example:
 * ```
 * mediator.registry.scope {
 *     streamBehavior<MyStreamRequest, String> { requestContext, next, request ->
 *         next(request).onCompletion { println("Stream complete") }
 *     }
 * }
 * ```
 */
inline fun <reified TRequest : StreamRequest<T>, T> HandlerRegistry.streamBehavior(
    order: Int = 0,
    isEnabled: Boolean = true,
    noinline appliesTo: (TRequest) -> Boolean = { true },
    noinline block: suspend (
        requestContext: RequestContext,
        next: (TRequest) -> Flow<T>,
        request: TRequest,
    ) -> Flow<T>,
): HandlerRegistry {
    val behavior = LambdaStreamPipelineBehavior(
        order = order,
        isEnabled = isEnabled,
        appliesTo = appliesTo as (Any?) -> Boolean,
        process = block as suspend (RequestContext, (Any?) -> Flow<*>, Any?) -> Flow<*>,
    )
    streamPipelineBehaviors += behavior
    return this
}
