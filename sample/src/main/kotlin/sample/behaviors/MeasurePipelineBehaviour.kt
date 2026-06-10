package sample.behaviors
import com.fajrbahr.mediatork.handler.*

import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestHandlerDelegate
import sample.query.FetchUserQueryId


class MeasurePipelineBehaviour : PipelineBehavior {

    override val order: Int = 0

    override fun appliesTo(request: Request<*>): Boolean {
        return request is FetchUserQueryId
    }

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest
    ): TResult {
        val start = System.currentTimeMillis()
        val response = next(request)
        val end = System.currentTimeMillis()
        println("Request ${request::class.simpleName} took ${end - start} ms")
        return response
    }
}

