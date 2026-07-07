package local.meditor.behaviors

import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import local.meditor.context.locale
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
