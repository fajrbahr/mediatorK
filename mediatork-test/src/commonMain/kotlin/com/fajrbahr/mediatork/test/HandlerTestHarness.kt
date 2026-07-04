package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ParallelNotificationPublisher
import kotlinx.coroutines.flow.Flow

/**
 * A scoped execution context for handler integration tests.
 *
 * [HandlerTestHarness] wires a real [com.fajrbahr.mediatork.api.Mediator] with real handlers and the full pipeline,
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
     * Dispatches a [com.fajrbahr.mediatork.api.StreamRequest] and returns the cold [Flow].
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
 *     pipelineBehaviors = listOf(validationBehavior(mapOf(CreateOrderCommand::class to listOf(CreateOrderValidator())))),
 * ) {
 *     +CreateOrderHandler(orderRepo)
 *     +GetOrderHandler(orderRepo)
 * }
 * ```
 *
 * @param pipelineBehaviors cross-cutting behaviors to include in the pipeline.
 *   Use [com.fajrbahr.mediatork.api.Stage.Pre] / [com.fajrbahr.mediatork.api.Stage.Post] to control phase ordering.
 * @param streamPipelineBehaviors cross-cutting behaviors wrapping stream handlers.
 * @param notificationPublisher strategy for delivering notifications; defaults to parallel.
 * @param init DSL block for registering handlers directly on the [HandlerRegistry].
 */
/**
 * Builds a [HandlerTestHarness] using the full [mediatorK] builder DSL.
 *
 * This gives the test the same pipeline as production — registrars, behaviors,
 * notification publishers, and features with bundled validators/behaviors — just
 * swap in test dependencies:
 *
 * ```kotlin
 * val harness = buildHandlerTestHarness {
 *     behaviors(LoggingPipelineBehavior(), MeasurePipelineBehaviour())
 *     +watchPriceFeature(testRepo)
 * }
 * harness.send(GetPriceQuery(productId = "PROD-1"))
 * ```
 */
fun buildHandlerTestHarness(
    block: MediatorBuilder.() -> Unit,
): HandlerTestHarness = HandlerTestHarness(mediatorK(block))

/**
 * Builds a [HandlerTestHarness] backed by an existing production [Mediator],
 * overriding only the handlers or notifications specified in [overrides].
 *
 * Non-overridden requests flow through the full production pipeline unchanged.
 *
 * ```kotlin
 * val harness = buildHandlerTestHarness(base = productionMediator) {
 *     handle<GetPriceQuery, FormattedPrice> { FormattedPrice("$0.00") }
 *     on<OrderCreatedNotification> { captured += it }
 * }
 * harness.send(GetPriceQuery("PROD-1"))  // → override
 * harness.send(GetOrderQuery("ORD-1"))   // → production
 * ```
 */
fun buildHandlerTestHarness(
    base: Mediator,
    overrides: HandlerRegistry.() -> Unit = {},
): HandlerTestHarness = HandlerTestHarness(base.forTesting(overrides))
