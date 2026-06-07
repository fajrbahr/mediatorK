package com.opentool.mediatork.com.opentool.mediatork

interface RequestPostProcessor {
    val order: Int get() = 0
    suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?)
}
