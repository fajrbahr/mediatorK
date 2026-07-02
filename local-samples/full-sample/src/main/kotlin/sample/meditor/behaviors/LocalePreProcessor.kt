package sample.meditor.behaviors

import com.fajrbahr.mediatork.api.*
import sample.meditor.context.locale
import java.util.*

class LocaleBehavior : PipelineBehavior {
    override val order = -10

    override val stage: Stage = Stage.Pre

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
