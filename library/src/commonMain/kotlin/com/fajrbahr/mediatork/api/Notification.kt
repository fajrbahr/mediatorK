package com.fajrbahr.mediatork.api

/**
 * An event that is broadcast to zero or more handlers with no response.
 *
 * Use [Notification] when something happened and you want other parts of the
 * system to react-independently, without the publisher knowing or caring who
 * is listening. Having no handlers registered is silent (no exception).
 *
 *
 * Multiple handlers can react to the same notification:
 *
 * The publish strategy controls how handlers are invoked:
 * - [com.fajrbahr.mediatork.notification.ParallelNotificationPublisher] — all handlers run concurrently (default)
 * - [com.fajrbahr.mediatork.notification.SequentialNotificationPublisher] — handlers run one-by-one, stops on first error
 * - [com.fajrbahr.mediatork.notification.ContinueOnExceptionNotificationPublisher] — all handlers run even if some fail, errors collected into [com.fajrbahr.mediatork.AggregateException]
 * - [com.fajrbahr.mediatork.notification.FireAndForgetNotificationPublisher] — returns immediately, handlers run in the background
 *
 * @see Request for operations that expect exactly one handler and a response.
 */
interface Notification