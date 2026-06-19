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
 * Behaviors are grouped into three phases by [tag], then sorted by [order] **within** each
 * phase. **Phase always wins over order**: every [Tag.Pre] behavior executes before every
 * [Tag.Default] behavior, and every [Tag.Default] before every [Tag.Post] — no matter what
 * [order] values are assigned. [order] only controls sequencing inside a phase.
 *
 * Typical uses:
 * - [Tag.Pre]: auth token injection, locale setup, tracing context
 * - [Tag.Default]: logging, retry, caching, timing, circuit-breaking
 * - [Tag.Post]: metrics emission, audit logging, response observation
 *
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
     * **Phase takes absolute priority over [order].** Every [Tag.Pre] behavior runs
     * before every [Tag.Default] behavior, and every [Tag.Default] behavior runs before
     * every [Tag.Post] behavior — regardless of what [order] values are set. [order] only
     * controls the sequence *within* a phase.
     *
     * | Phase        | Position     | Typical use                                      |
     * |--------------|--------------|--------------------------------------------------|
     * | [Tag.Pre]    | outermost    | auth injection, locale, trace-id setup           |
     * | [Tag.Default]| middle       | logging, retry, caching, circuit-breaking        |
     * | [Tag.Post]   | innermost    | metrics, audit logging, response observation     |
     *
     * Example: a [Tag.Pre] behavior with `order = 999` still runs **before** a
     * [Tag.Default] behavior with `order = -999`.
     *
     * Defaults to [Tag.Default].
     */
    sealed class Tag {
        data object Pre : Tag()
        data object Default : Tag()
        data object Post : Tag()
    }

    val tag: Tag get() = Tag.Default

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
