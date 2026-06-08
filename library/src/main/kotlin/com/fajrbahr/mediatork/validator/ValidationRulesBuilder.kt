package com.fajrbahr.mediatork.validator

fun rules(block: ValidationRulesBuilder.() -> Unit): ValidationResult {
    val builder = ValidationRulesBuilder()
    builder.block()
    return builder.build()
}

fun rulesFailFast(block: FailFastValidationBuilder.() -> Unit): ValidationResult {
    val builder = FailFastValidationBuilder()
    builder.block()
    return builder.build()
}

class FailFastValidationBuilder {
    private val errors = mutableListOf<ValidationError>()

    fun check(condition: Boolean, message: () -> String) {
        if (errors.isNotEmpty()) return
        if (!condition) errors += ValidationError(message = message())
    }

    fun <V> ruleFor(fieldName: FieldV, value: V, block: FieldValidator<V>.(V) -> Unit) {
        if (errors.isNotEmpty()) return

        val fieldValidator = FieldValidator<V>(fieldName, failFast = true)
        fieldValidator.block(value)
        errors += fieldValidator.collectErrors()
    }

    internal fun build(): ValidationResult = ValidationResult(errors.toList())
}

class ValidationRulesBuilder {
    private val errors = mutableListOf<ValidationError>()

    fun check(condition: Boolean, message: () -> String) {
        if (!condition) errors += ValidationError(message = message())
    }

    fun <V> ruleFor(fieldName: FieldV, value: V, block: FieldValidator<V>.(V) -> Unit) {
        val fieldValidator = FieldValidator<V>(fieldName)
        fieldValidator.block(value)
        errors += fieldValidator.collectErrors()
    }

    internal fun build(): ValidationResult = ValidationResult(errors.toList())
}

class FieldValidator<V>(private val fieldName: FieldV, private val failFast: Boolean = false) {
    private val errors = mutableListOf<ValidationError>()

    fun check(condition: Boolean, message: () -> String) {
        if (failFast && errors.isNotEmpty()) return
        if (!condition) errors += ValidationError(field = fieldName, message = message())
    }

    internal fun collectErrors(): List<ValidationError> = errors.toList()
}
