package com.fajrbahr.mediatork.validator

interface FieldV

object DefaultField : FieldV

data class ValidationError(
    val field: FieldV = DefaultField,
    val message: String
)

data class ValidationResult(
    val errors: List<ValidationError> = emptyList()
) {
    val isValid: Boolean get() = errors.isEmpty()

    companion object {
        val Success = ValidationResult()

        fun error(message: String) = ValidationResult(listOf(ValidationError(message = message)))
        fun error(field: FieldV, message: String) = ValidationResult(listOf(ValidationError(field, message)))
        fun failure(vararg errors: ValidationError) = ValidationResult(errors.toList())
    }
}

