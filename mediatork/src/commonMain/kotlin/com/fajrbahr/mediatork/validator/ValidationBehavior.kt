package com.fajrbahr.mediatork.validator

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.behavior
import kotlin.reflect.KClass

/**
 * Thrown when validation fails. [errors] contains one or more messages describing what failed.
 * [warnings] contains informational messages that were collected alongside the errors.
 * Catch this in a pipeline behavior, exception handler, or ViewModel.
 */
class ValidationException(
    val errors: List<*>,
    val warnings: List<*> = emptyList<Any>(),
    cause: Throwable? = null,
) : Exception(formatMessage(errors, warnings), cause) {

    constructor(message: String, cause: Throwable? = null) : this(listOf(message), emptyList<Any>(), cause)

    private companion object {
        fun formatMessage(errors: List<*>, warnings: List<*>): String = buildString {
            append(errors.joinToString("; ") { it.toString() })
            if (warnings.isNotEmpty()) {
                append(" | warnings: ")
                append(warnings.joinToString("; ") { it.toString() })
            }
        }
    }
}

/**
 * Creates a [PipelineBehavior] that runs registered [RequestValidator]s before the handler.
 * Throws [ValidationException] if any validator returns [ValidationResult.Invalid].
 * Warnings from [ValidationResult.ValidWithWarnings] are stored in the [RequestContext]
 * under the key `"validation_warnings"` and do not block the request.
 *
 * @param validators validators keyed by request [KClass]; only the entry matching the incoming request type runs.
 * @param order position in the behavior chain; defaults to `-50` (runs before most behaviors).
 */
fun validationBehavior(
    validators: Map<KClass<*>, List<RequestValidator<*>>>,
    order: Int = -50,
): PipelineBehavior = behavior(order = order) { requestContext, next, request ->
    @Suppress("UNCHECKED_CAST")
    val allWarnings = mutableListOf<Any?>()

    val selfResult = request.validate()
    collectOrThrow(selfResult, allWarnings)

    validators[request::class]?.forEach { validator ->
        val result = (validator as RequestValidator<Any>).validate(request)
        collectOrThrow(result, allWarnings)
    }

    if (allWarnings.isNotEmpty()) {
        requestContext.put("validation_warnings", allWarnings.toList())
    }

    next(request)
}

private fun collectOrThrow(result: ValidationResult, allWarnings: MutableList<Any?>) {
    when (result) {
        is ValidationResult.Invalid -> {
            allWarnings.addAll(result.warnings)
            throw ValidationException(result.errors, allWarnings.toList())
        }

        is ValidationResult.ValidWithWarnings -> allWarnings.addAll(result.warnings)
        is ValidationResult.Valid -> {}
    }
}
