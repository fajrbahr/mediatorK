package sample.behaviors

import com.fajrbahr.mediatork.PipelineBehavior
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestHandlerDelegate

class LoggingBehavior : PipelineBehavior {
    override val order: Int = 1

    override suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {
        val result = next(request)
        return result
    }
}
