package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
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

    // ── Storage ───────────────────────────────────────────────────────────────

    @PublishedApi
    internal val requestHandlers: MutableMap<KClass<*>, RequestHandler<*, *>> = mutableMapOf()

    @PublishedApi
    internal val streamHandlers: MutableMap<KClass<*>, StreamRequestHandler<*, *>> = mutableMapOf()

    @PublishedApi
    internal val notificationHandlers: MutableMap<KClass<*>, MutableList<NotificationHandler<*>>> = mutableMapOf()

    @PublishedApi
    internal val validatorsHandlers: MutableMap<KClass<*>, MutableList<RequestValidator<*>>> = mutableMapOf()

    // ── Registration ──────────────────────────────────────────────────────────

    fun scope(block: HandlerRegistry.() -> Unit) = block()

    inline infix fun <reified TRequest : Request<TResult>, TResult> register(
        handler: RequestHandler<TRequest, TResult>,
    ): HandlerRegistry {
        requestHandlers[TRequest::class] = handler
        return this
    }

    inline infix fun <reified TRequest : StreamRequest<T>, T> registerStream(
        handler: StreamRequestHandler<TRequest, T>,
    ): HandlerRegistry {
        streamHandlers[TRequest::class] = handler
        return this
    }

    inline infix fun <reified T : Notification> registerNotification(
        handler: NotificationHandler<T>,
    ): HandlerRegistry {
        notificationHandlers.getOrPut(T::class) { mutableListOf() }.add(handler)
        return this
    }

    inline fun <reified TRequest : Request<*>> registerValidator(
        validator: RequestValidator<TRequest>,
    ): HandlerRegistry {
        validatorsHandlers.getOrPut(TRequest::class) { mutableListOf() }.add(validator)
        return this
    }

    // ── DSL operators ─────────────────────────────────────────────────────────

    inline operator fun <reified TRequest : Request<TResult>, TResult> RequestHandler<TRequest, TResult>.unaryPlus() =
        register(this)

    inline operator fun <reified TRequest : StreamRequest<T>, T> StreamRequestHandler<TRequest, T>.unaryPlus() =
        registerStream(this)

    inline operator fun <reified T : Notification> NotificationHandler<T>.unaryPlus() =
        registerNotification(this)

    inline operator fun <reified TRequest : Request<*>> RequestValidator<TRequest>.unaryPlus() =
        registerValidator(this)

    // ── Dynamic registration (for DI frameworks) ──────────────────────────────

    fun registerDynamic(requestClass: KClass<*>, handler: RequestHandler<*, *>): HandlerRegistry {
        requestHandlers[requestClass] = handler
        return this
    }

    fun registerStreamDynamic(requestClass: KClass<*>, handler: StreamRequestHandler<*, *>): HandlerRegistry {
        streamHandlers[requestClass] = handler
        return this
    }

    fun registerNotificationDynamic(notificationClass: KClass<*>, handler: NotificationHandler<*>): HandlerRegistry {
        notificationHandlers.getOrPut(notificationClass) { mutableListOf() }.add(handler)
        return this
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    fun hasHandler(requestType: KClass<*>): Boolean = requestHandlers.containsKey(requestType)
    fun hasStreamHandler(requestType: KClass<*>): Boolean = streamHandlers.containsKey(requestType)
    fun hasNotificationHandler(notificationType: KClass<*>): Boolean =
        notificationHandlers.containsKey(notificationType)

    fun registeredRequestTypes(): Set<KClass<*>> = requestHandlers.keys.toSet()
    fun registeredStreamRequestTypes(): Set<KClass<*>> = streamHandlers.keys.toSet()

    // ── Resolution (internal) ─────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveHandlerOrNull(request: TRequest): RequestHandler<TRequest, TResult>? =
        requestHandlers[request::class] as? RequestHandler<TRequest, TResult>

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveHandler(request: TRequest): RequestHandler<TRequest, TResult> =
        requestHandlers[request::class] as? RequestHandler<TRequest, TResult>
            ?: throw MissingHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = requestHandlers.keys.mapNotNull { it.simpleName },
            )

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : StreamRequest<T>, T> resolveStreamHandler(request: TRequest): StreamRequestHandler<TRequest, T> =
        streamHandlers[request::class] as? StreamRequestHandler<TRequest, T>
            ?: throw MissingStreamHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = streamHandlers.keys.mapNotNull { it.simpleName },
            )

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Notification> resolveNotificationHandlers(notification: T): List<NotificationHandler<T>> =
        (notificationHandlers[notification::class] ?: emptyList()) as List<NotificationHandler<T>>


}

internal fun HandlerRegistry.anyValidators(): Map<KClass<*>, List<RequestValidator<*>>> = validatorsHandlers

