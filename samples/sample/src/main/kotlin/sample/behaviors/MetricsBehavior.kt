package sample.behaviors

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MetricsBehavior : PipelineBehavior {
    override val order = 100

    private val counts = ConcurrentHashMap<String, AtomicLong>()

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val key = request::class.simpleName ?: "Unknown"
        counts.getOrPut(key) { AtomicLong(0) }.incrementAndGet()
        return next(request)
    }

    fun snapshot(): Map<String, Long> = counts.mapValues { it.value.get() }
}
