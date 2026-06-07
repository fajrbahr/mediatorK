package com.opentool.mediatork.com.opentool.mediatork.functional

typealias RequestExceptionHandler<TRequest, TResponse, TException> = suspend (RequestContext, TRequest, TException) -> TResponse
