package com.opentool.mediatork.com.opentool.mediatork.functional

import kotlin.reflect.KClass


class HandlerRegistry {
    @PublishedApi
    internal val requestHandlers: MutableMap<KClass<*>, com.opentool.mediatork.com.opentool.mediatork.functional.RequestHandler<*, *>> = mutableMapOf()

    @PublishedApi
    internal val notificationHandlers: MutableMap<KClass<*>, MutableList<com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler<*>>> = mutableMapOf()

    // Keyed by request class; each entry pairs exception class with its handler.
    @PublishedApi
    internal val exceptionHandlers: MutableMap<KClass<*>, MutableList<Pair<KClass<out Throwable>, com.opentool.mediatork.com.opentool.mediatork.functional.RequestExceptionHandler<*, *, *>>>> = mutableMapOf()

    fun <TReq : com.opentool.mediatork.com.opentool.mediatork.functional.Request<TRes>, TRes> register(
        clazz: KClass<TReq>,
        handler: com.opentool.mediatork.com.opentool.mediatork.functional.RequestHandler<TReq, TRes>,
    ): HandlerRegistry {
        requestHandlers[clazz] = handler
        return this
    }

    fun scope(register: HandlerRegistry.() -> Unit) {
        register()
    }

    fun <T : com.opentool.mediatork.com.opentool.mediatork.functional.Notification> registerNotification(
        clazz: KClass<T>,
        handler: com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler<T>,
    ): HandlerRegistry {
        notificationHandlers.getOrPut(clazz) { mutableListOf() }.add(handler)
        return this
    }

    fun <TReq : com.opentool.mediatork.com.opentool.mediatork.functional.Request<TRes>, TRes, TEx : Throwable> registerExceptionHandler(
        requestClass: KClass<TReq>,
        exceptionClass: KClass<TEx>,
        handler: com.opentool.mediatork.com.opentool.mediatork.functional.RequestExceptionHandler<TReq, TRes, TEx>,
    ): HandlerRegistry {
        exceptionHandlers.getOrPut(requestClass) { mutableListOf() }.add(Pair(exceptionClass, handler))
        return this
    }

    /**
     * Returns true if a [com.opentool.mediatork.com.opentool.mediatork.functional.RequestHandler] has been registered for [requestType].
     * Unlike [resolveHandler], this never throws-it is safe to call at startup for validation.
     */
    fun hasHandler(requestType: KClass<*>): Boolean = requestHandlers.containsKey(requestType)

    @Suppress("UNCHECKED_CAST")
    internal fun <TReq : com.opentool.mediatork.com.opentool.mediatork.functional.Request<TRes>, TRes> resolveHandler(request: TReq): com.opentool.mediatork.com.opentool.mediatork.functional.RequestHandler<TReq, TRes> =
        requestHandlers[request::class] as? com.opentool.mediatork.com.opentool.mediatork.functional.RequestHandler<TReq, TRes>
            ?: throw _root_ide_package_.com.opentool.mediatork.com.opentool.mediatork.functional.MissingHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = requestHandlers.keys.mapNotNull { it.simpleName },
            )

    @Suppress("UNCHECKED_CAST")
    internal fun <T : com.opentool.mediatork.com.opentool.mediatork.functional.Notification> resolveNotificationHandlers(notification: T): List<com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler<T>> =
        (notificationHandlers[notification::class] ?: emptyList()) as List<com.opentool.mediatork.com.opentool.mediatork.functional.NotificationHandler<T>>

    @Suppress("UNCHECKED_CAST")
    internal fun <TReq : com.opentool.mediatork.com.opentool.mediatork.functional.Request<TRes>, TRes> resolveExceptionHandler(
        request: TReq,
        exception: Throwable,
    ): com.opentool.mediatork.com.opentool.mediatork.functional.RequestExceptionHandler<TReq, TRes, Throwable>? {
        val entries = exceptionHandlers[request::class] ?: return null
        val entry = entries.firstOrNull { (exClass, _) -> exClass.isInstance(exception) }
        return entry?.second as? RequestExceptionHandler<TReq, TRes, Throwable>
    }
}
