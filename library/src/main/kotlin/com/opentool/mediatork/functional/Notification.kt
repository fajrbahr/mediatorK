package com.opentool.mediatork.com.opentool.mediatork.functional

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
 * - [ParallelNotificationPublisher]-all handlers run concurrently (default)
 * - [SequentialNotificationPublisher]-handlers run one-by-one, stops on first error
 * - [com.opentool.mediatork.com.opentool.mediatork.functical.mediator.ContinueOnExceptionNotificationPublisher]-all handlers run even if some fail, errors collected into [AggregateException]
 * - [FireAndForgetNotificationPublisher]-returns immediately, handlers run in the background
 *
 * @see Request for operations that expect exactly one handler and a response.
 */
interface Notification
