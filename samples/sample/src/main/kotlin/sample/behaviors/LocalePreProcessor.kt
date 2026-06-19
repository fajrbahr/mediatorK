package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import sample.context.locale
import java.util.*

class LocaleBehavior : PipelineBehavior {
    override val tag = PipelineBehavior.Tag.PRE

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
