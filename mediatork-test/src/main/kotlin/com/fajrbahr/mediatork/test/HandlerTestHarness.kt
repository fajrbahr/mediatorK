package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ParallelNotificationPublisher
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import kotlinx.coroutines.flow.Flow

/**
 * A scoped execution context for handler integration tests.
 *
 * [HandlerTestHarness] wires a real [Mediator] with real handlers and the full pipeline,
 * reproducing the same execution path used in production. Use it to test slices end-to-end:
 * set up state through the front door with [given], trigger the action under test with
 * [send], and assert outcomes via follow-up queries with [query].
 *
 * Prefer [given] over direct repository inserts for setup — it exercises the same command
 * handlers used in production and ensures test state remains valid as the domain evolves.
 * Drop down to direct inserts only when you need to arrange state that is impossible or
 * prohibitively expensive to reach through the front door.
 *
 * ```kotlin
 * @Test
 * fun `approved invoice appears in approved list`() = runTest {
 *     val harness = buildHandlerTestHarness {
 *         +CreateInvoiceHandler(repo)
 *         +ApproveInvoiceHandler(repo)
 *         +ListApprovedInvoicesHandler(repo)
 *     }
 *     harness.given(CreateInvoice(amount = 100))
 *     val id = harness.send(CreateInvoice(amount = 200)).id
 *     harness.send(ApproveInvoice(id = id))
 *     val approved = harness.query(ListApprovedInvoices())
 *     assertTrue(approved.any { it.id == id })
 * }
 * ```
 *
 * @see buildHandlerTestHarness
 */
class HandlerTestHarness(private val mediator: Mediator) {

    /**
     * Sends one or more setup requests through the mediator to arrange preconditions.
     *
     * Each request traverses the full pipeline with real handlers, matching production
     * behaviour. Return values are discarded; use [send] or [query] when you need the result.
     */
    @Suppress("UNCHECKED_CAST", "RedundantSuppression")
    suspend fun given(vararg requests: Request<*>) {
        requests.forEach { mediator.send(it as Request<Any?>) }
    }

    /**
     * Sends [request] through the full mediator pipeline and returns the result.
     *
     * This is the action under test. Call [given] first to arrange preconditions, and
     * [query] afterwards to assert the outcome.
     */
    suspend fun <TResult> send(request: Request<TResult>): TResult = mediator.send(request)

    /**
     * Sends a read request through the full mediator pipeline and returns the result.
     *
     * Semantically identical to [send] but signals intent: this call is part of the
     * assertion phase, not the action under test.
     */
    suspend fun <TResult> query(request: Request<TResult>): TResult = mediator.send(request)

    /**
     * Dispatches a [StreamRequest] and returns the cold [Flow].
     *
     * Collect the flow inside your test to consume items, e.g.:
     * ```kotlin
     * val items = harness.stream(StreamOrdersQuery("USR-1")).toList()
     * ```
     */
    fun <T> stream(request: StreamRequest<T>): Flow<T> = mediator.stream(request)
}

/**
 * Builds a [HandlerTestHarness] backed by a real [Mediator] wired with the given handlers,
 * behaviors, and processors.
 *
 * Register handlers in [init] using the same DSL as [HandlerRegistry]:
 *
 * ```kotlin
 * val harness = buildHandlerTestHarness(
 *     pipelineBehaviors = listOf(ValidationBehavior(listOf(CreateOrderValidator()))),
 * ) {
 *     +CreateOrderHandler(orderRepo)
 *     +GetOrderHandler(orderRepo)
 * }
 * ```
 *
 * @param pipelineBehaviors cross-cutting behaviors to include in the pipeline.
 *   Use [PipelineBehavior.Tag.Pre] / [PipelineBehavior.Tag.Post] to control phase ordering.
 * @param notificationPublisher strategy for delivering notifications; defaults to parallel.
 * @param registrars additional [MediatorRegistrar]s that contribute handlers.
 * @param init DSL block for registering handlers directly on the [HandlerRegistry].
 */
fun buildHandlerTestHarness(
    pipelineBehaviors: List<PipelineBehavior> = emptyList(),
    notificationPublisher: NotificationPublishStrategy = ParallelNotificationPublisher(),
    registrars: List<MediatorRegistrar> = emptyList(),
    init: HandlerRegistry.() -> Unit = {},
): HandlerTestHarness {
    val allRegistrars = registrars + object : MediatorRegistrar {
        override fun register(registry: HandlerRegistry) = registry.init()
    }
    val mediator = MediatorFactory.create(
        registrars = allRegistrars,
        pipelineBehaviors = pipelineBehaviors,
        notificationPublisher = notificationPublisher,
    )
    return HandlerTestHarness(mediator)
}
