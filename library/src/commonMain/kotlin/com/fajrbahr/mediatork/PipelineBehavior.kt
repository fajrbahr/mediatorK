package com.fajrbahr.mediatork

/**
 * Suspend function alias for the next step in the request-handling pipeline.
 *
 * A [PipelineBehavior] receives one of these as the `next` parameter; calling it
 * advances execution to the next behavior, or ultimately to the handler if there
 * are no more behaviors in the chain.
 *
 * @param TRequest the request type flowing through the pipeline.
 * @param TResult the response type produced at the end of the pipeline.
 */
typealias RequestHandlerDelegate<TRequest, TResult> = suspend (TRequest) -> TResult

/**
 * Cross-cutting concern that wraps request handling in a decorator-style chain.
 *
 * Pipeline behaviors are composed using `foldRight`, so behaviors with a **lower**
 * [order] value are the **outermost** decorators and run first on the way in and
 * last on the way out. This mirrors the middleware ordering familiar from frameworks
 * such as ASP.NET Core or Ktor.
 *
 * Typical uses: logging, tracing, retry, caching, timing, and authorization.
 *
 * ```kotlin
 * class LoggingBehavior : PipelineBehavior {
 *     override val order = -100 // runs before other behaviors
 *     override suspend fun <TRequest : Request<TResult>, TResult> process(
 *         requestContext: RequestContext,
 *         next: RequestHandlerDelegate<TRequest, TResult>,
 *         request: TRequest,
 *     ): TResult {
 *         println("Handling ${request::class.simpleName}")
 *         val result = next(request)
 *         println("Handled — result: $result")
 *         return result
 *     }
 * }
 * ```
 *
 * @see PipelineBehavior.order
 * @see PipelineBehavior.isEnabled
 * @see PipelineBehavior.appliesTo
 */
interface PipelineBehavior {

    /**
     * Relative position in the behavior chain. Lower values are outermost (run first).
     * Defaults to `0`; behaviors with equal order run in an unspecified sequence.
     */
    val order: Int get() = 0

    /**
     * Whether this behavior participates in the pipeline at all.
     * When `false`, the behavior is skipped entirely, equivalent to not having
     * registered it. Defaults to `true`.
     */
    val isEnabled: Boolean get() = true

    /**
     * Determines whether this behavior should wrap the given [request].
     *
     * Override to restrict a behavior to a specific request type or subset of
     * requests (e.g. only requests that implement a particular interface).
     * Defaults to `true` (applies to every request).
     *
     * @param request the request being dispatched.
     * @return `true` if this behavior should participate in handling [request].
     */
    fun appliesTo(request: Request<*>): Boolean = true

    /**
     * Wraps the downstream pipeline step represented by [next].
     *
     * Implementations must call `next(request)` (or a modified copy of the request)
     * to continue the chain. Returning without calling [next] short-circuits the
     * remaining behaviors and the handler.
     *
     * @param TRequest the concrete request type.
     * @param TResult the response type.
     * @param requestContext mutable bag scoped to this pipeline execution.
     * @param next the next step in the pipeline; invoke it to continue processing.
     * @param request the request to process.
     * @return the result produced by [next] (possibly transformed).
     */
    suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult
}
