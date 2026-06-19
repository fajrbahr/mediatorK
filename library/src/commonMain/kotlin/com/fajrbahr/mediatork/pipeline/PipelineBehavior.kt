package com.fajrbahr.mediatork.pipeline

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext

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
 * Behaviors are grouped into three phases by [tag], then sorted by [order] **within** each
 * phase. **Phase always wins over order**: every [Tag.PRE] behavior executes before every
 * [Tag.DEFAULT] behavior, and every [Tag.DEFAULT] before every [Tag.POST] — no matter what
 * [order] values are assigned. [order] only controls sequencing inside a phase.
 *
 * Typical uses:
 * - [Tag.PRE]: auth token injection, locale setup, tracing context
 * - [Tag.DEFAULT]: logging, retry, caching, timing, circuit-breaking
 * - [Tag.POST]: metrics emission, audit logging, response observation
 *
 * ```kotlin
 * class LoggingBehavior : PipelineBehavior {
 *     override val order = -100 // outermost among DEFAULT behaviors
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
 *
 * class MetricsBehavior : PipelineBehavior {
 *     override val tag = Tag.POST
 *     override suspend fun <TRequest : Request<TResult>, TResult> process(
 *         requestContext: RequestContext,
 *         next: RequestHandlerDelegate<TRequest, TResult>,
 *         request: TRequest,
 *     ): TResult {
 *         val result = next(request)
 *         println("[METRICS] ${request::class.simpleName} completed")
 *         return result
 *     }
 * }
 * ```
 *
 * @see PipelineBehavior.tag
 * @see PipelineBehavior.order
 * @see PipelineBehavior.isEnabled
 * @see PipelineBehavior.appliesTo
 */
interface PipelineBehavior {

    /**
     * Phase that determines where this behavior sits in the execution chain.
     *
     * **Phase takes absolute priority over [order].** Every [Tag.PRE] behavior runs
     * before every [Tag.DEFAULT] behavior, and every [Tag.DEFAULT] behavior runs before
     * every [Tag.POST] behavior — regardless of what [order] values are set. [order] only
     * controls the sequence *within* a phase.
     *
     * | Phase        | Position     | Typical use                                      |
     * |--------------|--------------|--------------------------------------------------|
     * | [Tag.PRE]    | outermost    | auth injection, locale, trace-id setup           |
     * | [Tag.DEFAULT]| middle       | logging, retry, caching, circuit-breaking        |
     * | [Tag.POST]   | innermost    | metrics, audit logging, response observation     |
     *
     * Example: a [Tag.PRE] behavior with `order = 999` still runs **before** a
     * [Tag.DEFAULT] behavior with `order = -999`.
     *
     * Defaults to [Tag.DEFAULT].
     */
    enum class Tag { PRE, DEFAULT, POST }

    val tag: Tag get() = Tag.DEFAULT

    /**
     * Relative position within the [tag] phase. Lower values are outermost (run first on entry).
     * Defaults to `0`. Within a phase, behaviors with the same [order] run in registration order.
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
