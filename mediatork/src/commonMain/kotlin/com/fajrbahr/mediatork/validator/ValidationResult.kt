package com.fajrbahr.mediatork.validator

/**
 * The result of running a [com.fajrbahr.mediatork.api.RequestValidator].
 *
 * Produce a result with the [collectingValidator] or [shortCircuitValidator] DSL builders, or construct directly
 * for simple single-check validators.
 */
sealed class ValidationResult {

    /** Warnings collected during validation. Always safe to read — empty when absent. */
    open val warnings: List<*> get() = emptyList<Any>()

    /** All checks passed; the request is valid. */
    data object Valid : ValidationResult()

    /**
     * All error checks passed but one or more [warnings] were collected.
     * Warnings are informational — they do not block the request.
     *
     * @param warnings non-empty list of warning values.
     */
    data class ValidWithWarnings(override val warnings: List<*>) : ValidationResult()

    /**
     * One or more checks failed. [errors] contains the full list of error values.
     * The element type is intentionally untyped — use `String`, a sealed error class, or
     * any other type that suits your domain.
     *
     * @param errors non-empty list of validation errors.
     * @param warnings informational warnings collected alongside errors.
     */
    data class Invalid(
        val errors: List<*>,
        override val warnings: List<*> = emptyList<Any>(),
    ) : ValidationResult() {
        /** Convenience constructor for a single error value. */
        constructor(error: Any) : this(listOf(error))
    }
}

/**
 * Throws [ValidationException] if this result is [ValidationResult.Invalid]; no-op otherwise.
 * Any collected [ValidationResult.warnings] are included in the exception.
 */
fun ValidationResult.throwIfInvalid() {
    if (this is ValidationResult.Invalid) throw ValidationException(errors, warnings)
}
