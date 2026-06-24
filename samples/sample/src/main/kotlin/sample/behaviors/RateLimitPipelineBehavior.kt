package sample.behaviors

import com.fajrbahr.mediatork.api.*

/**
 * Rejects requests when more than [maxRequests] have been dispatched within the last [windowMs]
 * milliseconds (sliding window).
 */
class RateLimitPipelineBehavior(
    private val maxRequests: Int,
    private val windowMs: Long,
) : PipelineBehavior {

    private val timestamps = ArrayDeque<Long>()

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val now = System.currentTimeMillis()
        val cutoff = now - windowMs

        synchronized(timestamps) {
            while (timestamps.isNotEmpty() && timestamps.first() < cutoff) timestamps.removeFirst()
            if (timestamps.size >= maxRequests) {
                throw RuntimeException("Rate limit exceeded: max $maxRequests requests per ${windowMs}ms")
            }
            timestamps.addLast(now)
        }

        return next(request)
    }
}
