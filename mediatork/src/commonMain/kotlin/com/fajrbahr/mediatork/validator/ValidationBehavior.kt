package com.fajrbahr.mediatork.validator

import com.fajrbahr.mediatork.api.*
import kotlin.reflect.KClass

/**
 * Thrown when validation fails. [errors] contains one or more messages describing what failed.
 * Catch this in a pipeline behavior, exception handler, or ViewModel.
 */
class ValidationException(val errors: List<*>, cause: Throwable? = null) :
    Exception(errors.joinToString("; ") { it.toString() }, cause) {
    constructor(message: String, cause: Throwable? = null) : this(listOf(message), cause)
}

/**
 * Pre-built [PipelineBehavior] that runs registered [RequestValidator]s before the handler.
 * Throws [ValidationException] if any validator returns [ValidationResult.Invalid].
 *
 * @param validators validators keyed by request [KClass]; only the entry matching the incoming request type runs.
 * @param order position in the behavior chain; defaults to `-50` (runs before most behaviors).
 */
class ValidationBehavior(
    private val validators: Map<KClass<*>, List<RequestValidator<*>>>,
    override val order: Int = -50,
) : PipelineBehavior {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        validators[request::class]?.forEach { validator ->
            val result = (validator as RequestValidator<TRequest>).validate(request)
            if (result is ValidationResult.Invalid) throw ValidationException(result.errors)
        }
        return next(request)
    }
}
