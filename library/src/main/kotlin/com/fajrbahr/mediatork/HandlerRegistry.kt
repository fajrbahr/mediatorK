package com.fajrbahr.mediatork

import kotlin.reflect.KClass


class HandlerRegistry {

    @PublishedApi
    internal val requestHandlers: MutableMap<KClass<*>, RequestHandler<*, *>> = mutableMapOf()

    @PublishedApi
    internal val notificationHandlers: MutableMap<KClass<*>, MutableList<NotificationHandler<*>>> = mutableMapOf()

    @PublishedApi
    internal val exceptionHandlers: MutableMap<KClass<*>, MutableList<Pair<KClass<out Throwable>, RequestExceptionHandler<*, *, *>>>> =
        mutableMapOf()

    fun scope(block: HandlerRegistry.() -> Unit) {
        block()
    }

    inline infix fun <reified TReq : Request<TRes>, TRes> register(
        handler: RequestHandler<TReq, TRes>
    ): HandlerRegistry {
        requestHandlers[TReq::class] = handler
        return this
    }

    inline infix fun <reified T : Notification> registerNotification(
        handler: NotificationHandler<T>,
    ): HandlerRegistry {
        notificationHandlers.getOrPut(T::class) { mutableListOf() }.add(handler)
        return this
    }

    fun <TReq : Request<TRes>, TRes, TEx : Throwable> registerExceptionHandler(
        requestClass: KClass<TReq>,
        exceptionClass: KClass<TEx>,
        handler: RequestExceptionHandler<TReq, TRes, TEx>,
    ): HandlerRegistry {
        exceptionHandlers.getOrPut(requestClass) { mutableListOf() }.add(Pair(exceptionClass, handler))
        return this
    }

    inline operator fun <reified TReq : Request<TRes>, TRes> RequestHandler<TReq, TRes>.unaryPlus() {
        register(this)
    }

    inline operator fun <reified T : Notification> NotificationHandler<T>.unaryPlus() {
        registerNotification(this)
    }

    fun hasHandler(requestType: KClass<*>): Boolean = requestHandlers.containsKey(requestType)

    @Suppress("UNCHECKED_CAST")
    internal fun <TReq : Request<TRes>, TRes> resolveHandler(request: TReq): RequestHandler<TReq, TRes> =
        requestHandlers[request::class] as? RequestHandler<TReq, TRes>
            ?: throw MissingHandlerException(
                requestTypeName = request::class.simpleName ?: "Unknown",
                registered = requestHandlers.keys.mapNotNull { it.simpleName },
            )

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Notification> resolveNotificationHandlers(notification: T): List<NotificationHandler<T>> =
        (notificationHandlers[notification::class] ?: emptyList()) as List<NotificationHandler<T>>

    @Suppress("UNCHECKED_CAST")
    internal fun <TReq : Request<TRes>, TRes> resolveExceptionHandler(
        request: TReq,
        exception: Throwable,
    ): RequestExceptionHandler<TReq, TRes, Throwable>? {
        val entries = exceptionHandlers[request::class] ?: return null
        val entry = entries.firstOrNull { (exClass, _) -> exClass.isInstance(exception) }
        return entry?.second as? RequestExceptionHandler<TReq, TRes, Throwable>
    }
}
