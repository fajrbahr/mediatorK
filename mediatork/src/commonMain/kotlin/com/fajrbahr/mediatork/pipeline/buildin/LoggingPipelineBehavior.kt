package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

/**
 * A [PipelineBehavior] that logs each request as it enters and exits the pipeline,
 * including the result on exit.
 *
 * Accepts any `(String) -> Unit` logger so the same behavior works on every platform:
 * pass `::println` for KMP/Native, `Log.d` on Android, an SLF4J logger on JVM,
 * or `console::log` on JS/browser.
 *
 * @param logger function that receives each log line.
 * @param order position in the behavior chain. Defaults to `-100` so logging is outermost by default.
 */
class LoggingPipelineBehavior(
    val logger: (String) -> Unit = ::println,
    override val order: Int = -100,
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val name = request::class.simpleName ?: "UnknownRequest"
        logger("→ $name")
        val result = next(request)
        logger("← $name result=$result")
        return result
    }
}
