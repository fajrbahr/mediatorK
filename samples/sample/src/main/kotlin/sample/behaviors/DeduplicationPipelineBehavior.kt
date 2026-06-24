package sample.behaviors

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Deduplicates concurrent requests with the same value.
 *
 * When multiple coroutines dispatch identical requests simultaneously, only the first one
 * reaches the handler — all others wait and share its result. Requests are keyed by
 * structural equality so the request type must implement [equals]/[hashCode] correctly
 * (data classes do this automatically).
 */
class DeduplicationPipelineBehavior : PipelineBehavior {

    private val inflight = ConcurrentHashMap<Any, CompletableDeferred<Any?>>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val newDeferred = CompletableDeferred<Any?>()
        val existing = inflight.putIfAbsent(request, newDeferred)

        return if (existing == null) {
            try {
                val result = next(request)
                newDeferred.complete(result)
                result
            } catch (e: Throwable) {
                newDeferred.completeExceptionally(e)
                throw e
            } finally {
                inflight.remove(request, newDeferred)
            }
        } else {
            existing.await() as TResult
        }
    }
}
