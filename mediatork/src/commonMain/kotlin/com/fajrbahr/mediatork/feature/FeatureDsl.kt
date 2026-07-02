package com.fajrbahr.mediatork.feature

import com.fajrbahr.mediatork.HandlerScope
import com.fajrbahr.mediatork.LambdaRequestHandler
import com.fajrbahr.mediatork.LambdaRequestValidator
import com.fajrbahr.mediatork.MediatorKDsl
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult

fun interface FeatureMapper<TRaw, TResult> {
    fun map(raw: TRaw): TResult
}

fun <TRaw, TResult> mapper(block: (TRaw) -> TResult): FeatureMapper<TRaw, TResult> =
    FeatureMapper(block)

fun <TRequest : Any> validator(block: (TRequest) -> ValidationResult): RequestValidator<TRequest> =
    LambdaRequestValidator(block)

@MediatorKDsl
class FeatureBuilder<TRequest : Request<TResult>, TRaw, TResult>
@PublishedApi internal constructor() {

    @PublishedApi internal var handler: RequestHandler<TRequest, TResult>? = null
    @PublishedApi internal var validatorInstance: RequestValidator<TRequest>? = null

    fun validate(validator: RequestValidator<TRequest>) {
        this.validatorInstance = validator
    }

    fun validate(block: (TRequest) -> ValidationResult) {
        this.validatorInstance = LambdaRequestValidator(block)
    }

    fun handle(block: suspend HandlerScope.(TRequest) -> TRaw) {
        @Suppress("UNCHECKED_CAST")
        this.handler = LambdaRequestHandler(block) as RequestHandler<TRequest, TResult>
    }

    fun map(mapper: FeatureMapper<TRaw, TResult>) {
        map(mapper::map)
    }

    fun map(block: (TRaw) -> TResult) {
        val existing = this.handler
            ?: error("handle {} must be declared before map {}")
        @Suppress("UNCHECKED_CAST")
        val innerHandle: suspend (Mediator, RequestContext, TRequest) -> TRaw =
            { mediator, requestContext, request ->
                existing.handle(mediator, requestContext, request) as TRaw
            }
        this.handler = MappingRequestHandler(innerHandle, block)
    }
}

@PublishedApi
internal class MappingRequestHandler<TRequest : Request<TResult>, TRaw, TResult>(
    private val innerHandle: suspend (Mediator, RequestContext, TRequest) -> TRaw,
    private val mapper: (TRaw) -> TResult,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        return mapper(innerHandle(mediator, requestContext, request))
    }
}

class Feature<TRequest : Request<TResult>, TResult> @PublishedApi internal constructor(
    @PublishedApi internal val handler: RequestHandler<TRequest, TResult>,
    @PublishedApi internal val validator: RequestValidator<TRequest>?,
)

inline fun <reified TRequest : Request<TResult>, TResult> feature(
    block: FeatureBuilder<TRequest, TResult, TResult>.() -> Unit,
): Feature<TRequest, TResult> {
    val builder = FeatureBuilder<TRequest, TResult, TResult>().apply(block)
    return Feature(
        handler = builder.handler ?: error("feature {} requires a handle {} block"),
        validator = builder.validatorInstance,
    )
}

inline fun <reified TRequest : Request<TResult>, TRaw, TResult> mappedFeature(
    block: FeatureBuilder<TRequest, TRaw, TResult>.() -> Unit,
): Feature<TRequest, TResult> {
    val builder = FeatureBuilder<TRequest, TRaw, TResult>().apply(block)
    return Feature(
        handler = builder.handler ?: error("feature {} requires a handle {} block"),
        validator = builder.validatorInstance,
    )
}
