package com.fajrbahr.mediatork.validator

import kotlin.reflect.KClass

/**
 * Validates a request object. Return [ValidationResult.Invalid] to signal failure,
 * or [ValidationResult.Valid] to pass. Use [rules] or [rulesFailFast] for a declarative DSL,
 * or construct [ValidationResult] directly for simple checks.
 *
 * @param TRequest the request type this validator handles.
 */
interface RequestValidator<TRequest : Any> {
    val requestClass: KClass<TRequest>
    fun validate(request: TRequest): ValidationResult
}
