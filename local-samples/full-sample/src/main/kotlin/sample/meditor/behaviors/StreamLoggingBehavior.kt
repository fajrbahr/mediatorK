package sample.meditor.behaviors

import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.StreamHandlerDelegate
import com.fajrbahr.mediatork.api.StreamPipelineBehavior
import com.fajrbahr.mediatork.api.StreamRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class StreamLoggingBehavior : StreamPipelineBehavior {
    override fun <TRequest : StreamRequest<T>, T> process(
        requestContext: RequestContext,
        next: StreamHandlerDelegate<TRequest, T>,
        request: TRequest,
    ): Flow<T> = next(request)
        .onStart { println("[STREAM-START] ${request::class.simpleName}") }
        .onCompletion { println("[STREAM-END] ${request::class.simpleName}") }
}
