package com.fajrbahr.mediatork.pipeline
import com.fajrbahr.mediatork.handler.*

/**
 * A [PipelineBehavior] that logs each request as it enters and exits the pipeline.
 *
 * Accepts any `(String) -> Unit` logger so the same behavior works on every platform:
 * pass `::println` for KMP/Native, `Log.d` on Android, an SLF4J logger on JVM,
 * or `console::log` on JS/browser.
 *
 * ```kotlin
 * // KMP / multiplatform
 * LoggingPipelineBehavior(logger = ::println)
 *
 * // JVM — SLF4J
 * val log = LoggerFactory.getLogger("Mediator")
 * LoggingPipelineBehavior(logger = log::info)
 *
 * // Android — Logcat
 * LoggingPipelineBehavior(logger = { msg -> Log.d("Mediator", msg) })
 *
 * // JS / browser
 * LoggingPipelineBehavior(logger = { msg -> console.log(msg) })
 * ```
 *
 * @param logger function that receives each log line.
 * @param logResult when `true`, the string representation of the result is appended to the exit log line.
 * @param order position in the behavior chain. Defaults to `-100` so logging is outermost by default.
 */
class LoggingPipelineBehavior(
    val logger: (String) -> Unit = ::println,
    val logResult: Boolean = false,
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
        if (logResult) {
            logger("← $name result=$result")
        } else {
            logger("← $name")
        }
        return result
    }
}
