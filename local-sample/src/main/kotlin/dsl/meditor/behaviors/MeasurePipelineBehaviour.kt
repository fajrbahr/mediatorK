package dsl.meditor.behaviors

import com.fajrbahr.mediatork.feature.behavior
import kotlin.time.TimeSource

val measurePipelineBehavior = behavior(order = 10) { _, next, request ->
    val start = TimeSource.Monotonic.markNow()
    try {
        next(request)
    } finally {
        val ms = start.elapsedNow().inWholeMilliseconds
        println("[MEASURE] ${request::class.simpleName} took ${ms}ms")
    }
}
