package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request

suspend fun <TRequest : Request<TResult>, TResult> Mediator.query(request: TRequest): TResult = send(request)

suspend fun <T> Mediator.trySend(request: Request<T>): Result<T> =
    try {
        Result.success(send(request))
    } catch (e: Exception) {
        Result.failure(e)
    }
