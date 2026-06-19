package com.fajrbahr.mediatork.validator

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestValidator
import kotlin.reflect.KClass

class BoundValidator<TRequest : Any> internal constructor(
    internal val requestClass: KClass<TRequest>,
    internal val validator: RequestValidator<TRequest>,
)

inline fun <reified TRequest : Request<*>> RequestValidator<TRequest>.bind(): BoundValidator<TRequest> =
    BoundValidator(TRequest::class, this)
