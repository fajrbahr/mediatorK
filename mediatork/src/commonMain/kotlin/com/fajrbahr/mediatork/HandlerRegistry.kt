@file:Suppress("TooManyFunctions")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.StreamFeature
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

    @PublishedApi
    internal val pipelineBehaviors: MutableList<PipelineBehavior> = mutableListOf()

    @PublishedApi
    internal val streamPipelineBehaviors: MutableList<StreamPipelineBehavior> = mutableListOf()

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers [handler] for request type [TRequest], replacing any previously registered handler.
     * Use the `+handler` DSL operator as a shorter alternative.
     */
    inline infix fun <reified TRequest : Request<TResult>, TResult> register(
        handler: RequestHandler<TRequest, TResult>,
    ): HandlerRegistry {
        requestHandlers[TRequest::class] = handler
        return this
    }

    /**
     * Registers [handler] for stream request type [TRequest], replacing any previously registered handler.
     * Use the `+handler` DSL operator as a shorter alternative.
     */
    inline infix fun <reified TRequest : StreamRequest<T>, T> registerStream(
        handler: StreamRequestHandler<TRequest, T>,
    ): HandlerRegistry {
        streamHandlers[TRequest::class] = handler
        return this
    }

    /**
     * Appends [handler] to the list of handlers for notification type [T].
     * Multiple handlers for the same type are all invoked according to the active
     * [com.fajrbahr.mediatork.notification.NotificationPublishStrategy].
     */
    inline infix fun <reified T : Notification> registerNotification(
        handler: NotificationHandler<T>,
    ): HandlerRegistry {
        notificationHandlers.getOrPut(T::class) { mutableListOf() }.add(handler)
        return this
    }

    /**
     * Appends [validator] to the list of validators for request type [TRequest].
     * Validators are run by [com.fajrbahr.mediatork.validator.ValidationBehavior] before the handler.
     */
    inline fun <reified TRequest : Request<*>> registerValidator(
        validator: RequestValidator<TRequest>,
    ): HandlerRegistry {
        validatorsHandlers.getOrPut(TRequest::class) { mutableListOf() }.add(validator)
        return this
    }

    // ── Feature registration ──────────────────────────────────────────────────

    /** Registers a [Feature]'s handler, validators, notification handlers, and behaviors in one step. */
    inline fun <reified TRequest : Request<TResult>, TResult> registerFeature(
        feature: Feature<TRequest, TResult>,
    ): HandlerRegistry {
        register(feature.handler)
        feature.validators.forEach { registerValidator(it) }
        feature.notifications.forEach { registration ->
            notificationHandlers.getOrPut(registration.notificationClass) { mutableListOf() }
                .add(registration.handler)
        }
        pipelineBehaviors += feature.behaviors
        streamPipelineBehaviors += feature.streamBehaviors
        return this
    }

    /** Registers a [StreamFeature]'s stream handler and notification handlers in one step. */
    inline fun <reified TRequest : StreamRequest<T>, T> registerStreamFeature(
        feature: StreamFeature<TRequest, T>,
    ): HandlerRegistry {
        registerStream(feature.handler)
        feature.notifications.forEach { registration ->
            notificationHandlers.getOrPut(registration.notificationClass) { mutableListOf() }
                .add(registration.handler)
        }
        return this
    }

    // ── Dynamic registration (for DI frameworks) ──────────────────────────────

    /**
     * Registers [handler] for [requestClass] without a reified type parameter.
     * Intended for DI frameworks (Koin, Hilt) that resolve handlers at runtime via reflection.
     */
    internal fun registerDynamic(requestClass: KClass<*>, handler: RequestHandler<*, *>): HandlerRegistry {
        requestHandlers[requestClass] = handler
        return this
    }

    /**
     * Registers [handler] for [requestClass] without a reified type parameter.
     * Intended for DI frameworks that resolve stream handlers at runtime via reflection.
     */
    internal fun registerStreamDynamic(requestClass: KClass<*>, handler: StreamRequestHandler<*, *>): HandlerRegistry {
        streamHandlers[requestClass] = handler
        return this
    }

    /**
     * Appends [handler] to the notification handler list for [notificationClass] without a reified type parameter.
     * Intended for DI frameworks that resolve notification handlers at runtime via reflection.
     */
    internal fun registerNotificationDynamic(
        notificationClass: KClass<*>,
        handler: NotificationHandler<*>
    ): HandlerRegistry {
        notificationHandlers.getOrPut(notificationClass) { mutableListOf() }.add(handler)
        return this
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns `true` if a request handler is registered for [requestType]. */
    fun hasHandler(requestType: KClass<*>): Boolean = requestHandlers.containsKey(requestType)

    /** Returns `true` if a stream handler is registered for [requestType]. */
    fun hasStreamHandler(requestType: KClass<*>): Boolean = streamHandlers.containsKey(requestType)

    /** Returns `true` if at least one notification handler is registered for [notificationType]. */
    fun hasNotificationHandler(notificationType: KClass<*>): Boolean =
        notificationHandlers.containsKey(notificationType)

    /** Returns an immutable snapshot of all registered request types. */
    fun registeredRequestTypes(): Set<KClass<*>> = requestHandlers.keys.toSet()

    /** Returns an immutable snapshot of all registered stream request types. */
    fun registeredStreamRequestTypes(): Set<KClass<*>> = streamHandlers.keys.toSet()

    // ── Resolution (internal) ─────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveHandlerOrNull(
        request: TRequest,
    ): RequestHandler<TRequest, TResult>? =
        requestHandlers[request::class] as? RequestHandler<TRequest, TResult>

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : Request<TResult>, TResult> resolveHandler(
        request: TRequest,
    ): RequestHandler<TRequest, TResult> =
        requestHandlers[request::class] as? RequestHandler<TRequest, TResult>
            ?: throw MissingHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = requestHandlers.keys.mapNotNull { it.simpleName },
            )

    @Suppress("UNCHECKED_CAST")
    internal fun <TRequest : StreamRequest<T>, T> resolveStreamHandler(
        request: TRequest,
    ): StreamRequestHandler<TRequest, T> =
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

