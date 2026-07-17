package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

// ── Type aliases ─────────────────────────────────────────────────────────────

typealias Handler<TRequest, TResult> = suspend HandlerScope.(TRequest) -> TResult
typealias NotificationHandler<T> = suspend (T) -> Unit
typealias StreamHandler<TRequest, T> = HandlerScope.(TRequest) -> Flow<T>
typealias Validator<TRequest> = (TRequest) -> ValidationResult

typealias Interceptor = suspend (request: Request<*>, context: RequestContext, next: suspend () -> Any?) -> Any?
typealias StreamInterceptor = (request: StreamRequest<*>, context: RequestContext, next: () -> Flow<*>) -> Flow<*>

// ── Behavior ─────────────────────────────────────────────────────────────────
// A behavior is plain data: when it runs (order/isEnabled/appliesTo) plus the function
// that wraps the next step. No inheritance, no overridable method — stateful behaviors
// close over their own state and expose it through a control surface (see Behaviors.kt).

open class Behavior(
    val order: Int = 0,
    val isEnabled: Boolean = true,
    val appliesTo: (Request<*>) -> Boolean = { true },
    val process: Interceptor,
)

fun behavior(
    order: Int = 0,
    isEnabled: Boolean = true,
    appliesTo: (Request<*>) -> Boolean = { true },
    process: Interceptor,
): Behavior = Behavior(order, isEnabled, appliesTo, process)

class StreamBehavior(
    val order: Int = 0,
    val isEnabled: Boolean = true,
    val appliesTo: (StreamRequest<*>) -> Boolean = { true },
    val process: StreamInterceptor,
)

fun streamBehavior(
    order: Int = 0,
    isEnabled: Boolean = true,
    appliesTo: (StreamRequest<*>) -> Boolean = { true },
    process: StreamInterceptor,
): StreamBehavior = StreamBehavior(order, isEnabled, appliesTo, process)

// ── MediatorConfig ───────────────────────────────────────────────────────────

internal class MediatorConfig(
    val handlers: Map<KClass<*>, suspend HandlerScope.(Any) -> Any?>,
    val streamHandlers: Map<KClass<*>, HandlerScope.(Any) -> Flow<*>>,
    val notificationListeners: Map<KClass<*>, List<Pair<Int, suspend (Any) -> Unit>>>,
    val behaviors: List<Behavior>,
    val streamBehaviors: List<StreamBehavior>,
    val notificationPublisher: NotificationPublishStrategy,
    val onMissingHandler: (Request<*>) -> Nothing,
    val onMissingNotificationHandler: (Notification) -> Unit,
)

// ── DSL marker ───────────────────────────────────────────────────────────────

@DslMarker
annotation class MediatorDsl

// ── Builder ──────────────────────────────────────────────────────────────────

@MediatorDsl
class MediatorBuilder {

    @PublishedApi
    internal val handlers = mutableMapOf<KClass<*>, suspend HandlerScope.(Any) -> Any?>()

    @PublishedApi
    internal val streamHandlers = mutableMapOf<KClass<*>, HandlerScope.(Any) -> Flow<*>>()

    @PublishedApi
    internal val notificationHandlers = mutableMapOf<KClass<*>, MutableList<Pair<Int, suspend (Any) -> Unit>>>()

    @PublishedApi
    internal val validators = mutableMapOf<KClass<*>, MutableList<(Any) -> ValidationResult>>()

    private val _behaviors = mutableListOf<Behavior>()
    private val _streamBehaviors = mutableListOf<StreamBehavior>()

    var notificationPublisher: NotificationPublishStrategy = NotificationPublishStrategy.DEFAULT
    var onMissingHandler: ((Request<*>) -> Nothing)? = null
    var onMissingNotification: ((Notification) -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    inline fun <reified TRequest : Request<TResult>, TResult> handle(
        noinline handler: suspend HandlerScope.(TRequest) -> TResult,
    ) {
        handlers[TRequest::class] = handler as suspend HandlerScope.(Any) -> Any?
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified TRequest : StreamRequest<T>, T> handleStream(
        noinline handler: HandlerScope.(TRequest) -> Flow<T>,
    ) {
        streamHandlers[TRequest::class] = handler as HandlerScope.(Any) -> Flow<*>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Notification> notification(
        order: Int = 0,
        noinline notificationHandler: suspend (T) -> Unit,
    ) {
        notificationHandlers.getOrPut(T::class) { mutableListOf() }
            .add(order to (notificationHandler as suspend (Any) -> Unit))
    }

    /**
     * Registers a validator that runs (via a pre-handler behavior) before the handler for
     * [TRequest]. Note: validators apply to `send` requests only — `stream` requests are not
     * validated through this mechanism.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified TRequest : Request<*>> validate(
        noinline validator: (TRequest) -> ValidationResult,
    ) {
        validators.getOrPut(TRequest::class) { mutableListOf() }
            .add(validator as (Any) -> ValidationResult)
    }

    fun behaviors(vararg behaviors: Behavior) {
        _behaviors.addAll(behaviors)
    }

    fun streamBehaviors(vararg behaviors: StreamBehavior) {
        _streamBehaviors.addAll(behaviors)
    }

    internal fun build(): MediatorConfig {
        val frozenValidators = validators.mapValues { it.value.toList() }

        val validationBehavior = behavior(order = -50) { request, _, next ->
            val selfResult = request.validate()
            if (selfResult is ValidationResult.Invalid) throw ValidationException(selfResult.errors)

            frozenValidators[request::class]?.forEach { validator ->
                val result = validator(request)
                if (result is ValidationResult.Invalid) throw ValidationException(result.errors)
            }
            next()
        }

        return MediatorConfig(
            handlers = handlers.toMap(),
            streamHandlers = streamHandlers.toMap(),
            notificationListeners = notificationHandlers.mapValues { it.value.toList() },
            behaviors = listOf(validationBehavior) + _behaviors,
            streamBehaviors = _streamBehaviors.toList(),
            notificationPublisher = notificationPublisher,
            onMissingHandler = onMissingHandler ?: { req ->
                throw MissingHandlerException(
                    requestTypeName = req::class.simpleName ?: "Unknown",
                    registered = handlers.keys.mapNotNull { it.simpleName },
                )
            },
            onMissingNotificationHandler = onMissingNotification ?: { notif ->
                throw MissingNotificationHandlerException(
                    notificationTypeName = notif::class.simpleName ?: "Unknown",
                )
            },
        )
    }
}

fun mediatorK(block: MediatorBuilder.() -> Unit): Mediator {
    val config = MediatorBuilder().apply(block).build()
    return MediatorImpl(config)
}
