package sample.behaviors

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlin.reflect.KClass

class ValidationBehavior(
    private val validators: Map<KClass<*>, List<RequestValidator<*>>>
) : PipelineBehavior {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest
    ): TResult {
        validators[request::class]?.forEach { validator ->
            val result = (validator as RequestValidator<TRequest>).validate(request)
            if (result is ValidationResult.Invalid) throw ValidationException(result.errors)
        }
        return next(request)
    }
}
