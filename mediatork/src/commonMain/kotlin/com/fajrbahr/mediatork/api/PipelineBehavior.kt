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
 * Create instances via the [behavior] DSL function. Behaviors are grouped into three stages,
 * then sorted by order **within** each stage. **Stage always wins over order**: every
 * [Stage.Pre] behavior executes before every [Stage.Default] behavior, and every [Stage.Default]
 * before every [Stage.Post] — no matter what order values are assigned. Order only controls
 * sequencing inside a stage.
 *
 * Typical uses:
 * - [Stage.Pre]: auth token injection, locale setup, tracing context
 * - [Stage.Default]: logging, retry, caching, timing, circuit-breaking
 * - [Stage.Post]: metrics emission, audit logging, response observation
 *
 * @see com.fajrbahr.mediatork.feature.behavior
 */
interface PipelineBehavior : Behavior {
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

val PipelineBehavior.stage: Stage
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.stage ?: Stage.Default

val PipelineBehavior.order: Int
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.order ?: 0

val PipelineBehavior.isEnabled: Boolean
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.isEnabled ?: true

fun PipelineBehavior.appliesTo(request: Request<*>): Boolean =
    (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.appliesTo(request) ?: true
