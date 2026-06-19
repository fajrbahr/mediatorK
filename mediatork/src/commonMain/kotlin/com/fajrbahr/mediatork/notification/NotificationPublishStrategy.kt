package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.AggregateException
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * Strategy that controls how a [com.fajrbahr.mediatork.api.Notification] is delivered to its handlers.
 *
 * Prefer the companion-object constants for one-off overrides at a publish site:
 *
 * Custom implementations can enforce ordering, add tracing, or apply retry logic.
 *
 * @see Publisher
 */
interface NotificationPublishStrategy {
    /**
     * Delivers [notification] to every handler in [handlers] according to this strategy.
     *
     * @param T the concrete notification type.
     * @param notification the notification to deliver.
     * @param handlers the pre-resolved list of handlers to invoke.
     */
    suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>)

    companion object {
        /** Default strategy — handlers run concurrently; first failure cancels remaining. */
        val DEFAULT: NotificationPublishStrategy = ParallelNotificationPublisher()

        /** All handlers run concurrently; first failure cancels remaining. */
        val PARALLEL: NotificationPublishStrategy = ParallelNotificationPublisher()

        /** Handlers run one-by-one in registration order; first failure aborts. */
        val SEQUENTIAL: NotificationPublishStrategy = SequentialNotificationPublisher()

        /**
         * All handlers run regardless of individual failures; errors are collected and
         * rethrown together as [AggregateException].
         */
        val CONTINUE_ON_EXCEPTION: NotificationPublishStrategy = ContinueOnExceptionNotificationPublisher()

        /**
         * Returns a [FireAndForgetNotificationPublisher] bound to [scope].
         * Handlers are launched in [scope] and the caller returns immediately.
         */
        fun fireAndForget(scope: CoroutineScope): NotificationPublishStrategy =
            FireAndForgetNotificationPublisher(scope)
    }

    /**
     * Invokes handlers one at a time, in registration order.
     *
     * Execution stops at the first handler that throws — subsequent handlers are not
     * called and the exception propagates to the caller.
     */
    class SequentialNotificationPublisher : NotificationPublishStrategy {
        override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
            handlers.forEach { it.handle(notification) }
        }
    }

    /**
     * Invokes all handlers concurrently and waits for every one to complete.
     *
     * All handlers are launched as child coroutines within a [coroutineScope], so
     * structured concurrency guarantees apply: if any handler throws, the scope
     * cancels remaining handlers and the exception propagates to the caller.
     *
     * This is the default strategy used by [NotificationPublishStrategy.DEFAULT].
     */
    class ParallelNotificationPublisher : NotificationPublishStrategy {
        override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
            coroutineScope {
                handlers.map { handler -> launch { handler.handle(notification) } }.joinAll()
            }
        }
    }

    /**
     * Runs every handler regardless of failures and then reports all errors together.
     *
     * Each handler is invoked in sequence. Any exception thrown is caught and collected.
     * After all handlers have been attempted, if at least one failed, an [AggregateException]
     * containing every collected error is thrown.
     */
    class ContinueOnExceptionNotificationPublisher : NotificationPublishStrategy {
        override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
            val errors = mutableListOf<Throwable>()
            handlers.forEach { handler ->
                try {
                    handler.handle(notification)
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
            if (errors.isNotEmpty()) throw AggregateException(errors)
        }
    }

    /**
     * Dispatches handlers on a caller-supplied [scope] and returns immediately.
     *
     * Failures are not propagated to the caller — they surface only via the exception
     * handler of the provided [scope]. Use for non-critical side effects such as
     * analytics events or cache warm-ups.
     *
     * The [scope] must outlive the expected handler execution time.
     *
     * @param scope the coroutine scope in which handlers are launched.
     */
    class FireAndForgetNotificationPublisher(
        private val scope: CoroutineScope,
    ) : NotificationPublishStrategy {
        override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
            handlers.forEach { handler ->
                scope.launch { handler.handle(notification) }
            }
        }
    }
}
