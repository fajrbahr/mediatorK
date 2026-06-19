package com.fajrbahr.mediatork.validator

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errors: List<*>) : ValidationResult() {
        constructor(error: Any) : this(listOf(error))
    }
}

fun ValidationResult.throwIfInvalid() {
    if (this is ValidationResult.Invalid) throw ValidationException(errors)
}
