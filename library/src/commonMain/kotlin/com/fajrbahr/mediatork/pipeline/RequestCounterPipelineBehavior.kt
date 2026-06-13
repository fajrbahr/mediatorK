package com.fajrbahr.mediatork.pipeline

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [PipelineBehavior] that counts how many times each request type has passed through
 * the pipeline. Counts survive across multiple `send` calls for the lifetime of this
 * behavior instance.
 *
 * ```kotlin
 * val counter = RequestCounterPipelineBehavior()
 *
 * val mediator = MediatorFactory.create(
 *     registrars = listOf(AppRegistrar()),
 *     pipelineBehaviors = listOf(counter),
 * )
 *
 * mediator.send(GetUserQuery(id = 1))
 * mediator.send(GetUserQuery(id = 2))
 * mediator.send(CreateOrderCommand(...))
 *
 * counter.countFor(GetUserQuery::class)   // 2
 * counter.countFor(CreateOrderCommand::class) // 1
 * counter.snapshot()  // map of all request names → counts
 * ```
 *
 * Thread-safe: uses a [Mutex] so concurrent `send` calls never race on the counter map.
 *
 * @param order position in the behavior chain. Defaults to `0`.
 */
class RequestCounterPipelineBehavior(
    override val order: Int = 0,
) : PipelineBehavior {

    private val mutex = Mutex()
    private val counts = mutableMapOf<String, Long>()

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val key = request::class.simpleName ?: request::class.toString()
        mutex.withLock { counts[key] = (counts[key] ?: 0L) + 1L }
        return next(request)
    }

    /** Returns the number of times [requestClass] has been dispatched. */
    suspend fun countFor(requestClass: kotlin.reflect.KClass<*>): Long {
        val key = requestClass.simpleName ?: requestClass.toString()
        return mutex.withLock { counts[key] ?: 0L }
    }

    /** Returns a snapshot of all counts keyed by request class simple name. */
    suspend fun snapshot(): Map<String, Long> = mutex.withLock { counts.toMap() }

    /** Resets all counters to zero. */
    suspend fun reset() = mutex.withLock { counts.clear() }
}
