package com.fajrbahr.mediatork.feature

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

fun interface FeatureMapper<TRaw, TResult> {
    fun map(raw: TRaw): TResult
}

fun <TRaw, TResult> mapper(block: (TRaw) -> TResult): FeatureMapper<TRaw, TResult> =
    FeatureMapper(block)

fun <TRequest : Any> requestValidator(block: (TRequest) -> ValidationResult): RequestValidator<TRequest> =
    LambdaRequestValidator(block)

fun <TNotification : Notification> notificationHandler(
    order: Int,
    block: (TNotification) -> Unit
): NotificationHandler<TNotification> =
    LambdaNotificationHandler(order, block)

fun <TNotification : Notification> notificationHandler(block: (TNotification) -> Unit): NotificationHandler<TNotification> =
    LambdaNotificationHandler(0, block)

@PublishedApi
internal class NotificationRegistration<T : Notification>(
    val notificationClass: KClass<T>,
    val handler: NotificationHandler<T>,
)

class FeatureHandler<TRequest, TRaw>(
    @PublishedApi internal val block: suspend HandlerScope.(TRequest) -> TRaw,
)

fun <TRequest, TRaw> requestHandler(
    block: suspend HandlerScope.(TRequest) -> TRaw,
): FeatureHandler<TRequest, TRaw> = FeatureHandler(block)

// ── Feature Builder ──────────────────────────────────────────────────────────

@MediatorKDsl
class FeatureBuilder<TRequest : Request<TResult>, TResult>
@PublishedApi internal constructor() {

    @PublishedApi
    internal var handler: RequestHandler<TRequest, TResult>? = null

    @PublishedApi
    internal val validators: MutableList<RequestValidator<TRequest>> = mutableListOf()

    @PublishedApi
    internal val notificationRegistrations: MutableList<NotificationRegistration<*>> = mutableListOf()

    @PublishedApi
    internal val pipelineBehaviors: MutableList<PipelineBehavior> = mutableListOf()

    @PublishedApi
    internal val streamPipelineBehaviors: MutableList<StreamPipelineBehavior> = mutableListOf()

    fun validate(validator: RequestValidator<TRequest>) {
        validators.add(validator)
    }

    fun requestValidator(block: (TRequest) -> ValidationResult) {
        validators.add(LambdaRequestValidator(block))
    }

    inline fun <reified TNotification : Notification> notification(handler: NotificationHandler<TNotification>) {
        notificationRegistrations.add(NotificationRegistration(TNotification::class, handler))
    }

    fun behavior(behavior: PipelineBehavior) {
        pipelineBehaviors.add(behavior)
    }

    fun behavior(
        stage: Stage = Stage.Default,
        order: Int = 0,
        isEnabled: Boolean = true,
        appliesTo: (Request<*>) -> Boolean = { true },
        block: suspend (requestContext: RequestContext, next: suspend (Request<*>) -> Any?, request: Request<*>) -> Any?,
    ) {
        pipelineBehaviors.add(LambdaPipelineBehavior(stage, order, isEnabled, appliesTo, block))
    }

    fun <TRaw> handler(featureHandler: FeatureHandler<TRequest, TRaw>) {
        @Suppress("UNCHECKED_CAST")
        this.handler = LambdaRequestHandler(featureHandler.block) as RequestHandler<TRequest, TResult>
    }

    fun <TRaw> requestHandler(block: suspend HandlerScope.(TRequest) -> TRaw) {
        @Suppress("UNCHECKED_CAST")
        this.handler = LambdaRequestHandler(block) as RequestHandler<TRequest, TResult>
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
    @PublishedApi internal val notifications: List<NotificationRegistration<*>> = emptyList(),
    @PublishedApi internal val behaviors: List<PipelineBehavior> = emptyList(),
    @PublishedApi internal val streamBehaviors: List<StreamPipelineBehavior> = emptyList(),
)


inline fun <reified TRequest : Request<TResult>, TResult> feature(
    block: FeatureBuilder<TRequest, TResult>.() -> Unit,
): Feature<TRequest, TResult> {
    val builder = FeatureBuilder<TRequest, TResult>().apply(block)
    return Feature(
        handler = builder.handler ?: error("feature {} requires a handle {} block"),
        validators = builder.validators.toList(),
        notifications = builder.notificationRegistrations.toList(),
        behaviors = builder.pipelineBehaviors.toList(),
        streamBehaviors = builder.streamPipelineBehaviors.toList(),
    )
}

inline fun <reified TRequest : Request<TResult>, TResult> mappedFeature(
    mapper: FeatureMapper<*, TResult>,
    block: FeatureBuilder<TRequest, TResult>.() -> Unit,
): Feature<TRequest, TResult> {
    val builder = FeatureBuilder<TRequest, TResult>().apply(block)
    val existing = builder.handler ?: error("mappedFeature {} requires a handle {} block")

    @Suppress("UNCHECKED_CAST")
    val typedMapper = mapper as FeatureMapper<Any?, TResult>
    val innerHandle: suspend (Mediator, RequestContext, TRequest) -> Any? =
        { mediator, requestContext, request ->
            existing.handle(mediator, requestContext, request)
        }
    return Feature(
        handler = MappingRequestHandler(innerHandle, typedMapper::map),
        validators = builder.validators.toList(),
        notifications = builder.notificationRegistrations.toList(),
        behaviors = builder.pipelineBehaviors.toList(),
        streamBehaviors = builder.streamPipelineBehaviors.toList(),
    )
}

// ── Stream Feature Builder ───────────────────────────────────────────────────

@MediatorKDsl
class StreamFeatureBuilder<TRequest : StreamRequest<T>, T>
@PublishedApi internal constructor() {

    @PublishedApi
    internal var handler: StreamRequestHandler<TRequest, T>? = null

    @PublishedApi
    internal val notificationRegistrations: MutableList<NotificationRegistration<*>> = mutableListOf()

    inline fun <reified TNotification : Notification> notification(handler: NotificationHandler<TNotification>) {
        notificationRegistrations.add(NotificationRegistration(TNotification::class, handler))
    }

    fun handler(streamHandler: StreamRequestHandler<TRequest, T>) {
        this.handler = streamHandler
    }

    fun handle(block: HandlerScope.(TRequest) -> Flow<T>) {
        this.handler = LambdaStreamRequestHandler(block)
    }
}

class StreamFeature<TRequest : StreamRequest<T>, T> @PublishedApi internal constructor(
    @PublishedApi internal val handler: StreamRequestHandler<TRequest, T>,
    @PublishedApi internal val notifications: List<NotificationRegistration<*>> = emptyList(),
)

inline fun <reified TRequest : StreamRequest<T>, T> streamFeature(
    block: StreamFeatureBuilder<TRequest, T>.() -> Unit,
): StreamFeature<TRequest, T> {
    val builder = StreamFeatureBuilder<TRequest, T>().apply(block)
    return StreamFeature(
        handler = builder.handler ?: error("streamFeature {} requires a handle {} block"),
        notifications = builder.notificationRegistrations.toList(),
    )
}

// ── Pipeline behavior DSL ────────────────────────────────────────────────────

@PublishedApi
internal class LambdaPipelineBehavior(
    val stage: Stage,
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
    stage: Stage = Stage.Default,
    order: Int = 0,
    isEnabled: Boolean = true,
    appliesTo: (Request<*>) -> Boolean = { true },
    block: suspend (requestContext: RequestContext, next: suspend (Request<*>) -> Any?, request: Request<*>) -> Any?,
): PipelineBehavior = LambdaPipelineBehavior(stage, order, isEnabled, appliesTo, block)

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
