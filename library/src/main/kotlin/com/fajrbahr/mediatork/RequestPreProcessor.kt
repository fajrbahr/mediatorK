package com.fajrbahr.mediatork

interface RequestPreProcessor {
    val order: Int get() = 0
    suspend fun process(requestContext: RequestContext, request: Request<*>)
}
