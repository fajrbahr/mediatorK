@file:Suppress("TooManyFunctions")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.api.StreamPipelineBehavior
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.handler.ThrowMissingRequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Scope-limiting marker for the [mediatorK] builder DSL.
 *
 * Prevents accidentally calling builder registration functions from inside a
 * lambda handler body (and vice versa) via an implicit outer receiver.
 */
@DslMarker
annotation class MediatorKDsl

/**
 * Receiver for lambda handlers registered via [handle] and [handleStream].
 *
 * Implements [Mediator] by delegation, so a handler body can call [send][com.fajrbahr.mediatork.handler.Sender.send]
 * and [publish][com.fajrbahr.mediatork.notification.Publisher.publish] directly:
 *
 * ```kotlin
 * handle<CreateOrderCommand, Order> { request ->
 *     val order = db.save(Order(request.id, request.amount))
 *     publish(OrderCreatedEvent(order.id))   // `this` is the mediator
 *     order
 * }
 * ```
 *
 * @property mediator the active mediator, for nested sends or publishes.
 * @property context mutable bag scoped to this pipeline execution; values set by
 *   pipeline behaviors and pre-processors are read via [RequestContext.getMetaData].
 */
@MediatorKDsl
class HandlerScope(
    val mediator: Mediator,
    val context: RequestContext,
) : Mediator by mediator

// ── Lambda adapters ───────────────────────────────────────────────────────────

@PublishedApi
internal class LambdaRequestHandler<TRequest : Request<TResult>, TResult>(
    private val block: suspend HandlerScope.(TRequest) -> TResult,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult = HandlerScope(mediator, requestContext).block(request)
}

@PublishedApi
internal class LambdaStreamRequestHandler<TRequest : StreamRequest<T>, T>(
    private val block: HandlerScope.(TRequest) -> Flow<T>,
) : StreamRequestHandler<TRequest, T> {
    override fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): Flow<T> = HandlerScope(mediator, requestContext).block(request)
}

@PublishedApi
internal class LambdaNotificationHandler<T : Notification>(
    override val order: Int,
    private val block: suspend (T) -> Unit,
) : NotificationHandler<T> {
    override suspend fun handle(notification: T) = block(notification)
}

@PublishedApi
internal class LambdaRequestValidator<TRequest : Any>(
    private val block: (TRequest) -> ValidationResult,
) : RequestValidator<TRequest> {
    override fun validate(request: TRequest): ValidationResult = block(request)
}

// ── Lambda registration on HandlerRegistry ────────────────────────────────────

/**
 * Registers an inline lambda as the handler for request type [TRequest] —
 * no [RequestHandler] class needed:
 *
 * ```kotlin
 * registry.handle<GetTodoQuery, Todo?> { request -> db.find(request.id) }
 * ```
 *
 * The lambda runs with a [HandlerScope] receiver, so `send`/`publish` and
 * [HandlerScope.context] are available directly. Replaces any previously
 * registered handler for the same type, exactly like [HandlerRegistry.register].
 */
inline fun <reified TRequest : Request<TResult>, TResult> HandlerRegistry.handle(
    noinline block: suspend HandlerScope.(TRequest) -> TResult,
): HandlerRegistry = register(LambdaRequestHandler(block))

/**
 * Registers an inline lambda as the stream handler for [TRequest]:
 *
 * ```kotlin
 * registry.handleStream<WatchOrdersQuery, Order> { request -> db.observeOrders(request.filter) }
 * ```
 *
 * The lambda must return a cold [Flow]; work starts only on collection,
 * exactly like [HandlerRegistry.registerStream].
 */
inline fun <reified TRequest : StreamRequest<T>, T> HandlerRegistry.handleStream(
    noinline block: HandlerScope.(TRequest) -> Flow<T>,
): HandlerRegistry = registerStream(LambdaStreamRequestHandler(block))

