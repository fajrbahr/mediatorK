package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate

/**
 * A [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that intercepts every unhandled exception and forwards it to a
 * callback before rethrowing.
 *
 * Use this to wire crash-reporting services (Firebase Crashlytics, Sentry, Bugsnag) into
 * the pipeline without touching handler code. The callback receives the original request
 * and the throwable — the exception is always rethrown after the callback returns.
 *
 *
 * @param onError callback invoked on every unhandled exception with `(request, throwable)`.
 * @param order position in the behavior chain. Defaults to `Int.MAX_VALUE` (innermost) so it
 *   fires closest to the handler, after retry/timeout behaviors have already given up.
 */
class ErrorTrackingPipelineBehavior(
    override val order: Int = Int.MAX_VALUE,
    val onError: (request: Request<*>, error: Throwable) -> Unit,
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        return try {
            next(request)
        } catch (e: Throwable) {
            onError(request, e)
            throw e
        }
    }
}
