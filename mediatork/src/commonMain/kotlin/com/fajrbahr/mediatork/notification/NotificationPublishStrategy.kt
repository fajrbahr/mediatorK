package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.AggregateException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

fun interface NotificationPublishStrategy {
    suspend fun publish(notification: Any, listeners: List<suspend (Any) -> Unit>)

    companion object {
        /** Runs listeners one after another, in order. Stops at the first exception. */
        val SEQUENTIAL = NotificationPublishStrategy { notification, listeners ->
            listeners.forEach { it(notification) }
        }

        /** Runs listeners concurrently and awaits them all. Propagates the first failure. */
        val PARALLEL = NotificationPublishStrategy { notification, listeners ->
            coroutineScope {
                listeners.map { listener -> launch { listener(notification) } }.joinAll()
            }
        }

        /** Runs every listener even if some fail, then throws an [AggregateException] of all failures. */
        val CONTINUE_ON_EXCEPTION = NotificationPublishStrategy { notification, listeners ->
            val errors = mutableListOf<Throwable>()
            listeners.forEach { listener ->
                try {
                    listener(notification)
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
            if (errors.isNotEmpty()) throw AggregateException(errors)
        }

        val DEFAULT = PARALLEL

        /** Launches listeners on [scope] and returns immediately without awaiting them. */
        fun fireAndForget(scope: CoroutineScope) = NotificationPublishStrategy { notification, listeners ->
            listeners.forEach { listener -> scope.launch { listener(notification) } }
        }
    }
}
