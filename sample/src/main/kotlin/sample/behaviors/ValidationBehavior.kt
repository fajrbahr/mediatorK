package sample.behaviors

import com.fajrbahr.mediatork.PipelineBehavior
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestHandlerDelegate
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationError

class ValidationBehavior(
    private val validators: List<RequestValidator<*>>
) : PipelineBehavior {

    override suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {
        val validator: RequestValidator<TReq>? =
            validators.find { it.requestClass == request::class } as? RequestValidator<TReq>

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