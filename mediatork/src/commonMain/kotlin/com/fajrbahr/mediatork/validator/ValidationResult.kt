package com.fajrbahr.mediatork.validator

/**
 * The result of running a [com.fajrbahr.mediatork.api.RequestValidator].
 *
 * Produce a result with the [rules] or [rulesFailFast] DSL builders, or construct directly
 * for simple single-check validators.
 */
sealed class ValidationResult {
    /** All checks passed; the request is valid. */
    data object Valid : ValidationResult()

    /**
     * One or more checks failed. [errors] contains the full list of error values.
     * The element type is intentionally untyped — use `String`, a sealed error class, or
     * any other type that suits your domain.
     *
     * @param errors non-empty list of validation errors.
     */
    data class Invalid(val errors: List<*>) : ValidationResult() {
        /** Convenience constructor for a single error value. */
        constructor(error: Any) : this(listOf(error))
    }
}

/**
 * Throws [ValidationException] if this result is [ValidationResult.Invalid]; no-op otherwise.
 */
fun ValidationResult.throwIfInvalid() {
    if (this is ValidationResult.Invalid) throw ValidationException(errors)
}
