package com.fajrbahr.mediatork

/**
 * An event that is broadcast to zero or more handlers with no response.
 *
 * Use [Notification] when something happened and you want other parts of the
 * system to react-independently, without the publisher knowing or caring who
 * is listening. Having no handlers registered is silent (no exception).
 *
 * ```kotlin
 * data class BookingPurchasedNotification(
 *     val bookingId: String,
 *     val amount: Double,
 * ) : Notification
 * ```
 *
 * Multiple handlers can react to the same notification:
 * ```kotlin
 * // Analytics handler tracks the purchase
 * // Email handler sends a receipt
 * // Badge handler updates the booking count
 * //-none of them respond to the publisher
 * ```
 *
 * The publish strategy controls how handlers are invoked:
 * - [com.fajrbahr.mediatork.ParallelNotificationPublisher]-all handlers run concurrently (default)
 * - [com.fajrbahr.mediatork.mediator.SequentialNotificationPublisher]-handlers run one-by-one, stops on first error
 * - [com.fajrbahr.mediatork.ContinueOnExceptionNotificationPublisher]-all handlers run even if some fail, errors collected into [com.fajrbahr.mediatork.AggregateException]
 * - [com.fajrbahr.mediatork.FireAndForgetNotificationPublisher]-returns immediately, handlers run in the background
 *
 * @see com.fajrbahr.mediatork.Request for operations that expect exactly one handler and a response.
 */
interface Notification
