package com.fajrbahr.mediatork.sample.android.aftersuper.domain

import android.util.Log
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestPostProcessor
import com.fajrbahr.mediatork.RequestPreProcessor

class TraceIdPreProcessor : RequestPreProcessor {
    override val order = -1000
    override suspend fun process(requestContext: RequestContext, request: Request<*>) {
        requestContext.put("traceId", "trace-${request::class.simpleName}-${System.currentTimeMillis()}")
    }
}

class RequestAuditPostProcessor : RequestPostProcessor {
    override val order = 0
    override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
        val traceId = requestContext.getMetaDate<String>("traceId") ?: "no-trace"
        Log.d("MediatorK", "[Audit] $traceId | ${request::class.simpleName} completed")
    }
}
