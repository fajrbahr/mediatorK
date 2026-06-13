package com.fajrbahr.mediatork.validator

/**
 * Marker interface for a field identifier used in a [ValidationError].
 *
 * Implement this with an enum, sealed class, or data object to represent specific
 * fields of a request. Using a typed identifier rather than a plain string prevents
 * typos and enables exhaustive `when` expressions over known fields.
 *
 * @see DefaultField
 * @see ValidationError
 */
interface FieldValidator

/**
 * Sentinel field identifier used when a [ValidationError] is not associated with
 * any specific field — for example, a cross-field constraint violation.
 */
object DefaultField : FieldValidator

/**
 * Describes a single validation failure, optionally tied to a specific [field].
 *
 * @property field the field that failed validation; defaults to [DefaultField] for
 *   errors that are not field-specific.
 * @property message a human-readable description of the validation failure.
 */
data class ValidationError(
    val field: FieldValidator = DefaultField,
    val message: String
)

/**
 * Outcome of a validation pass, carrying zero or more [ValidationError]s.
 *
 * Use [isValid] to check success at a glance, and inspect [errors] for details.
 * Prefer the factory functions on the companion object over constructing directly.
 *
 * @property errors the list of errors found during validation; empty means valid.
 */
data class ValidationResult(
    val errors: List<ValidationError> = emptyList()
) {
    /** `true` when no errors were found. */
    val isValid: Boolean get() = errors.isEmpty()

    companion object {
        /** A pre-built successful result with an empty error list. */
        val Success = ValidationResult()

        /**
         * Creates a result with a single field-agnostic error.
         *
         * @param message the error description.
         */
        fun error(message: String) = ValidationResult(listOf(ValidationError(message = message)))

        /**
         * Creates a result with a single error tied to a specific [field].
         *
         * @param field the field that failed.
         * @param message the error description.
         */
        fun error(field: FieldValidator, message: String) = ValidationResult(listOf(ValidationError(field, message)))

        /**
         * Creates a result from an arbitrary set of [ValidationError]s.
         *
         * @param errors one or more errors describing the failures.
         */
        fun failure(vararg errors: ValidationError) = ValidationResult(errors.toList())
    }
}
