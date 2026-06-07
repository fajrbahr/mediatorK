package sample.behaviors

import com.fajrbahr.mediatork.PipelineBehavior
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestHandlerDelegate
import sample.context.traceNetworkMetrics

class Mertix(traceName: String, metricName: String, value: Long) {
    fun start() {
    }
}

class Trace(traceName: String) {
    fun start() {
    }

    fun stop() {}
    fun putMetric(metricName: String, value: Long) {

    }
}

class FirebasePerformance {
    fun newTrace(requestName: String) = Trace(requestName)

}

class TracingPipelineBehavior(val tracker: FirebasePerformance = FirebasePerformance()) : PipelineBehavior {

    override val order: Int = 3

    override suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext, next: RequestHandlerDelegate<TReq, TRes>, request: TReq
    ): TRes {

        val requestName = request::class.simpleName ?: "UnknownRequest"

        val trace = tracker.newTrace(requestName)
        trace.start()

        val result = next(request)

        requestContext.traceNetworkMetrics.map { trace.putMetric(it.name, it.value) }

        trace.stop()
        return result
    }
}
