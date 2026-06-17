package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationError

class ValidationBehavior(
    private val validators: List<RequestValidator<*>>
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest
    ): TResult {
        val validator: RequestValidator<TRequest>? =
            validators.find { it.requestClass == request::class } as? RequestValidator<TRequest>

        validator?.validate(request)?.let { result ->
            if (!result.isValid) {
                throw ValidationException(result.errors)
            }
        }

        return next(request)
    }
}

class ValidationException(
    val errors: List<ValidationError>
) : IllegalArgumentException(errors.joinToString("; ") { it.message })