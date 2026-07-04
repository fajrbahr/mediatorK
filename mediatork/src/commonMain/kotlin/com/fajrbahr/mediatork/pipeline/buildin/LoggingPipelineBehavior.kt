package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.feature.behavior

/**
 * A behavior provider that logs each request as it enters and exits the pipeline,
 * including the result on exit.
 *
 * Accepts any `(String) -> Unit` logger so the same behavior works on every platform:
 * pass `::println` for KMP/Native, `Log.d` on Android, an SLF4J logger on JVM,
 * or `console::log` on JS/browser.
 *
 * @param logger function that receives each log line.
 * @param order position in the behavior chain. Defaults to `-100` so logging is outermost by default.
 */
fun loggingPipelineBehavior(
    logger: (String) -> Unit = ::println,
    order: Int = -100,
): PipelineBehavior = behavior(order = order) { _, next, request ->
    val name = request::class.simpleName ?: "UnknownRequest"
    logger("→ $name")
    val result = next(request)
    logger("← $name result=$result")
    result
}
