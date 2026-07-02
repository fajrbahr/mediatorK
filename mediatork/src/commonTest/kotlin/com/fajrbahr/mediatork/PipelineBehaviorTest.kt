package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

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
            register(PingHandler())
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
            register(PingHandler())
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
            register(PingHandler())
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
            register(PingHandler())
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
        val m = mediator(pipelineBehaviors = listOf(b)) { register(PingHandler()) }
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
        val m = mediator(pipelineBehaviors = listOf(disabled)) { register(PingHandler()) }
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
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                captured = requestContext.getMetaData("from-behavior")
                return "ok"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(b)) { register(handler) }
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
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                handlerRan = true
                return "handler"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(shortCircuit)) { register(handler) }
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
            register(PingHandler())
            register(AddHandler())
        }
        m.send(AddCommand(1, 2))
        assertFalse(ranForPing)
        m.send(PingQuery("x"))
        assertTrue(ranForPing)
    }

    @Test
    fun `result flows through behaviors unchanged when unmodified`() = runTest {
        val b = loggingBehavior(0, "b", mutableListOf())
        val m = mediator(pipelineBehaviors = listOf(b)) { register(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }
}
