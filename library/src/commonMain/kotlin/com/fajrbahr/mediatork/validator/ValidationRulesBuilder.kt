package com.fajrbahr.mediatork.validator

/**
 * Builds a [ValidationResult] by running all rules in [block] and collecting every error.
 *
 * Unlike [rulesFailFast], this function always evaluates every rule, so all failures
 * are reported in a single pass. Use it when callers benefit from seeing every error
 * at once (e.g. form validation).
 *
 * ```kotlin
 * val result = rules {
 *     check(name.isNotBlank()) { "Name must not be blank" }
 *     ruleFor(Fields.Email, email) { value ->
 *         check(value.contains('@')) { "Must be a valid email address" }
 *     }
 * }
 * ```
 *
 * @param block the validation logic expressed via [ValidationRulesBuilder].
 * @return a [ValidationResult] containing all errors found, or [ValidationResult.Success].
 */
fun rules(block: ValidationRulesBuilder.() -> Unit): ValidationResult {
    val builder = ValidationRulesBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builds a [ValidationResult] that stops at the first error found.
 *
 * Once a [FailFastValidationBuilder.check] or [FailFastValidationBuilder.ruleFor]
 * produces an error, all subsequent rules are skipped. Use this when later rules
 * depend on earlier ones being valid (e.g. parse before validate).
 *
 * @param block the validation logic expressed via [FailFastValidationBuilder].
 * @return a [ValidationResult] with at most one error, or [ValidationResult.Success].
 */
fun rulesFailFast(block: FailFastValidationBuilder.() -> Unit): ValidationResult {
    val builder = FailFastValidationBuilder()
    builder.block()
    return builder.build()
}

/**
 * DSL builder that evaluates rules in order and stops at the first failure.
 *
 * All methods are no-ops once the first error has been collected.
 *
 * @see rulesFailFast
 */
class FailFastValidationBuilder {
    private val errors = mutableListOf<ValidationError>()

    /**
     * Adds a field-agnostic error if [condition] is `false`.
     * Skipped entirely if an error was already recorded.
     *
     * @param condition `true` means the check passed.
     * @param message lazy producer of the error description; only evaluated on failure.
     */
    fun check(condition: Boolean, message: () -> String) {
        if (errors.isNotEmpty()) return
        if (!condition) errors += ValidationError(message = message())
    }

    /**
     * Applies [block] to [value] using a [FieldValidator] scoped to [fieldName].
     * Skipped entirely if an error was already recorded.
     *
     * @param V the type of the field value.
     * @param fieldName the [FieldV] identifier for the field being validated.
     * @param value the field value to validate.
     * @param block validation rules applied to [value] via [FieldValidator].
     */
    fun <V> ruleFor(fieldName: FieldV, value: V, block: FieldValidator<V>.(V) -> Unit) {
        if (errors.isNotEmpty()) return
        val fieldValidator = FieldValidator<V>(fieldName, failFast = true)
        fieldValidator.block(value)
        errors += fieldValidator.collectErrors()
    }

    /** Finalises and returns the [ValidationResult]. */
    internal fun build(): ValidationResult = ValidationResult(errors.toList())
}

/**
 * DSL builder that evaluates all rules and collects every error.
 *
 * @see rules
 */
class ValidationRulesBuilder {
    private val errors = mutableListOf<ValidationError>()

    /**
     * Adds a field-agnostic error if [condition] is `false`.
     *
     * @param condition `true` means the check passed.
     * @param message lazy producer of the error description; only evaluated on failure.
     */
    fun check(condition: Boolean, message: () -> String) {
        if (!condition) errors += ValidationError(message = message())
    }

    /**
     * Applies [block] to [value] using a [FieldValidator] scoped to [fieldName],
     * collecting all errors the block produces.
     *
     * @param V the type of the field value.
     * @param fieldName the [FieldV] identifier for the field being validated.
     * @param value the field value to validate.
     * @param block validation rules applied to [value] via [FieldValidator].
     */
    fun <V> ruleFor(fieldName: FieldV, value: V, block: FieldValidator<V>.(V) -> Unit) {
        val fieldValidator = FieldValidator<V>(fieldName)
        fieldValidator.block(value)
        errors += fieldValidator.collectErrors()
    }

    /** Finalises and returns the [ValidationResult]. */
    internal fun build(): ValidationResult = ValidationResult(errors.toList())
}

/**
 * Accumulates errors for a single named field within a validation builder.
 *
 * Obtained via [ValidationRulesBuilder.ruleFor] or [FailFastValidationBuilder.ruleFor].
 *
 * @param V the type of the value being validated.
 */
class FieldValidator<V>(private val fieldName: FieldV, private val failFast: Boolean = false) {
    private val errors = mutableListOf<ValidationError>()

    /**
     * Adds an error attributed to [fieldName] if [condition] is `false`.
     * When [failFast] is `true`, skipped if an error has already been recorded
     * for this field.
     *
     * @param condition `true` means the check passed.
     * @param message lazy producer of the error description; only evaluated on failure.
     */
    fun check(condition: Boolean, message: () -> String) {
        if (failFast && errors.isNotEmpty()) return
        if (!condition) errors += ValidationError(field = fieldName, message = message())
    }

    /** Returns a snapshot of all errors collected for this field. */
    internal fun collectErrors(): List<ValidationError> = errors.toList()
}
