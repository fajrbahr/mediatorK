package com.fajrbahr.mediatork.feature

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.flow.Flow

fun interface FeatureMapper<TRaw, TResult> {
    fun map(raw: TRaw): TResult
}

inline fun <reified TRaw, reified TResult> mapper(noinline block: (TRaw) -> TResult): FeatureMapper<TRaw, TResult> =
    FeatureMapper(block)

class ValidateBuilder<TRequest>(val request: TRequest) {
    private val errors = mutableListOf<String>()
    private val warnings = mutableListOf<String>()

    fun check(condition: Boolean, message: String) {
        if (!condition) errors.add(message)
    }

    fun check(condition: (TRequest) -> Boolean, message: (TRequest) -> String) {
        if (!condition(request)) errors.add(message(request))
    }

    fun warn(message: String) {
        warnings.add(message)
    }

    fun warn(condition: Boolean, message: String) {
        if (condition) warnings.add(message)
    }

    fun build(): ValidationResult = when {
        errors.isNotEmpty() -> ValidationResult.Invalid(errors, warnings)
        warnings.isNotEmpty() -> ValidationResult.ValidWithWarnings(warnings)
        else -> ValidationResult.Valid
    }
}

fun <TRequest : Any> validate(
    block: ValidateBuilder<TRequest>.() -> Unit,
): RequestValidator<TRequest> = RequestValidator { request ->
    ValidateBuilder(request).apply(block).build()
}

fun <TNotification : Notification> notificationHandler(
    block: suspend (TNotification) -> Unit
): NotificationHandler<TNotification> =
    LambdaNotificationHandler(0, block)

class FeatureHandler<TRequest, TRaw>(
    @PublishedApi internal val block: suspend HandlerScope.(TRequest) -> TRaw,
)

inline fun <reified TRequest : Request<*>, TRaw> handler(
    noinline block: suspend HandlerScope.(TRequest) -> TRaw,
): FeatureHandler<TRequest, TRaw> = FeatureHandler(block)

// ── Feature Builder ──────────────────────────────────────────────────────────

