package com.fajrbahr.mediatork.validator

import kotlin.reflect.KClass


interface RequestValidator<TRequest : Any> {
    val requestClass: KClass<TRequest>
    fun validate(request: TRequest): ValidationResult
}
