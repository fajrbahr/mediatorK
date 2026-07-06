package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.behavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineBehaviorTest {

    private fun loggingBehavior(order: Int, label: String, log: MutableList<String>) =
        object : PipelineBehavior {
            override val order = order
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "$label-before"
                return next(request).also { log += "$label-after" }
            }
        }

    @Test
    fun `single behavior wraps the handler`() = runTest {
        val log = mutableListOf<String>()
        val m = mediator(pipelineBehaviors = listOf(loggingBehavior(0, "b", log))) {
            handler(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("b-before", "b-after"), log)
    }

    @Test
    fun `lower order behavior is outermost`() = runTest {
        val log = mutableListOf<String>()
        val outer = loggingBehavior(-10, "outer", log)
        val inner = loggingBehavior(10, "inner", log)
        val m = mediator(pipelineBehaviors = listOf(inner, outer)) {
            handler(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("outer-before", "inner-before", "inner-after", "outer-after"), log)
    }

    @Test
    fun `behaviors with equal order run in registered order`() = runTest {
        val log = mutableListOf<String>()
        val b1 = loggingBehavior(0, "b1", log)
        val b2 = loggingBehavior(0, "b2", log)
        val m = mediator(pipelineBehaviors = listOf(b1, b2)) {
            handler(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("b1-before", "b2-before", "b2-after", "b1-after"), log)
    }

    @Test
    fun `behavior with appliesTo=false is skipped`() = runTest {
        var ran = false
        val selective = object : PipelineBehavior {
            override fun appliesTo(request: Request<*>) = false
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                ran = true; return next(request)
            }
        }
        val m = mediator(pipelineBehaviors = listOf(selective)) {
            handler(PingHandler())
        }
        m.send(PingQuery("x"))
        assertFalse(ran)
    }

    @Test
    fun `behavior with appliesTo=true runs`() = runTest {
        var ran = false
        val b = object : PipelineBehavior {
            override fun appliesTo(request: Request<*>) = true
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                ran = true; return next(request)
            }
        }
        val m = mediator(pipelineBehaviors = listOf(b)) { handler(PingHandler()) }
        m.send(PingQuery("x"))
        assertTrue(ran)
    }

    @Test
    fun `behavior with isEnabled=false is skipped`() = runTest {
        var ran = false
        val disabled = object : PipelineBehavior {
            override val isEnabled = false
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                ran = true; return next(request)
            }
        }
        val m = mediator(pipelineBehaviors = listOf(disabled)) { handler(PingHandler()) }
        m.send(PingQuery("x"))
        assertFalse(ran)
    }

    @Test
    fun `behavior can read and write request context`() = runTest {
        val b = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                requestContext.put("from-behavior", "injected")
                return next(request)
            }
        }
        var captured: String? = null
        val handler = RequestHandler<PingQuery, String> { mediator, requestContext, request ->
            captured = requestContext.getMetaData("from-behavior")
            "ok"
        }
        val m = mediator(pipelineBehaviors = listOf(b)) { handler(handler) }
        m.send(PingQuery("x"))
        assertEquals("injected", captured)
    }

    @Test
    fun `behavior can short-circuit without calling next`() = runTest {
        var handlerRan = false
        val shortCircuit = object : PipelineBehavior {
            @Suppress("UNCHECKED_CAST")
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult = "short-circuited" as TResult
        }
        val handler = RequestHandler<PingQuery, String> { mediator, requestContext, request ->
            handlerRan = true
            "handler"
        }
        val m = mediator(pipelineBehaviors = listOf(shortCircuit)) { handler(handler) }
        val result = m.send(PingQuery("x"))
        assertEquals("short-circuited", result)
        assertFalse(handlerRan)
    }

    @Test
    fun `behavior appliesTo can restrict to specific request type`() = runTest {
        var ranForPing = false
        val pingOnly = object : PipelineBehavior {
            override fun appliesTo(request: Request<*>) = request is PingQuery
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                ranForPing = true; return next(request)
            }
        }
        val m = mediator(pipelineBehaviors = listOf(pingOnly)) {
            handler(PingHandler())
            handler(AddHandler())
        }
        m.send(AddCommand(1, 2))
        assertFalse(ranForPing)
        m.send(PingQuery("x"))
        assertTrue(ranForPing)
    }

    @Test
    fun `result flows through behaviors unchanged when unmodified`() = runTest {
        val b = loggingBehavior(0, "b", mutableListOf())
        val m = mediator(pipelineBehaviors = listOf(b)) { handler(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    // ── DSL behavior() tests ─────────────────────────────────────────────────

    @Test
    fun `dsl behavior wraps the handler`() = runTest {
        val log = mutableListOf<String>()
        val b = behavior { _, next, request ->
            log += "before"
            next(request).also { log += "after" }
        }
        val m = mediator(pipelineBehaviors = listOf(b)) { handler(PingHandler()) }
        val result = m.send(PingQuery("x"))
        assertEquals("pong:x", result)
        assertEquals(listOf("before", "after"), log)
    }

    @Test
    fun `dsl behavior respects order`() = runTest {
        val log = mutableListOf<String>()
        val pre = behavior(order = -100) { _, next, request ->
            log += "pre"; next(request)
        }
        val post = behavior(order = 100) { _, next, request ->
            log += "post"; next(request)
        }
        val m = mediator(pipelineBehaviors = listOf(post, pre)) { handler(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("pre", "post"), log)
    }

    @Test
    fun `dsl behavior order controls nesting`() = runTest {
        val log = mutableListOf<String>()
        val outer = behavior(order = -10) { _, next, request ->
            log += "outer-before"; next(request).also { log += "outer-after" }
        }
        val inner = behavior(order = 10) { _, next, request ->
            log += "inner-before"; next(request).also { log += "inner-after" }
        }
        val m = mediator(pipelineBehaviors = listOf(inner, outer)) { handler(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("outer-before", "inner-before", "inner-after", "outer-after"), log)
    }

    @Test
    fun `dsl behavior appliesTo filters requests`() = runTest {
        var ran = false
        val pingOnly = behavior(appliesTo = { it is PingQuery }) { _, next, request ->
            ran = true; next(request)
        }
        val m = mediator(pipelineBehaviors = listOf(pingOnly)) {
            handler(PingHandler())
            handler(AddHandler())
        }
        m.send(AddCommand(1, 2))
        assertFalse(ran)
        m.send(PingQuery("x"))
        assertTrue(ran)
    }

    @Test
    fun `dsl behavior can short-circuit`() = runTest {
        var handlerRan = false
        val shortCircuit = behavior { _, _, _ -> "blocked" }
        val handler =
            RequestHandler<PingQuery, String> { mediator, requestContext, request -> handlerRan = true; "handler" }
        val m = mediator(pipelineBehaviors = listOf(shortCircuit)) { handler(handler) }
        assertEquals("blocked", m.send(PingQuery("x")))
        assertFalse(handlerRan)
    }

    @Test
    fun `dsl behavior can access request context`() = runTest {
        val b = behavior { requestContext, next, request ->
            requestContext.put("dsl-key", "dsl-value")
            next(request)
        }
        var captured: String? = null
        val handler = RequestHandler<PingQuery, String> { mediator, requestContext, request ->
            captured = requestContext.getMetaData("dsl-key")
            "ok"
        }
        val m = mediator(pipelineBehaviors = listOf(b)) { handler(handler) }
        m.send(PingQuery("x"))
        assertEquals("dsl-value", captured)
    }
}