@MediatorKDsl
class FeatureBuilder<TRequest : Any, TRaw, TResult>
@PublishedApi internal constructor() {

    @PublishedApi
    internal var handler: Any? = null

    @PublishedApi
    internal val validators: MutableList<RequestValidator<TRequest>> = mutableListOf()

    @PublishedApi
    internal var mapperBlock: ((TRaw) -> TResult)? = null

    @PublishedApi
    internal var beforeBlock: (suspend (RequestContext, Request<*>) -> Unit)? = null

    @PublishedApi
    internal var afterBlock: (suspend (RequestContext, Any?, Request<*>) -> Unit)? = null

    fun validate(validator: RequestValidator<TRequest>) {
        validators.add(validator)
    }

    fun validate(block: ValidateBuilder<TRequest>.() -> Unit) {
        validators.add(RequestValidator { request ->
            ValidateBuilder(request).apply(block).build()
        })
    }

    fun <TRequestExt : TRequest> handler(featureHandler: FeatureHandler<TRequestExt, TRaw>) {
        @Suppress("UNCHECKED_CAST")
        this.handler = LambdaRequestHandler(featureHandler.block)
    }

    fun handle(block: suspend HandlerScope.(TRequest) -> TRaw) = apply {
        this.handler = LambdaRequestHandler(block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun wrapHandler(wrapper: (RequestHandler<Request<Any?>, Any?>) -> RequestHandler<Request<Any?>, Any?>) {
        handler = wrapper(handler as RequestHandler<Request<Any?>, Any?>)
    }

    fun retry(maxAttempts: Int = 3) = apply { wrapHandler { RetryHandler(it, maxAttempts) } }

    fun timeout(duration: kotlin.time.Duration) = apply { wrapHandler { TimeoutHandler(it, duration) } }

    fun measure(
        onMeasured: suspend (TRequest, Long) -> Unit = { _, ms -> println("[MEASURE] completed in ${ms}ms") },
    ) = apply {
        @Suppress("UNCHECKED_CAST")
        val typed = handler as RequestHandler<Request<Any?>, Any?>
        handler = MeasureHandler(typed, onMeasured as suspend (Request<Any?>, Long) -> Unit)
    }

    fun log(
        logger: HandlerLogger<TRequest, TRaw> = PrintHandlerLogger(),
    ) = apply {
        @Suppress("UNCHECKED_CAST")
        val typed = handler as RequestHandler<Request<Any?>, Any?>
        handler = LogHandler(typed, logger as HandlerLogger<Request<Any?>, Any?>)
    }

    fun cache(
        keyFrom: (TRequest) -> Any = { it },
        store: MutableMap<Any, Any?> = mutableMapOf(),
    ) = apply {
        @Suppress("UNCHECKED_CAST")
        val typed = handler as RequestHandler<Request<Any?>, Any?>
        handler = CacheHandler(typed, keyFrom as (Request<Any?>) -> Any, store)
    }

    fun fallback(block: suspend HandlerScope.(TRequest) -> TRaw) = apply {
        @Suppress("UNCHECKED_CAST")
        val typed = handler as RequestHandler<Request<Any?>, Any?>
        val fb = LambdaRequestHandler(block) as RequestHandler<Request<Any?>, Any?>
        handler = FallbackHandler(typed, fb)
    }

    fun mapper(block: (TRaw) -> TResult) {
        mapperBlock = block
    }

    fun <TMapperRaw> mapper(featureMapper: FeatureMapper<TMapperRaw, TResult>) {
        @Suppress("UNCHECKED_CAST")
        mapperBlock = featureMapper::map as (TRaw) -> TResult
    }

    fun before(block: suspend (RequestContext, Request<*>) -> Unit) {
        beforeBlock = block
    }

    fun after(block: suspend (RequestContext, Any?, Request<*>) -> Unit) {
        afterBlock = block
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
    @PublishedApi internal val validators: List<RequestValidator<TRequest>> = emptyList(),
    @PublishedApi internal val beforeBlock: (suspend (RequestContext, Request<*>) -> Unit)? = null,
    @PublishedApi internal val afterBlock: (suspend (RequestContext, Any?, Request<*>) -> Unit)? = null,
)

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <TRequest : Request<TResult>, TResult> buildFeature(
    builder: FeatureBuilder<TRequest, *, TResult>,
): Feature<TRequest, TResult> {
    val baseHandler = builder.handler as? RequestHandler<TRequest, Any?>
        ?: error("feature {} requires a handle {} block")

    val finalHandler = if (builder.mapperBlock != null) {
        val mapper = builder.mapperBlock!! as (Any?) -> TResult
        RequestHandler { mediator, requestContext, request ->
            mapper(
                baseHandler.handle(
                    mediator,
                    requestContext,
                    request
                )
            )
        }
    } else {
        baseHandler as RequestHandler<TRequest, TResult>
    }

    return Feature(
        handler = finalHandler,
        validators = builder.validators.toList(),
        beforeBlock = builder.beforeBlock,
        afterBlock = builder.afterBlock,
    )
}

inline fun <reified TRequest : Request<TResult>, TResult> feature(
    block: FeatureBuilder<TRequest, TResult, TResult>.() -> Unit,
): Feature<TRequest, TResult> {
    val builder = FeatureBuilder<TRequest, TResult, TResult>().apply(block)
    return buildFeature(builder)
}

@Suppress("UNCHECKED_CAST", "PLATFORM_DECLARATION_CLASH")
inline fun <reified TRequest : Request<TRaw>, TRaw : Any, TResult : Any> feature(
    block: FeatureBuilder<TRequest, TRaw, TResult>.() -> Unit,
): Feature<TRequest, TRaw> {
    val builder = FeatureBuilder<TRequest, TRaw, TResult>().apply(block)
    val baseHandler = builder.handler as? RequestHandler<TRequest, TRaw>
        ?: error("feature {} requires a handle {} block")

    val finalHandler = if (builder.mapperBlock != null) {
        val mapper = builder.mapperBlock!!
        RequestHandler { mediator, requestContext, request ->
            val rawResult = baseHandler.handle(mediator, requestContext, request)
            mapper(rawResult) as TRaw
        }
    } else {
        baseHandler
    }

    return Feature(
        handler = finalHandler,
        validators = builder.validators.toList(),
        beforeBlock = builder.beforeBlock,
        afterBlock = builder.afterBlock,
    )
}

@Suppress("UNCHECKED_CAST", "PLATFORM_DECLARATION_CLASH")
inline fun <reified TRequest : Request<TResult>, TRaw : Any, TResult : Any> feature(
    block: FeatureBuilder<TRequest, TRaw, TResult>.() -> Unit,
): Feature<TRequest, TResult> {
    val builder = FeatureBuilder<TRequest, TRaw, TResult>().apply(block)
    return buildFeature(builder)
}

inline fun <reified TRequest : StreamRequest<T>, T> feature(
    block: StreamFeatureBuilder<TRequest, T>.() -> Unit,
): StreamFeature<TRequest, T> {
    val builder = StreamFeatureBuilder<TRequest, T>().apply(block)
    return StreamFeature(
        handler = builder.handler ?: error("feature {} requires a handle {} block"),
    )
}

// ── Stream Feature Builder ───────────────────────────────────────────────────

@MediatorKDsl
class StreamFeatureBuilder<TRequest : StreamRequest<T>, T>
@PublishedApi internal constructor() {

    @PublishedApi
    internal var handler: StreamRequestHandler<TRequest, T>? = null

    fun handler(streamHandler: StreamRequestHandler<TRequest, T>) {
        this.handler = streamHandler
    }

    fun handle(block: HandlerScope.(TRequest) -> Flow<T>) {
        this.handler = LambdaStreamRequestHandler(block)
    }
}

class StreamFeature<TRequest : StreamRequest<T>, T> @PublishedApi internal constructor(
    @PublishedApi internal val handler: StreamRequestHandler<TRequest, T>,
)

// ── Pipeline behavior DSL ────────────────────────────────────────────────────

@PublishedApi
internal class LambdaPipelineBehavior(
    val order: Int,
    val isEnabled: Boolean,
    private val filter: (Request<*>) -> Boolean,
    private val block: suspend (RequestContext, suspend (Request<*>) -> Any?, Request<*>) -> Any?,
) : PipelineBehavior {
    fun appliesTo(request: Request<*>) = filter(request)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val untypedNext: suspend (Request<*>) -> Any? = { r -> next(r as TRequest) }
        return block(requestContext, untypedNext, request) as TResult
    }
}

fun behavior(
    order: Int = 0,
    isEnabled: Boolean = true,
    appliesTo: (Request<*>) -> Boolean = { true },
    block: suspend (requestContext: RequestContext, next: suspend (Request<*>) -> Any?, request: Request<*>) -> Any?,
): PipelineBehavior = LambdaPipelineBehavior(order, isEnabled, appliesTo, block)

@PublishedApi
internal class LambdaStreamPipelineBehavior(
    val order: Int,
    val isEnabled: Boolean,
    private val filter: (StreamRequest<*>) -> Boolean,
    private val block: (RequestContext, (StreamRequest<*>) -> Flow<*>, StreamRequest<*>) -> Flow<*>,
) : StreamPipelineBehavior {
    fun appliesTo(request: StreamRequest<*>) = filter(request)

    @Suppress("UNCHECKED_CAST")
    override fun <TRequest : StreamRequest<T>, T> process(
        requestContext: RequestContext,
        next: StreamHandlerDelegate<TRequest, T>,
        request: TRequest,
    ): Flow<T> {
        val untypedNext: (StreamRequest<*>) -> Flow<*> = { r -> next(r as TRequest) }
        return block(requestContext, untypedNext, request) as Flow<T>
    }
}

fun streamBehavior(
    order: Int = 0,
    isEnabled: Boolean = true,
    appliesTo: (StreamRequest<*>) -> Boolean = { true },
    block: (requestContext: RequestContext, next: (StreamRequest<*>) -> Flow<*>, request: StreamRequest<*>) -> Flow<*>,
): StreamPipelineBehavior = LambdaStreamPipelineBehavior(order, isEnabled, appliesTo, block)
