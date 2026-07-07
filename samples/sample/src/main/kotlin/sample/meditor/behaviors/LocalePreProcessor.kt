package sample.meditor.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import sample.meditor.context.locale
import java.util.*

class LocaleBehavior : PipelineBehavior {
    override val order = -10

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val systemLocale = Locale.getDefault().language
        requestContext.locale = systemLocale
        return next(request)
    }
}
