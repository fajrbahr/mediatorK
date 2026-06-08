package sample.behaviors

import com.fajrbahr.mediatork.PipelineBehavior
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestHandlerDelegate

/**
 * In real systems, you usually retry only transient faults (network timeouts, SQLException, etc.) – not business exceptions or validation errors. C
 */
class RetryPipelineBehavior(
    private val maxRetries: Int = 2
) : PipelineBehavior {

    override val order: Int = 1

    override suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {

        var lastError: Throwable? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return next(request)
            } catch (t: Throwable) {
                if (t is IllegalArgumentException) throw t
                lastError = t
                println("[RETRY] attempt ${attempt + 1}/${maxRetries + 1} failed for ${request::class.simpleName}: ${t.message}")
            }
        }

        throw lastError ?: RuntimeException("Unknown error")
    }
}


