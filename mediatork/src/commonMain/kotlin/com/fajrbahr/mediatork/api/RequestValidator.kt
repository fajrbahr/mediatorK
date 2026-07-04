package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.validator.ValidationResult

/**
 * Handles validation for a specific request type.
 */
fun interface RequestValidator<in TRequest> {
    fun validate(request: TRequest): ValidationResult
}
