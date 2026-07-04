package dsl.meditor.behaviors

import com.fajrbahr.mediatork.api.StreamPipelineBehavior
import com.fajrbahr.mediatork.feature.streamBehavior
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

val streamLoggingBehavior: StreamPipelineBehavior = streamBehavior { _, next, request ->
    next(request)
        .onStart { println("[STREAM-START] ${request::class.simpleName}") }
        .onCompletion { println("[STREAM-END] ${request::class.simpleName}") }
}
