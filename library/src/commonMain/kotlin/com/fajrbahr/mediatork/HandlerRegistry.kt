package com.fajrbahr.mediatork

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
     * Marked `@PublishedApi` so that inline functions can access this internal map.
     * Still marked internal — IDEs hide it from autocomplete, it doesn't appear in generated docs, and it signals "don't use this directly"
     * Consumers can technically access it if they try, but it's clearly marked as not intended for use
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
     * are all invoked in registration order (subject to the active [NotificationPublisher]).
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
     * Returns `true` if a [RequestHandler] is registered for [requestType].
     *
     * @param requestType the [KClass] of the request to check.
     */
    fun hasHandler(requestType: KClass<*>): Boolean = requestHandlers.containsKey(requestType)

    /** Returns the set of all request types that have a registered handler. */
    fun registeredRequestTypes(): Set<KClass<*>> = requestHandlers.keys.toSet()

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
