package dsl.meditor.behaviors

import com.fajrbahr.mediatork.feature.behavior
import com.fajrbahr.mediatork.pipeline.buildin.errorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.timingPipelineBehavior
import kotlin.time.TimeSource

val measurePipelineBehavior = timingPipelineBehavior { requestName, durationMs ->
    println("[$requestName] took ${durationMs}ms")
}

val allRequestsErrorTracking = errorTrackingPipelineBehavior { request, error ->
    println("[ERROR] ${request::class.simpleName}: ${error?.message}")
}