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
 * Execution stage that determines where a [PipelineBehavior] sits in the pipeline chain.
 *
 * **Stage takes absolute priority over order.** Every [Stage.Pre] behavior runs before every
 * [Stage.Default] behavior, and every [Stage.Default] before every [Stage.Post] — regardless
 * of [PipelineBehavior.order]. Order only controls sequencing *within* a stage.
 *
 * | Stage           | Position  | Typical use                                   |
 * |-----------------|-----------|-----------------------------------------------|
 * | [Stage.Pre]     | outermost | auth injection, locale, trace-id setup        |
 * | [Stage.Default] | middle    | logging, retry, caching, circuit-breaking     |
 * | [Stage.Post]    | innermost | metrics, audit logging, response observation  |
 */
sealed class Stage {
    data object Pre : Stage()
    data object Default : Stage()
    data object Post : Stage()
}

/**
 * Cross-cutting concern that wraps request handling in a decorator-style chain.
 *
 * Behaviors are grouped into three stages by [stage], then sorted by [order] **within** each
 * stage. **Stage always wins over order**: every [Stage.Pre] behavior executes before every
 * [Stage.Default] behavior, and every [Stage.Default] before every [Stage.Post] — no matter what
 * [order] values are assigned. [order] only controls sequencing inside a stage.
 *
 * Typical uses:
 * - [Stage.Pre]: auth token injection, locale setup, tracing context
 * - [Stage.Default]: logging, retry, caching, timing, circuit-breaking
 * - [Stage.Post]: metrics emission, audit logging, response observation
 *
 * @see PipelineBehavior.stage
 * @see PipelineBehavior.order
 * @see PipelineBehavior.isEnabled
 * @see PipelineBehavior.appliesTo
 */
interface PipelineBehavior {

    val stage: Stage get() = Stage.Default

    /**
     * Relative position within the [stage]. Lower values are outermost (run first on entry).
     * Defaults to `0`. Within a stage, behaviors with the same [order] run in registration order.
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
