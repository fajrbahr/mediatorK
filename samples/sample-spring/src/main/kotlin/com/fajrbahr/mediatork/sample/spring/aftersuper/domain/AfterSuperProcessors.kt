package com.fajrbahr.mediatork.sample.spring.aftersuper.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.Stage
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

class TraceIdBehavior : PipelineBehavior {
    override val stage = Stage.Pre
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
    override val stage = Stage.Post
    override val order = 0

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val result = next(request)
        val traceId = requestContext.getMetaDate<String>("traceId") ?: "no-trace"
        println("[Audit] $traceId | ${request::class.simpleName} completed (response=${result != null})")
        return result
    }
}
