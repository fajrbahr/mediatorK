package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ── Tests ─────────────────────────────────────────────────────────────────────

class MediatorTest {

    @Test
    fun `send returns handler result`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { request -> "pong:${request.value}" }
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `send routes to correct handler among many`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { request -> "pong:${request.value}" }
            handle<AddCommand, Int> { request -> request.a + request.b }
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `send with Request_Unit returns Unit`() = runTest {
        var lastId: String? = null
        val m = mediatorK {
            handle<NoResultCommand, Unit> { request -> lastId = request.id }
        }
        m.send(NoResultCommand("id-1"))
        assertEquals("id-1", lastId)
    }

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingHandlerException> {
            m.send(PingQuery("x"))
        }
    }

    @Test
    fun `MissingHandlerException message includes request type name`() = runTest {
        val m = mediatorK { }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("PingQuery"))
    }

    @Test
    fun `publish delivers notification to all registered handlers`() = runTest {
        val received1 = mutableListOf<String>()
        val received2 = mutableListOf<String>()
        val m = mediatorK {
            on<PingNotification> { received1 += it.message }
            on<PingNotification> { received2 += it.message }
        }
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), received1)
        assertEquals(listOf("hello"), received2)
    }

    @Test
    fun `publish with no handlers throws MissingNotificationHandlerException`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("silent"))
        }
    }

    @Test
    fun `pipeline behavior wraps handler in order`() = runTest {
        val log = mutableListOf<String>()

        val outer = object : PipelineBehavior {
            override val order = -10
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "outer-before"
                return next(request).also { log += "outer-after" }
            }
        }

        val inner = object : PipelineBehavior {
            override val order = 10
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "inner-before"
                return next(request).also { log += "inner-after" }
            }
        }

        val m = mediatorK {
            handle<PingQuery, String> { request -> "pong:${request.value}" }
            behaviors(inner, outer)
        }

        m.send(PingQuery("x"))
        assertEquals(listOf("outer-before", "inner-before", "inner-after", "outer-after"), log)
    }

    @Test
    fun `pipeline behavior with appliesTo=false is skipped`() = runTest {
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
        val m = mediatorK {
            handle<PingQuery, String> { request -> "pong:${request.value}" }
            behaviors(selective)
        }
        m.send(PingQuery("x"))
        assertFalse(ran)
    }

    @Test
    fun `PRE behavior runs before handler and can populate context`() = runTest {
        var contextValue: String? = null

        val pre = object : PipelineBehavior {
            override val stage = Stage.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                requestContext.put("key", "injected"); return next(request)
            }
        }

        val m = mediatorK {
            handle<PingQuery, String> { request ->
                contextValue = context.getMetaData("key"); "ok"
            }
            behaviors(pre)
        }
        m.send(PingQuery("x"))
        assertEquals("injected", contextValue)
    }

    @Test
    fun `POST behavior runs after handler and receives response`() = runTest {
        var captured: Any? = "not-set"

        val post = object : PipelineBehavior {
            override val stage = Stage.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); captured = r; return r
            }
        }

        val m = mediatorK {
            handle<PingQuery, String> { request -> "pong:${request.value}" }
            behaviors(post)
        }
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }

}
