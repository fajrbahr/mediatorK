package com.fajrbahr.mediatork.validator

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate

/**
 * Thrown by [ValidationBehavior] when a [RequestValidator] returns an invalid [ValidationResult].
 *
 * Catch this in a pipeline behavior, exception handler, or ViewModel to inspect
 * [errors] and map each [ValidationError.field] to the appropriate UI state.
 *
 * @param errors the non-empty list of errors produced by the validator.
 */
class ValidationException(val errors: List<ValidationError>) :
    Exception("Validation failed: ${errors.joinToString { "[${it.field}] ${it.message}" }}")

/**
 * Pre-built [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that runs registered [RequestValidator]s before the handler.
 *
 * Runs early in the pipeline (order = -50) so validation failures short-circuit
 * before any business logic executes. Throws [ValidationException] when the result
 * is invalid — catch it in your ViewModel or an exception handler to map errors to UI.
 *
 * You can use this behavior as-is or replace it with your own implementation if you
 * need different ordering, error formatting, or conditional validation logic.
 *
 * ```kotlin
 * val mediator = MediatorFactory.create(
 *     registrars = listOf(AppRegistrar()),
 *     pipelineBehaviors = listOf(
 *         ValidationBehavior(listOf(CreateTodoValidator())),
 *     ),
 * )
 * ```
 *
 * @param validators the validators to run; each is matched to a request by [RequestValidator.requestClass].
 * @param order position in the behavior chain; defaults to `-50` (runs before most behaviors).
 */
class ValidationBehavior(
    private val validators: List<RequestValidator<*>>,
    override val order: Int = -50,
) : PipelineBehavior {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val validator = validators
            .firstOrNull { it.requestClass.isInstance(request) }
                as? RequestValidator<TRequest>

        val result = validator?.validate(request)
        if (result != null && !result.isValid) throw ValidationException(result.errors)

        return next(request)
    }
}
