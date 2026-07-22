package sample.meditor.behaviors

import com.fajrbahr.mediatork.Behavior
import com.fajrbahr.mediatork.behavior
import kotlin.time.TimeSource

/** Post-handler behavior: times how long each request takes to flow through the pipeline. */
fun measureBehavior(): Behavior = behavior(order = 10) { request, _, next ->
    val start = TimeSource.Monotonic.markNow()
    try {
        next()
    } finally {
        val ms = start.elapsedNow().inWholeMilliseconds
        println("[MEASURE] ${request::class.simpleName} took ${ms}ms")
    }
}
