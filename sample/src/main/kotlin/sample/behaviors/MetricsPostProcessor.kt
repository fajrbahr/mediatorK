package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestPostProcessor


class MetricsPostProcessor : RequestPostProcessor {
    override suspend fun process(
        requestContext: RequestContext,
        request: Request<*>,
        response: Any?
    ) {
        // println("📊 Metrics for ${request::class.simpleName}")
    }

}
