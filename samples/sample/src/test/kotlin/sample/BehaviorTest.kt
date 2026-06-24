package sample

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import sample.behaviors.*
import sample.context.CurrentUser
import sample.context.currentUser
import sample.context.locale
import sample.orders.queries.getorder.GetOrderQuery
import sample.orders.queries.getorder.GetOrderRegistrar
import kotlin.test.*

/** No-mocking integration tests for every sample pipeline behavior. */
class BehaviorTest {

    // ── Shared test request/response ─────────────────────────────────────────

    data class PingRequest(val id: String) : Request<String>

    private fun pingMediator(
        behaviors: List<PipelineBehavior> = emptyList(),
        handler: suspend (PingRequest) -> String = { "pong-${it.id}" },
    ) = MediatorFactory.create(
        registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry register object : RequestHandler<PingRequest, String> {
                    override suspend fun handle(
                        mediator: Mediator,
                        requestContext: RequestContext,
                        request: PingRequest,
                    ) = handler(request)
                }
            }
        }),
        pipelineBehaviors = behaviors,
        verifyHandlers = false,
    )

    // ── RetryPipelineBehavior ─────────────────────────────────────────────────

    @Test
    fun `retry succeeds on first attempt when no error`() = runTest {
        var calls = 0
        val mediator = pingMediator(
            behaviors = listOf(RetryPipelineBehavior(maxRetries = 3, delayMs = 0)),
            handler = { calls++; "ok" },
        )
        assertEquals("ok", mediator.send(PingRequest("1")))
        assertEquals(1, calls)
    }

    @Test
    fun `retry succeeds after transient failures`() = runTest {
        var calls = 0
        val mediator = pingMediator(
            behaviors = listOf(RetryPipelineBehavior(maxRetries = 3, delayMs = 0)),
            handler = {
                calls++
                if (calls < 3) throw RuntimeException("transient")
                "recovered"
            },
        )
        assertEquals("recovered", mediator.send(PingRequest("1")))
        assertEquals(3, calls)
    }

    @Test
    fun `retry exhausts all attempts and rethrows last exception`() = runTest {
        var calls = 0
        val mediator = pingMediator(
            behaviors = listOf(RetryPipelineBehavior(maxRetries = 2, delayMs = 0)),
            handler = { calls++; throw RuntimeException("always fails") },
        )
        val ex = assertFailsWith<RuntimeException> { mediator.send(PingRequest("1")) }
        assertEquals("always fails", ex.message)
        assertEquals(3, calls) // 1 initial + 2 retries
    }

    // ── RateLimitPipelineBehavior ─────────────────────────────────────────────

    @Test
    fun `rate limit allows requests within quota`() = runTest {
        val mediator = pingMediator(
            behaviors = listOf(RateLimitPipelineBehavior(maxRequests = 5, windowMs = 30_000)),
        )
        repeat(5) { assertEquals("pong-1", mediator.send(PingRequest("1"))) }
    }

    @Test
    fun `rate limit rejects request that exceeds quota`() = runTest {
        val mediator = pingMediator(
            behaviors = listOf(RateLimitPipelineBehavior(maxRequests = 2, windowMs = 30_000)),
        )
        mediator.send(PingRequest("1"))
        mediator.send(PingRequest("1"))
        val ex = assertFailsWith<RuntimeException> { mediator.send(PingRequest("1")) }
        assertTrue(ex.message!!.contains("Rate limit"))
    }

    // ── CircuitBreakerPipelineBehavior ────────────────────────────────────────

    @Test
    fun `circuit breaker passes calls through when healthy`() = runTest {
        val breaker = CircuitBreakerPipelineBehavior(failureThreshold = 3, resetTimeoutMs = 5_000)
        val mediator = pingMediator(behaviors = listOf(breaker))
        assertEquals("pong-1", mediator.send(PingRequest("1")))
    }

    @Test
    fun `circuit breaker opens after reaching failure threshold`() = runTest {
        val states = mutableListOf<CircuitState>()
        val breaker = CircuitBreakerPipelineBehavior(
            failureThreshold = 2,
            resetTimeoutMs = 5_000,
            onStateChange = { states += it },
        )
        val mediator = pingMediator(
            behaviors = listOf(breaker),
            handler = { throw RuntimeException("downstream down") },
        )
        repeat(2) { runCatching { mediator.send(PingRequest("1")) } }

        val ex = assertFailsWith<RuntimeException> { mediator.send(PingRequest("1")) }
        assertTrue(ex.message!!.contains("OPEN"))
        assertTrue(CircuitState.OPEN in states)
    }

    // ── DeduplicationPipelineBehavior ─────────────────────────────────────────

    @Test
    fun `deduplication passes single request through normally`() = runTest {
        val dedup = DeduplicationPipelineBehavior()
        val mediator = pingMediator(behaviors = listOf(dedup))
        assertEquals("pong-1", mediator.send(PingRequest("1")))
    }

    @Test
    fun `deduplication coalesces concurrent identical requests to one handler call`() = runTest {
        var handlerCalls = 0
        val dedup = DeduplicationPipelineBehavior()
        val mediator = pingMediator(
            behaviors = listOf(dedup),
            handler = {
                handlerCalls++
                delay(50)
                "shared-${it.id}"
            },
        )
        val req = PingRequest("dup")
        val results = coroutineScope {
            listOf(
                async { mediator.send(req) },
                async { mediator.send(req) },
                async { mediator.send(req) },
            ).map { it.await() }
        }
        assertTrue(results.all { it == "shared-dup" })
        assertEquals(1, handlerCalls, "handler should only run once for identical concurrent requests")
    }

    // ── AuthorizationPipelineBehavior ─────────────────────────────────────────

    @Test
    fun `authorization behavior is skipped for non-AuthenticatedRequest`() = runTest {
        val auth = AuthorizationPipelineBehavior { _, _ -> throw UnauthorizedException("should not be called") }
        val mediator = pingMediator(behaviors = listOf(auth))
        assertEquals("pong-1", mediator.send(PingRequest("1")))
    }

    @Test
    fun `authorization throws UnauthorizedException for AuthenticatedRequest without token`() = runTest {
        data class SecureRequest(val id: String) : Request<String>, AuthenticatedRequest

        val auth = AuthorizationPipelineBehavior { ctx, _ ->
            ctx.getMetaDate<String>("token") ?: throw UnauthorizedException("No auth token")
        }
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object : RequestHandler<SecureRequest, String> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: SecureRequest,
                        ) = "secret-${request.id}"
                    }
                }
            }),
            pipelineBehaviors = listOf(auth),
            verifyHandlers = false,
        )
        val ex = assertFailsWith<UnauthorizedException> { mediator.send(SecureRequest("x")) }
        assertEquals("No auth token", ex.message)
    }

    // ── GetOrder handler + validator ──────────────────────────────────────────

    @Test
    fun `get order returns details for valid query`() = runTest {
        val mediator = MediatorFactory.create(registrars = listOf(GetOrderRegistrar()), verifyHandlers = false)
        val order = mediator.send(GetOrderQuery(orderId = "ORD-001", customerId = "USR-1"))
        assertEquals("ORD-001", order.orderId)
        assertEquals("USR-1", order.customerId)
    }

    @Test
    fun `get order validation rejects blank orderId`() = runTest {
        val mediator = MediatorFactory.create(registrars = listOf(GetOrderRegistrar()), verifyHandlers = false)
        assertFailsWith<ValidationException> {
            mediator.send(GetOrderQuery(orderId = "", customerId = "USR-1"))
        }
    }

    @Test
    fun `get order validation rejects orderId without ORD- prefix`() = runTest {
        val mediator = MediatorFactory.create(registrars = listOf(GetOrderRegistrar()), verifyHandlers = false)
        assertFailsWith<ValidationException> {
            mediator.send(GetOrderQuery(orderId = "12345", customerId = "USR-1"))
        }
    }

    @Test
    fun `get order validation rejects blank customerId`() = runTest {
        val mediator = MediatorFactory.create(registrars = listOf(GetOrderRegistrar()), verifyHandlers = false)
        assertFailsWith<ValidationException> {
            mediator.send(GetOrderQuery(orderId = "ORD-1", customerId = ""))
        }
    }

    // ── Context-capturing helper ──────────────────────────────────────────────

    private fun contextCapturingMediator(
        behaviors: List<PipelineBehavior> = emptyList(),
        handler: suspend (PingRequest, RequestContext) -> String,
    ) = MediatorFactory.create(
        registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry register object : RequestHandler<PingRequest, String> {
                    override suspend fun handle(
                        mediator: Mediator,
                        requestContext: RequestContext,
                        request: PingRequest,
                    ) = handler(request, requestContext)
                }
            }
        }),
        pipelineBehaviors = behaviors,
        verifyHandlers = false,
    )

    // ── LoggingBehavior ───────────────────────────────────────────────────────

    @Test
    fun `logging behavior passes result through unchanged`() = runTest {
        val mediator = pingMediator(behaviors = listOf(LoggingBehavior()))
        assertEquals("pong-1", mediator.send(PingRequest("1")))
    }

    @Test
    fun `logging behavior has order 1`() {
        assertEquals(1, LoggingBehavior().order)
    }

    @Test
    fun `logging behavior propagates handler exception`() = runTest {
        val mediator = pingMediator(
            behaviors = listOf(LoggingBehavior()),
            handler = { throw RuntimeException("domain error") },
        )
        val ex = assertFailsWith<RuntimeException> { mediator.send(PingRequest("1")) }
        assertEquals("domain error", ex.message)
    }

    // ── TracingPipelineBehavior ───────────────────────────────────────────────

    @Test
    fun `tracing behavior injects traceId into request context`() = runTest {
        var capturedId: String? = null
        val mediator = contextCapturingMediator(behaviors = listOf(TracingPipelineBehavior())) { _, ctx ->
            capturedId = ctx.getMetaDate("traceId")
            "ok"
        }
        mediator.send(PingRequest("t"))
        assertNotNull(capturedId)
        assertEquals(8, capturedId!!.length)
    }

    @Test
    fun `tracing behavior order is negative 50`() {
        assertEquals(-50, TracingPipelineBehavior().order)
    }

    @Test
    fun `tracing behavior result passes through unchanged`() = runTest {
        val mediator = pingMediator(behaviors = listOf(TracingPipelineBehavior()))
        assertEquals("pong-z", mediator.send(PingRequest("z")))
    }

    // ── MetricsBehavior ───────────────────────────────────────────────────────

    @Test
    fun `metrics snapshot is empty before any request`() {
        assertTrue(MetricsBehavior().snapshot().isEmpty())
    }

    @Test
    fun `metrics increments count per request type`() = runTest {
        val metrics = MetricsBehavior()
        val mediator = pingMediator(behaviors = listOf(metrics))
        repeat(3) { mediator.send(PingRequest("m")) }
        assertEquals(3L, metrics.snapshot()["PingRequest"])
    }

    @Test
    fun `metrics behavior order is 100`() {
        assertEquals(100, MetricsBehavior().order)
    }

    @Test
    fun `metrics behavior passes result through unchanged`() = runTest {
        val mediator = pingMediator(behaviors = listOf(MetricsBehavior()))
        assertEquals("pong-m", mediator.send(PingRequest("m")))
    }

    // ── MeasurePipelineBehaviour ──────────────────────────────────────────────

    @Test
    fun `measure behavior returns handler result unchanged`() = runTest {
        val mediator = pingMediator(behaviors = listOf(MeasurePipelineBehaviour()))
        assertEquals("pong-q", mediator.send(PingRequest("q")))
    }

    @Test
    fun `measure behavior order is 10`() {
        assertEquals(10, MeasurePipelineBehaviour().order)
    }

    @Test
    fun `measure behavior propagates exception from handler`() = runTest {
        val mediator = pingMediator(
            behaviors = listOf(MeasurePipelineBehaviour()),
            handler = { throw IllegalStateException("timed-out-domain") },
        )
        assertFailsWith<IllegalStateException> { mediator.send(PingRequest("x")) }
    }

    // ── AuthBehavior ──────────────────────────────────────────────────────────

    @Test
    fun `auth behavior populates currentUser in request context`() = runTest {
        var captured: CurrentUser? = null
        val mediator = contextCapturingMediator(behaviors = listOf(AuthBehavior())) { _, ctx ->
            captured = ctx.currentUser
            "ok"
        }
        mediator.send(PingRequest("a"))
        assertNotNull(captured)
        assertEquals("Alice", captured!!.name)
        assertEquals("user-123", captured!!.id)
        assertTrue("admin" in captured!!.roles)
    }

    @Test
    fun `auth behavior stage is Pre`() {
        assertEquals(Stage.Pre, AuthBehavior().stage)
    }

    // ── LocaleBehavior ────────────────────────────────────────────────────────

    @Test
    fun `locale behavior sets locale in request context`() = runTest {
        var capturedLocale: String? = null
        val mediator = contextCapturingMediator(behaviors = listOf(LocaleBehavior())) { _, ctx ->
            capturedLocale = ctx.locale
            "ok"
        }
        mediator.send(PingRequest("l"))
        assertNotNull(capturedLocale)
        assertTrue(capturedLocale.isNotEmpty())
    }

    @Test
    fun `locale behavior stage is Pre`() {
        assertEquals(Stage.Pre, LocaleBehavior().stage)
    }
}