/**
 * Registers an inline lambda as a notification handler for [T] —
 * no [NotificationHandler] class needed:
 *
 * ```kotlin
 * registry.on<OrderCreatedEvent> { event -> emailService.send(event.orderId) }
 * ```
 *
 * Multiple handlers may be registered for the same notification type; [order]
 * controls their relative execution order (lower runs first, default `0`).
 */
inline fun <reified T : Notification> HandlerRegistry.on(
    order: Int = 0,
    noinline block: suspend (T) -> Unit,
): HandlerRegistry = registerNotification(LambdaNotificationHandler(order, block))

/**
 * Registers an inline lambda as a validator for request type [TRequest]:
 *
 * ```kotlin
 * registry.validate<CreateOrderCommand> { request ->
 *     rules<CreateOrderCommand> {
 *         check(request.amount > 0) { "Amount must be positive" }
 *     }
 * }
 * ```
 *
 * Runs before the handler via [com.fajrbahr.mediatork.validator.ValidationBehavior].
 */
inline fun <reified TRequest : Request<*>> HandlerRegistry.validate(
    noinline block: (TRequest) -> ValidationResult,
): HandlerRegistry = registerValidator(LambdaRequestValidator(block))

// ── Builder ───────────────────────────────────────────────────────────────────

/**
 * Builder behind [mediatorK]. Collects handlers, behaviors, and configuration,
 * then assembles a [Mediator] via [MediatorFactory].
 *
 * All of [HandlerRegistry]'s registration surface is available directly in the
 * builder block — class-based handlers via [register] or the `+handler` operator,
 * lambda handlers via [handle], [handleStream], [on], and [validate].
 */
@MediatorKDsl
class MediatorBuilder @PublishedApi internal constructor() {

    /** The registry being populated; exposed for advanced/dynamic registration. */
    val registry: HandlerRegistry = HandlerRegistry()

    private val registrars = mutableListOf<MediatorRegistrar>()
    private val pipelineBehaviors = mutableListOf<PipelineBehavior>()
    private val streamPipelineBehaviors = mutableListOf<StreamPipelineBehavior>()

    /** Strategy for delivering notifications to their handlers. Defaults to parallel delivery. */
    var notificationPublisher: NotificationPublishStrategy =
        NotificationPublishStrategy.ParallelNotificationPublisher()

    /** When `true` (the default), logs a warning for every request type without a handler. */
    var verifyHandlers: Boolean = true

    /** Fallback invoked when a notification has no registered handler. Throws by default. */
    var missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler()

    /** Fallback invoked when a request has no registered handler. Throws by default. */
    var missingRequestHandler: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler()

    /** Adds [MediatorRegistrar]s — including KSP-generated ones — to populate the registry. */
    fun registrars(vararg registrars: MediatorRegistrar) {
        this.registrars += registrars
    }

    /** Adds cross-cutting [PipelineBehavior]s wrapping every `send` call. */
    fun behaviors(vararg behaviors: PipelineBehavior) {
        pipelineBehaviors += behaviors
    }

    /** Adds cross-cutting [StreamPipelineBehavior]s wrapping every `stream` call. */
    fun streamBehaviors(vararg behaviors: StreamPipelineBehavior) {
        streamPipelineBehaviors += behaviors
    }

    /** Registers a class-based request handler. */
    inline fun <reified TRequest : Request<TResult>, TResult> register(
        handler: RequestHandler<TRequest, TResult>,
    ) {
        registry register handler
    }

    /** Registers a class-based stream request handler. */
    inline fun <reified TRequest : StreamRequest<T>, T> register(
        handler: StreamRequestHandler<TRequest, T>,
    ) {
        registry registerStream handler
    }

    /** Registers a class-based notification handler. */
    inline fun <reified T : Notification> register(handler: NotificationHandler<T>) {
        registry registerNotification handler
    }

    /** Registers a class-based validator for [TRequest]. */
    inline fun <reified TRequest : Request<*>> register(validator: RequestValidator<TRequest>) {
        registry.registerValidator(validator)
    }

