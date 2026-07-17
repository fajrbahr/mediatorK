package local.meditor.behaviors

import com.fajrbahr.mediatork.Behavior
import com.fajrbahr.mediatork.behavior
import kotlin.time.TimeSource

fun measureBehavior(): Behavior = behavior(order = 10) { request, _, next ->
    val start = TimeSource.Monotonic.markNow()
        next()
        val ms = start.elapsedNow().inWholeMilliseconds
        println("[MEASURE] ${request::class.simpleName} took ${ms}ms")
}
