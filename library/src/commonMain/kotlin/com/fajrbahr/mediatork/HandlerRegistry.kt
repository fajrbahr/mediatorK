package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestExceptionAction
import com.fajrbahr.mediatork.handler.RequestExceptionHandler
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.handler.StreamRequestHandler
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.validator.RequestValidator
import kotlin.reflect.KClass

/**
 * Central store for all handlers registered with the mediator.
 *
 * Holds mappings from request/notification/exception types to their respective
 * handlers. Populated at application startup — typically via [MediatorRegistrar]
 * implementations — and then queried at runtime by [MediatorImpl] for each
 * dispatched request or published notification.
 *
 * The inline registration functions use reified type parameters so that the
 * request/notification [KClass] is captured without reflection at the call site.
 *
 * @see MediatorFactory
 * @see MediatorRegistrar
 */
class HandlerRegistry {

    /**
     * Maps each request [KClass] to its single registered [RequestHandler].
     * `@PublishedApi` allows inline functions to access this map; `internal` keeps it
     * out of the public API surface.
     */
    @PublishedApi
    internal val requestHandlers: MutableMap<KClass<*>, RequestHandler<*, *>> = mutableMapOf()

    /**
     * Maps each notification [KClass] to the ordered list of [NotificationHandler]s
     * registered for it. Marked `@PublishedApi` for inline-function access.
     */
    @PublishedApi
    internal val notificationHandlers: MutableMap<KClass<*>, MutableList<NotificationHandler<*>>> = mutableMapOf()

    /**
     * Maps each request [KClass] to a list of `(exception KClass, handler)` pairs,
     * enabling per-request exception handling. Marked `@PublishedApi` for inline-function access.
     */
    @PublishedApi
    internal val exceptionHandlers: MutableMap<KClass<*>, MutableList<Pair<KClass<out Throwable>, RequestExceptionHandler<*, *, *>>>> =
        mutableMapOf()

    /**
     * Maps each [StreamRequest] [KClass] to its single registered [StreamRequestHandler].
     * Marked `@PublishedApi` for inline-function access.
     */
    @PublishedApi
    internal val streamHandlers: MutableMap<KClass<*>, StreamRequestHandler<*, *>> = mutableMapOf()

    /**
     * Maps each request [KClass] to a list of `(exception KClass, action)` pairs for
     * side-effect-only exception hooks. Unlike [exceptionHandlers], these do not provide
     * a fallback response — they run their side effect and then let the exception propagate.
     * Marked `@PublishedApi` for inline-function access.
     */
    @PublishedApi
    internal val exceptionActions: MutableMap<KClass<*>, MutableList<Pair<KClass<out Throwable>, RequestExceptionAction<*, *>>>> =
        mutableMapOf()

    /**
     * Groups a set of registrations into a logical block for readability.
     * Has no effect beyond executing [block] with this registry as the receiver.
     *
     * @param block lambda in which handlers are registered.
     */
    fun scope(block: HandlerRegistry.() -> Unit) {
        block()
    }

    /**
     * Registers [handler] as the sole handler for request type [TRequest].
     *
     * If a handler is already registered for [TRequest], it is silently replaced.
     *
     * @param TRequest the request type to associate with [handler].
     * @param TResult the response type produced by the handler.
     * @param handler the handler to register.
     * @return this registry, for chaining.
     */
    inline infix fun <reified TRequest : Request<TResult>, TResult> register(
        handler: RequestHandler<TRequest, TResult>
    ): HandlerRegistry {
        requestHandlers[TRequest::class] = handler
        return this
    }

    /**
     * Appends [handler] to the list of handlers for notification type [T].
     *
     * Multiple handlers may be registered for the same notification type; they
     * are all invoked in registration order (subject to the active [NotificationPublishStrategy]).
     *
     * @param T the notification type to associate with [handler].
     * @param handler the handler to register.
     * @return this registry, for chaining.
     */
    inline infix fun <reified T : Notification> registerNotification(
        handler: NotificationHandler<T>,
    ): HandlerRegistry {
        notificationHandlers.getOrPut(T::class) { mutableListOf() }.add(handler)
        return this
    }

