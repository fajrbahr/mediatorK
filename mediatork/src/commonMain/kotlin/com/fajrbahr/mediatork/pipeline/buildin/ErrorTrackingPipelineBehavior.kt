@file:Suppress("TooGenericExceptionCaught")

package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.behavior

/**
 * A behavior provider that intercepts every unhandled exception and forwards it to a
 * callback before rethrowing.
 *
 * Use this to wire crash-reporting services (Firebase Crashlytics, Sentry, Bugsnag) into
 * the pipeline without touching handler code. The callback receives the original request
 * and the throwable — the exception is always rethrown after the callback returns.
 *
 * @param onError callback invoked on every unhandled exception with `(request, throwable)`.
 * @param order position in the behavior chain. Defaults to `Int.MAX_VALUE` (innermost) so it
 *   fires closest to the handler, after retry/timeout behaviors have already given up.
 */
fun errorTrackingPipelineBehavior(
    order: Int = Int.MAX_VALUE,
    onError: (request: Request<*>, error: Throwable) -> Unit,
): PipelineBehavior = behavior(order = order) { _, next, request ->
    try {
        next(request)
    } catch (e: Throwable) {
        onError(request, e)
        throw e
    }
}
