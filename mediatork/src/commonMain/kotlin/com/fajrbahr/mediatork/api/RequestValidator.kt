package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.validator.ValidationResult

/**
 * Validates a request object. Return [com.fajrbahr.mediatork.validator.ValidationResult.Invalid] to signal failure,
 * or [com.fajrbahr.mediatork.validator.ValidationResult.Valid] to pass. Use [com.fajrbahr.mediatork.validator.rules] or [com.fajrbahr.mediatork.validator.rulesFailFast] for a declarative DSL,
 * or construct [com.fajrbahr.mediatork.validator.ValidationResult] directly for simple checks.
 *
 * @param TRequest the request type this validator handles.
 */
interface RequestValidator<TRequest : Any> {
    fun validate(request: TRequest): ValidationResult
}