package sample.behaviors

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import sample.bookings.queries.fetchbookings.FetchBookingsQuery


class MeasurePipelineBehaviour : PipelineBehavior {

    override val order: Int = 0

    override fun appliesTo(request: Request<*>): Boolean {
        return request is FetchBookingsQuery
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

