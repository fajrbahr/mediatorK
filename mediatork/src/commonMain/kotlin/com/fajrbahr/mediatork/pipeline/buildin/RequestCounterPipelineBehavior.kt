package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * A [PipelineBehavior] that counts how many times each request type has passed through
 * the pipeline. Counts survive across multiple `send` calls for the lifetime of this
 * behavior instance.
 *
 *
 * Thread-safe: uses a [Mutex] so concurrent `send` calls never race on the counter map.
 *
 * @param order position in the behavior chain. Defaults to `0`.
 */
class RequestCounterPipelineBehavior(
    override val order: Int = 0,
) : PipelineBehavior {

    private val mutex = Mutex()
    private val counts = mutableMapOf<String, Long>()

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val key = request::class.simpleName ?: request::class.toString()
        mutex.withLock { counts[key] = (counts[key] ?: 0L) + 1L }
        return next(request)
    }

    /** Returns the number of times [requestClass] has been dispatched. */
    suspend fun countFor(requestClass: KClass<*>): Long {
        val key = requestClass.simpleName ?: requestClass.toString()
        return mutex.withLock { counts[key] ?: 0L }
    }

    /** Returns a snapshot of all counts keyed by request class simple name. */
    suspend fun snapshot(): Map<String, Long> = mutex.withLock { counts.toMap() }

    /** Resets all counters to zero. */
    suspend fun reset() = mutex.withLock { counts.clear() }
}
