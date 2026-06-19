package com.fajrbahr.mediatork.sample.android.aftersuper.domain

import android.util.Log
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate

class TraceIdBehavior : PipelineBehavior {
    override val tag = PipelineBehavior.Tag.PRE
    override val order = -1000

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        requestContext.put("traceId", "trace-${request::class.simpleName}-${System.currentTimeMillis()}")
        return next(request)
    }
}

class RequestAuditBehavior : PipelineBehavior {
    override val tag = PipelineBehavior.Tag.POST
    override val order = 0

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val result = next(request)
        val traceId = requestContext.getMetaDate<String>("traceId") ?: "no-trace"
        Log.d("MediatorK", "[Audit] $traceId | ${request::class.simpleName} completed")
        return result
    }
}
