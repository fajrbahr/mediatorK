package com.fajrbahr.mediatork.api

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
 * Behaviors are sorted by [order]. Lower values run first on entry (outermost); higher values
 * run closer to the handler (innermost). Behaviors with the same [order] run in registration order.
 *
 * Typical order ranges:
 * - Negative (e.g., -100): outermost — auth, context setup, tracing
 * - Zero to positive (e.g., 0): middle — logging, retry, caching, circuit-breaking
 * - Positive (e.g., 100+): innermost — metrics, audit logging, observation
 *
 * @see PipelineBehavior.order
 * @see PipelineBehavior.isEnabled
 * @see PipelineBehavior.appliesTo
 */
interface PipelineBehavior {

    /**
     * Relative position in the behavior chain. Lower values execute first (outermost);
     * higher values execute later (closer to handler). Defaults to `0`.
     * Behaviors with the same [order] run in registration order.
     */
    val order: Int get() = 0

    /**
     * Whether this behavior participates in the pipeline at all.
     * When `false`, the behavior is skipped entirely. Defaults to `true`.
     */
    val isEnabled: Boolean get() = true

    /**
     * Determines whether this behavior should wrap the given [request].
     * Defaults to `true` (applies to every request).
     */
    fun appliesTo(request: Request<*>): Boolean = true

    /**
     * Wraps the downstream pipeline step represented by [next].
     *
     * Implementations must call `next(request)` to continue the chain.
     * Returning without calling [next] short-circuits the remaining behaviors and handler.
     */
    suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult
}
