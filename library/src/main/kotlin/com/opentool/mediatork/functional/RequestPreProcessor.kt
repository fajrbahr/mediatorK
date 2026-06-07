package com.opentool.mediatork.com.opentool.mediatork.functional

data class RequestPreProcessor(
    val order: Int = 0,
    val process: suspend (RequestContext, Request<*>) -> Unit,
)
