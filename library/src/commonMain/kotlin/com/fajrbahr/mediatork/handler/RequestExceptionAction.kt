package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext

/**
 * Executes a side effect when a specific exception is thrown during request handling,
 * without recovering from or swallowing the exception.
 *
 * This is the counterpart to [RequestExceptionHandler]: where [RequestExceptionHandler]
 * converts an exception into a fallback response (recovery), [RequestExceptionAction]
 * runs a side effect — logging, telemetry, alerting — and then lets the exception
 * continue propagating unchanged.
 *
 * Multiple actions can be registered for the same `(request type, exception type)` pair;
 * all matching actions are executed in registration order before the exception is rethrown
 * (or handed to a [RequestExceptionHandler] if one is registered).
 *
 * Typical uses: crash reporting (Firebase Crashlytics, Sentry), structured error logging,
 * metrics emission, and audit-trail recording on failure.
 *
 * ```kotlin
 * class LogNetworkErrorAction : RequestExceptionAction<FetchDataQuery, NetworkException> {
 *     override suspend fun execute(
 *         requestContext: RequestContext,
 *         request: FetchDataQuery,
 *         exception: NetworkException,
 *     ) {
 *         logger.error("Network failure on FetchDataQuery(id=${request.id})", exception)
 *     }
 * }
 *
 * // Registration
 * registry.registerExceptionAction(
 *     FetchDataQuery::class,
 *     NetworkException::class,
 *     LogNetworkErrorAction(),
 * )
 * ```
 *
 * @param TRequest the request type whose pipeline this action monitors.
 * @param TException the exception type this action reacts to.
 * @see RequestExceptionHandler
 * @see com.fajrbahr.mediatork.HandlerRegistry.registerExceptionAction
 */
interface RequestExceptionAction<in TRequest : Request<*>, in TException : Throwable> {
    /**
     * Performs a side effect in response to [exception] thrown while handling [request].
     *
     * Must **not** throw — any exception thrown here is suppressed so that the
     * original [exception] (or subsequent actions) are not affected.
     *
     * @param requestContext the context for the current pipeline execution.
     * @param request the request that was being handled when the exception occurred.
     * @param exception the exception that was thrown.
     */
    suspend fun execute(requestContext: RequestContext, request: TRequest, exception: TException)
}
