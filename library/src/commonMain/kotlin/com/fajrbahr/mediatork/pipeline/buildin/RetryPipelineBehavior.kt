package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import kotlinx.coroutines.delay

/**
 * A [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that retries the downstream pipeline on failure.
 *
 * On each attempt, if [next] throws an exception that satisfies [retryOn], the behavior
 * waits [delayMillis] milliseconds and tries again. Once [maxRetries] attempts are
 * exhausted the last exception is rethrown.
 *
 * ```kotlin
 * // Retry up to 3 times on any IOException, waiting 200 ms between attempts.
 * val retry = RetryPipelineBehavior(
 *     maxRetries = 3,
 *     delayMillis = 200,
 *     retryOn = { it is IOException },
 * )
 * ```
 *
 * @param maxRetries number of retry attempts after the first failure (total attempts = maxRetries + 1).
 * @param delayMillis milliseconds to wait between attempts. Defaults to `0` (no delay).
 * @param retryOn predicate that decides whether a thrown [Throwable] should trigger a retry.
 *   Defaults to retrying on every [Throwable].
 * @param order position in the behavior chain. Defaults to `0`.
 */
class RetryPipelineBehavior(
    val maxRetries: Int = 3,
    val delayMillis: Long = 0L,
    val retryOn: (Throwable) -> Boolean = { true },
    override val order: Int = 0,
) : PipelineBehavior {

    init {
        require(maxRetries >= 0) { "maxRetries must be >= 0, was $maxRetries" }
        require(delayMillis >= 0) { "delayMillis must be >= 0, was $delayMillis" }
    }

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        var attempt = 0
        while (true) {
            try {
                return next(request)
            } catch (e: Throwable) {
                if (attempt >= maxRetries || !retryOn(e)) throw e
                attempt++
                if (delayMillis > 0) delay(delayMillis)
            }
        }
    }
}
