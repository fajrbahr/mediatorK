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
 * instance.
 *
 * Thread-safe: uses a [Mutex] so concurrent `send` calls never race on the counter map.
 */
class RequestCounter : PipelineBehavior {

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

    suspend fun countFor(requestClass: KClass<*>): Long {
        val key = requestClass.simpleName ?: requestClass.toString()
        return mutex.withLock { counts[key] ?: 0L }
    }

    suspend fun snapshot(): Map<String, Long> = mutex.withLock { counts.toMap() }

    suspend fun reset() = mutex.withLock { counts.clear() }
}
