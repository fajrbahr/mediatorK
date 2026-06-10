package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.pipeline.*

/**
 * Hook that runs after the [RequestHandler] has produced a result, before that
 * result is returned to the caller.
 *
 * Common uses include response logging, cache population, metric emission, and
 * audit trails. The [response] is `Any?` to remain generic; cast it to the
 * expected type when the specific response type is known.
 *
 * Multiple post-processors are sorted by [order] (ascending) before execution.
 * Unlike [PipelineBehavior], a post-processor cannot mutate the response — it is
 * for observation only. Throw an exception to signal a post-processing failure.
 *
 * @see RequestPreProcessor
 * @see PipelineBehavior
 */
interface RequestPostProcessor {
    /**
     * Relative position among post-processors. Lower values run first.
     * Defaults to `0`; processors with the same order run in an unspecified sequence.
     */
    val order: Int get() = 0

    /**
     * Executes post-processing logic after the handler has returned.
     *
     * @param requestContext mutable bag scoped to this pipeline execution; contains
     *   any values written by pre-processors, behaviors, or the handler.
     * @param request the original request that was handled.
     * @param response the value returned by the handler, or `null` for `Unit` responses.
     */
    suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?)
}
