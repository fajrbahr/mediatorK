package com.opentool.mediatork.com.opentool.mediatork.functional

data class RequestPostProcessor(
    val order: Int = 0,
    val process: suspend (RequestContext, Request<*>, Any?) -> Unit,
)