    /**
     * Registers [handler] to intercept exceptions of type [TEx] thrown while
     * handling requests of type [TRequest].
     *
     * If multiple exception handlers are registered for the same request type,
     * the first one whose exception class [KClass.isInstance] matches the thrown
     * exception is used.
     *
     * @param TRequest the request type this handler guards.
     * @param TResult the response type; must match [TRequest]'s response parameter.
     * @param TEx the exception type this handler intercepts.
     * @param requestClass [KClass] of the request type.
     * @param exceptionClass [KClass] of the exception type.
     * @param handler the exception handler to register.
     * @return this registry, for chaining.
     */
    fun <TRequest : Request<TResult>, TResult, TEx : Throwable> registerExceptionHandler(
        requestClass: KClass<TRequest>,
        exceptionClass: KClass<TEx>,
        handler: RequestExceptionHandler<TRequest, TResult, TEx>,
    ): HandlerRegistry {
        exceptionHandlers.getOrPut(requestClass) { mutableListOf() }.add(Pair(exceptionClass, handler))
        return this
    }

    /**
     * Registers [action] to run a side effect when an exception of type [TEx] is thrown
     * while handling requests of type [TRequest].
     *
     * Unlike [registerExceptionHandler], this does **not** provide a fallback response.
     * The action runs its side effect (logging, telemetry, alerting), then the exception
     * continues propagating — either to a [RequestExceptionHandler] if one is registered,
     * or up to the caller.
     *
     * Multiple actions may be registered for the same `(request type, exception type)` pair;
     * all matching actions execute in registration order.
     *
     * @param TRequest the request type this action monitors.
     * @param TEx the exception type this action reacts to.
     * @param requestClass [KClass] of the request type.
     * @param exceptionClass [KClass] of the exception type.
     * @param action the exception action to register.
     * @return this registry, for chaining.
     */
    fun <TRequest : Request<*>, TEx : Throwable> registerExceptionAction(
        requestClass: KClass<TRequest>,
        exceptionClass: KClass<TEx>,
        action: RequestExceptionAction<TRequest, TEx>,
    ): HandlerRegistry {
        exceptionActions.getOrPut(requestClass) { mutableListOf() }.add(Pair(exceptionClass, action))
        return this
    }

    /**
     * Registers [handler] as the sole stream handler for request type [TRequest].
     *
     * If a handler is already registered for [TRequest], it is silently replaced.
     *
     * @param TRequest the stream request type to associate with [handler].
     * @param T the type of each item emitted by the flow.
     * @param handler the stream handler to register.
     * @return this registry, for chaining.
     */
    inline infix fun <reified TRequest : StreamRequest<T>, T> registerStream(
        handler: StreamRequestHandler<TRequest, T>,
    ): HandlerRegistry {
        streamHandlers[TRequest::class] = handler
        return this
    }

    /**
     * DSL operator that registers this [RequestHandler] via [register].
     *
     * Allows the `+handler` syntax inside a [scope] block.
     */
    inline operator fun <reified TRequest : Request<TResult>, TResult> RequestHandler<TRequest, TResult>.unaryPlus() {
        register(this)
    }

    /**
     * DSL operator that registers this [NotificationHandler] via [registerNotification].
     *
     * Allows the `+handler` syntax inside a [scope] block.
     */
    inline operator fun <reified T : Notification> NotificationHandler<T>.unaryPlus() {
        registerNotification(this)
    }

    /**
     * DSL operator that registers this [StreamRequestHandler] via [registerStream].
     *
     * Allows the `+handler` syntax inside a [scope] block.
     */
    inline operator fun <reified TRequest : StreamRequest<T>, T> StreamRequestHandler<TRequest, T>.unaryPlus() {
        registerStream(this)
    }

    /**
     * Registers [handler] for [requestClass] without a reified type parameter.
     *
     * Intended for DI-framework integrations (e.g. Koin, Hilt) that discover
     * handlers at runtime and therefore cannot supply a compile-time reified type.
     * The caller is responsible for ensuring [requestClass] matches the handler's
     * actual [TRequest] type parameter.
     */
    @Suppress("UNCHECKED_CAST")
    fun registerDynamic(requestClass: KClass<*>, handler: RequestHandler<*, *>): HandlerRegistry {
        requestHandlers[requestClass] = handler
        return this
    }

