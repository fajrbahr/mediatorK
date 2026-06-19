package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that deduplicates concurrent in-flight requests with the same key.
 *
 * When two `send` calls arrive with the same request key while the first is still executing,
 * the second call suspends and awaits the first call's result instead of running a second
 * pipeline. This prevents duplicate network calls, DB queries, or expensive computations
 * caused by concurrent ViewModels or rapid user interactions.
 *
 * ```kotlin
 * DeduplicationPipelineBehavior(
 *     keyFor = { req -> "${req::class.simpleName}:${req}" }
 * )
 * ```
 *
 * The default key is the request's class name — which deduplicates all concurrent calls
 * of the same type regardless of field values. Override [keyFor] to deduplicate by both
 * type and identity (e.g. include the request's ID).
 *
 * @param keyFor function that produces a deduplication key. Defaults to simple class name.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class DeduplicationPipelineBehavior(
    val keyFor: (Request<*>) -> String = { it::class.simpleName ?: it.toString() },
    override val order: Int = 0,
) : PipelineBehavior {

    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Any?>>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val key = keyFor(request)
        val myDeferred = CompletableDeferred<Any?>()

        val existingDeferred = mutex.withLock {
            inFlight[key].also { if (it == null) inFlight[key] = myDeferred }
        }

        // Another call is already in flight — await its result
        if (existingDeferred != null) {
            return existingDeferred.await() as TResult
        }

        // We are the first caller — execute the pipeline and broadcast the result
        return try {
            val result = next(request)
            myDeferred.complete(result)
            result
        } catch (e: Throwable) {
            myDeferred.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }

    /** Returns the number of requests currently in flight. */
    suspend fun inFlightCount(): Int = mutex.withLock { inFlight.size }
}
