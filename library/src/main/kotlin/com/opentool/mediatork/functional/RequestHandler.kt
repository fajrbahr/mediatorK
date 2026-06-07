package com.opentool.mediatork.com.opentool.mediatork.functional

typealias RequestHandler<TRequest, TResult> = suspend (RequestContext, TRequest) -> TResult
