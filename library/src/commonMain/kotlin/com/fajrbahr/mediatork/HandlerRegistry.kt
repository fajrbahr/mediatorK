package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.StreamRequestHandler
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.BoundValidator
import kotlin.reflect.KClass

/**
 * Central store for all handlers registered with the mediator.
 *
 * Holds mappings from request/notification/exception types to their respective
 * handlers. Populated at application startup — typically via [com.fajrbahr.mediatork.api.MediatorRegistrar]
 * implementations — and then queried at runtime by [MediatorImpl] for each
 * dispatched request or published notification.
 *
 * The inline registration functions use reified type parameters so that the
 * request/notification [KClass] is captured without reflection at the call site.
 *
 * @see MediatorFactory
 * @see com.fajrbahr.mediatork.api.MediatorRegistrar
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
     * Maps each [com.fajrbahr.mediatork.api.StreamRequest] [KClass] to its single registered [StreamRequestHandler].
     * Marked `@PublishedApi` for inline-function access.
     */
    @PublishedApi
    internal val streamHandlers: MutableMap<KClass<*>, StreamRequestHandler<*, *>> = mutableMapOf()

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
     * DSL operator that registers this [RequestValidator] via [registerValidator].
     *
     * Allows the `+validator` syntax inside a [scope] block.
     */
    inline operator fun <reified TRequest : Request<*>> RequestValidator<TRequest>.unaryPlus() {
        registerValidator(this)
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

    @PublishedApi
    internal val validators: MutableList<BoundValidator<*>> = mutableListOf()

    inline fun <reified TRequest : Request<*>> registerValidator(
        validator: RequestValidator<TRequest>,
    ): HandlerRegistry {
        validators.add(BoundValidator(TRequest::class, validator))
        return this
    }

    internal fun collectValidators(): List<BoundValidator<*>> = validators.toList()

    /** Returns `true` if a [StreamRequestHandler] is registered for [requestType]. */
    fun hasStreamHandler(requestType: KClass<*>): Boolean = streamHandlers.containsKey(requestType)

    /** Returns `true` if at least one [NotificationHandler] is registered for [notificationType]. */
    fun hasNotificationHandler(notificationType: KClass<*>): Boolean = notificationHandlers.containsKey(notificationType)

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

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveHandlerOrNull(request: TRequest): RequestHandler<TRequest, TResult>? =
        requestHandlers[request::class] as? RequestHandler<TRequest, TResult>

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

}
