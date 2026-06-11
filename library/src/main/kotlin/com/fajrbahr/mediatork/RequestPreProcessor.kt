package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler

/**
 * Hook that runs before the [RequestHandler] is invoked, for every request that
 * passes through the mediator.
 *
 * Common uses include logging, validation, authentication checks, and populating
 * the [RequestContext] with values that handlers will consume (e.g. a resolved
 * locale or a trace ID).
 *
 * Multiple pre-processors are sorted by [order] (ascending) before execution.
 * Unlike [com.fajrbahr.mediatork.pipeline.PipelineBehavior], a pre-processor cannot short-circuit the pipeline —
 * throw an exception to abort.
 *
 * @see RequestPostProcessor
 * @see com.fajrbahr.mediatork.pipeline.PipelineBehavior
 */
interface RequestPreProcessor {
    /**
     * Relative position among pre-processors. Lower values run first.
     * Defaults to `0`; processors with the same order run in an unspecified sequence.
     */
    val order: Int get() = 0

    /**
     * Executes pre-processing logic before the handler is called.
     *
     * @param requestContext mutable bag scoped to this pipeline execution; values
     *   stored here are visible to the handler and post-processors.
     * @param request the incoming request (read-only from the processor's perspective).
     */
    suspend fun process(requestContext: RequestContext, request: Request<*>)
}