    /**
     * Appends [handler] for [notificationClass] without a reified type parameter.
     *
     * @see registerDynamic
     */
    fun registerNotificationDynamic(notificationClass: KClass<*>, handler: NotificationHandler<*>): HandlerRegistry {
        notificationHandlers.getOrPut(notificationClass) { mutableListOf() }.add(handler)
        return this
    }

    /**
     * Registers [handler] for [requestClass] without a reified type parameter.
     *
     * @see registerDynamic
     */
    fun registerStreamDynamic(requestClass: KClass<*>, handler: StreamRequestHandler<*, *>): HandlerRegistry {
        streamHandlers[requestClass] = handler
        return this
    }

    /**
     * Returns `true` if a [RequestHandler] is registered for [requestType].
     *
     * @param requestType the [KClass] of the request to check.
     */
    fun hasHandler(requestType: KClass<*>): Boolean = requestHandlers.containsKey(requestType)

    /** Returns the set of all request types that have a registered handler. */
    fun registeredRequestTypes(): Set<KClass<*>> = requestHandlers.keys.toSet()

    /** Returns all [RequestValidator]s declared on every registered handler via [RequestHandler.validators]. */
    internal fun collectValidators(): List<RequestValidator<*>> =
        requestHandlers.values.flatMap { it.validators() }

    /** Returns `true` if a [StreamRequestHandler] is registered for [requestType]. */
    fun hasStreamHandler(requestType: KClass<*>): Boolean = streamHandlers.containsKey(requestType)

    /** Returns the set of all stream request types that have a registered handler. */
    fun registeredStreamRequestTypes(): Set<KClass<*>> = streamHandlers.keys.toSet()

    /**
     * Looks up and returns the handler registered for [request]'s runtime type.
     *
     * @throws MissingHandlerException if no handler is registered for the request type.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveHandler(request: TRequest): RequestHandler<TRequest, TResult> =
        requestHandlers[request::class] as? RequestHandler<TRequest, TResult>
            ?: throw MissingHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = requestHandlers.keys.mapNotNull { it.simpleName },
            )

    /**
     * Returns all notification handlers registered for [notification]'s runtime type.
     * Returns an empty list if none are registered.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Notification> resolveNotificationHandlers(notification: T): List<NotificationHandler<T>> =
        (notificationHandlers[notification::class] ?: emptyList()) as List<NotificationHandler<T>>

    /**
     * Looks up and returns the stream handler registered for [request]'s runtime type.
     *
     * @throws MissingStreamHandlerException if no handler is registered for the request type.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : StreamRequest<T>, T> resolveStreamHandler(request: TRequest): StreamRequestHandler<TRequest, T> =
        streamHandlers[request::class] as? StreamRequestHandler<TRequest, T>
            ?: throw MissingStreamHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = streamHandlers.keys.mapNotNull { it.simpleName },
            )

    /**
     * Returns all exception actions registered for [request]'s type whose exception
     * class matches [exception] via [KClass.isInstance], in registration order.
     *
     * Unlike [resolveExceptionHandler], this returns all matching actions rather than
     * just the first — all of them are executed as side effects.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<*>> resolveExceptionActions(
        request: TRequest,
        exception: Throwable,
    ): List<RequestExceptionAction<TRequest, Throwable>> {
        val entries = exceptionActions[request::class] ?: return emptyList()
        return entries
            .filter { (exClass, _) -> exClass.isInstance(exception) }
            .map { (_, action) -> action as RequestExceptionAction<TRequest, Throwable> }
    }

    /**
     * Finds the first exception handler registered for [request]'s type whose
     * exception class matches [exception] via [KClass.isInstance].
     *
     * @return the matching handler cast to the correct generic types, or `null` if
     *   none is registered for the combination of request type and exception type.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveExceptionHandler(
        request: TRequest,
        exception: Throwable,
    ): RequestExceptionHandler<TRequest, TResult, Throwable>? {
        val entries = exceptionHandlers[request::class] ?: return null
        val entry = entries.firstOrNull { (exClass, _) -> exClass.isInstance(exception) }
        return entry?.second as? RequestExceptionHandler<TRequest, TResult, Throwable>
    }
}
