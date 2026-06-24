package sample.behaviors

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.delay

/**
 * Retries the handler up to [maxRetries] times on any exception, with [delayMs] between attempts.
 */
class RetryPipelineBehavior(
    private val maxRetries: Int = 3,
    private val delayMs: Long = 100,
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        var lastException: Throwable? = null
        for (attempt in 0..maxRetries) {
            try {
                return next(request)
            } catch (e: Throwable) {
                lastException = e
                if (attempt < maxRetries && delayMs > 0) delay(delayMs)
            }
        }
        throw lastException!!
    }
}
