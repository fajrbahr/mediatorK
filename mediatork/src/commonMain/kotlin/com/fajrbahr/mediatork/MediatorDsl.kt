@file:Suppress("TooManyFunctions")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.StreamFeature
import com.fajrbahr.mediatork.handler.ThrowMissingRequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.flow.Flow

// ── Missing handler strategies ───────────────────────────────────────────────

/** Strategy: Throw exception on missing request handlers (safe, clear error). */
val missingRequestHandlerThrow: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler()

/** Strategy: Silently ignore unhandled notifications (data loss risk). */
val missingNotificationHandlerSilent: NotificationHandler<Notification> = SilentMissingNotificationHandler()

/** Strategy: Throw exception on unhandled notifications (safe, clear error). */
val missingNotificationHandlerThrow: NotificationHandler<Notification> = ThrowMissingNotificationHandler()

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
 * lambda handlers via [handle], [handleStream], and [validate].
 */
@MediatorKDsl
class MediatorBuilder constructor() {

    /** The registry being populated; exposed for advanced/dynamic registration. */
    val registry: HandlerRegistry = HandlerRegistry()

    @PublishedApi
    internal val registrars = mutableListOf<Any>()
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

    /** Configures a custom fallback for missing request handlers using a lambda. */
    fun missingRequestHandler(block: suspend HandlerScope.(Request<*>) -> Any?) {
        missingRequestHandler = LambdaRequestHandler { req -> block(req) }
    }

    /** Configures a custom fallback for missing notification handlers using a lambda. */
    fun missingNotificationHandler(block: suspend (Notification) -> Unit) {
        missingNotificationHandler = LambdaNotificationHandler(0, block)
    }

    /**
     * Applies a reusable registrar block created via [mediatorRegistrar].
     *
     * ```kotlin
     * val productRegistrar = mediatorRegistrar { +GetPriceHandler() }
     * mediatorK {
     *     registrar(productRegistrar)
     * }
     * ```
     */
    fun registrar(block: MediatorBuilder.() -> Unit) {
        block()
    }

    /** Adds cross-cutting behaviors wrapping both `send` and `stream` calls. Automatically routes to the correct collection. */
    fun behaviors(vararg behaviors: Behavior) {
        for (behavior in behaviors) {
            when (behavior) {
                is PipelineBehavior -> pipelineBehaviors += behavior
                is StreamPipelineBehavior -> streamPipelineBehaviors += behavior
            }
        }
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

    /** Registers a [Feature]'s handler, validators, notifications, and behaviors. */
    inline fun <reified TRequest : Request<TResult>, TResult> register(feature: Feature<TRequest, TResult>) {
        registry.registerFeature(feature)
    }

    /** Support direct invocation: `myFeature()` inside builder. */
    inline operator fun <reified TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.invoke() =
        register(this)

    /** Registers a [StreamFeature]'s stream handler and notifications. */
    inline fun <reified TRequest : StreamRequest<T>, T> register(feature: StreamFeature<TRequest, T>) {
        registry.registerStreamFeature(feature)
    }

    /** Support direct invocation: `myStreamFeature()` inside builder. */
    inline operator fun <reified TRequest : StreamRequest<T>, T> StreamFeature<TRequest, T>.invoke() =
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


    /** Lambda validator — see [HandlerRegistry.validate]. */
    inline fun <reified TRequest : Request<*>> validate(
        noinline block: (TRequest) -> ValidationResult,
    ) {
        registry.validate(block)
    }

    fun build(): Mediator {
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

/**
 * Creates a reusable group of registrations that can be applied to a Mediator builder.
 */
fun mediatorRegistrar(block: MediatorBuilder.() -> Unit): MediatorBuilder.() -> Unit = block

// ── Runtime registration on Mediator ──────────────────────────────────────────

/**
 * Adds a request handler at runtime:
 *
 * ```kotlin
 * val mediator = mediatorK { ... }
 * mediator.handler(GetTodoHandler())
 * ```
 */
inline fun <reified TRequest : Request<TResult>, TResult> Mediator.handler(
    handler: RequestHandler<TRequest, TResult>,
): Mediator {
    registry.register(handler)
    return this
}

/**
 * Adds a stream request handler at runtime:
 *
 * ```kotlin
 * mediator.handler(WatchOrdersHandler())
 * ```
 */
inline fun <reified TRequest : StreamRequest<T>, T> Mediator.handler(
    handler: StreamRequestHandler<TRequest, T>,
): Mediator {
    registry.registerStream(handler)
    return this
}

/**
 * Adds a notification handler at runtime:
 *
 * ```kotlin
 * mediator.handler(OrderCreatedHandler())
 * ```
 */
inline fun <reified T : Notification> Mediator.handler(handler: NotificationHandler<T>): Mediator {
    registry.registerNotification(handler)
    return this
}

/**
 * Adds a lambda request handler at runtime:
 *
 * ```kotlin
 * mediator.handle<GetTodoQuery, Todo?> { request -> db.find(request.id) }
 * ```
 */
inline fun <reified TRequest : Request<TResult>, TResult> Mediator.handle(
    noinline block: suspend HandlerScope.(TRequest) -> TResult,
): Mediator {
    registry.handle(block)
    return this
}

/**
 * Adds a lambda stream handler at runtime:
 *
 * ```kotlin
 * mediator.handleStream<WatchOrdersQuery, Order> { request -> db.observeOrders() }
 * ```
 */
inline fun <reified TRequest : StreamRequest<T>, T> Mediator.handleStream(
    noinline block: HandlerScope.(TRequest) -> Flow<T>,
): Mediator {
    registry.handleStream(block)
    return this
}

/**
 * Adds a lambda notification handler at runtime:
 *
 * ```kotlin
 * mediator.on<OrderCreatedEvent> { event -> emailService.send(event.orderId) }
 * ```
 */
inline fun <reified T : Notification> Mediator.on(
    order: Int = 0,
    noinline block: suspend (T) -> Unit,
): Mediator {
    registry.on(order, block)
    return this
}