    /** Shorthand for [register]: `+MyHandler()`. */
    inline operator fun <reified TRequest : Request<TResult>, TResult> RequestHandler<TRequest, TResult>.unaryPlus() =
        register(this)

    /** Shorthand for [register]: `+MyStreamHandler()`. */
    inline operator fun <reified TRequest : StreamRequest<T>, T> StreamRequestHandler<TRequest, T>.unaryPlus() =
        register(this)

    /** Shorthand for [register]: `+MyNotificationHandler()`. */
    inline operator fun <reified T : Notification> NotificationHandler<T>.unaryPlus() =
        register(this)

    /** Shorthand for [register]: `+MyValidator()`. */
    inline operator fun <reified TRequest : Request<*>> RequestValidator<TRequest>.unaryPlus() =
        register(this)

    /** Registers a [Feature]'s handler and optional validator. */
    inline fun <reified TRequest : Request<TResult>, TResult> register(feature: Feature<TRequest, TResult>) {
        registry.registerFeature(feature)
    }

    /** Shorthand for [register]: `+myFeature`. */
    inline operator fun <reified TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.unaryPlus() =
        register(this)

    /** Lambda request handler — see [HandlerRegistry.handle]. */
    inline fun <reified TRequest : Request<TResult>, TResult> handle(
        noinline block: suspend HandlerScope.(TRequest) -> TResult,
    ) {
        registry.handle(block)
    }

    /** Lambda stream handler — see [HandlerRegistry.handleStream]. */
    inline fun <reified TRequest : StreamRequest<T>, T> handleStream(
        noinline block: HandlerScope.(TRequest) -> Flow<T>,
    ) {
        registry.handleStream(block)
    }

    /** Lambda notification handler — see [HandlerRegistry.on]. */
    inline fun <reified T : Notification> on(
        order: Int = 0,
        noinline block: suspend (T) -> Unit,
    ) {
        registry.on(order, block)
    }

    /** Lambda validator — see [HandlerRegistry.validate]. */
    inline fun <reified TRequest : Request<*>> validate(
        noinline block: (TRequest) -> ValidationResult,
    ) {
        registry.validate(block)
    }

    @PublishedApi
    internal fun build(): Mediator {
        registrars.forEach { it.register(registry) }

        if (verifyHandlers) {
            registry.verify { typeName ->
                println("MEDIATOR WARNING: No handler registered for '$typeName'")
            }
        }

        return MediatorFactory.create(
            registry = registry,
            pipelineBehaviors = pipelineBehaviors,
            streamPipelineBehaviors = streamPipelineBehaviors,
            notificationPublisher = notificationPublisher,
            missingNotificationHandler = missingNotificationHandler,
            missingRequestHandler = missingRequestHandler,
        )
    }
}

/**
 * Builds a [Mediator] with a single expressive block — the smoothest way to get
 * started, no registrar or handler classes required:
 *
 * ```kotlin
 * val mediator = mediatorK {
 *     handle<GetTodoQuery, Todo?> { request -> db.find(request.id) }
 *
 *     handle<AddTodoCommand, Todo> { request ->
 *         val todo = db.save(Todo(request.id, request.title))
 *         publish(TodoAddedNotification(todo.id))
 *         todo
 *     }
 *
 *     on<TodoAddedNotification> { event -> log.info("added ${event.id}") }
 *
 *     validate<AddTodoCommand> { request ->
 *         rules<AddTodoCommand> { check(request.title.isNotBlank()) { "Title required" } }
 *     }
 *
 *     behaviors(LoggingPipelineBehavior())
 * }
 *
 * val todo = mediator.send(AddTodoCommand("1", "write docs"))
 * ```
 *
 * Class-based handlers and existing [MediatorRegistrar]s plug into the same block
 * via [MediatorBuilder.register] / `+handler` and [MediatorBuilder.registrars],
 * so both styles can be mixed freely.
 *
 * Equivalent to configuring [MediatorFactory.create] manually.
 */
fun mediatorK(block: MediatorBuilder.() -> Unit): Mediator = MediatorBuilder().apply(block).build()
