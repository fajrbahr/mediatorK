package com.fajrbahr.mediatork

/**
 * A no-op [Mediator] for use in tests.
 *
 * [send] always throws [NotImplementedError] unless you override it in an anonymous object.
 * [publish] does nothing.
 *
 * ```kotlin
 * val mediator = object : DumpMediator() {
 *     override suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes {
 *         @Suppress("UNCHECKED_CAST")
 *         return OrderResult(orderId = "test-1") as TRes
 *     }
 * }
 * ```
 */
open class DumpMediator : Mediator {
    override suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes =
        throw NotImplementedError("DumpMediator.send not implemented for ${request::class.simpleName}")

    override suspend fun <T : Notification> publish(notification: T) = Unit

    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher) = Unit
}
