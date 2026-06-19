package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.ValidationResult

class ValidationBehavior(
    private val validators: List<RequestValidator<*>>
) : PipelineBehavior {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest
    ): TResult {
        validators
            .find { it.requestClass.isInstance(request) }
            ?.let { validator ->
                val result = (validator as RequestValidator<TRequest>).validate(request)
                if (result is ValidationResult.Invalid) throw ValidationException(result.errors)
            }

        return next(request)
    }
}
