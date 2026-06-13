package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PrePostProcessorTest {

    // ── RequestPreProcessor ────────────────────────────────────────────────────

    @Test
    fun `pre-processor runs before handler`() = runTest {
        val order = mutableListOf<String>()
        val pre = object : RequestPreProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                order += "pre"
            }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                order += "handler"
                return "ok"
            }
        }
        val m = mediator(preProcessors = listOf(pre)) { register(handler) }
        m.send(PingQuery("x"))
        assertEquals(listOf("pre", "handler"), order)
    }

    @Test
    fun `pre-processor can populate request context for handler`() = runTest {
        var captured: String? = null
        val pre = object : RequestPreProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                requestContext.put("token", "abc123")
            }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                captured = requestContext.getMetaDate("token")
                return "ok"
            }
        }
        val m = mediator(preProcessors = listOf(pre)) { register(handler) }
        m.send(PingQuery("x"))
        assertEquals("abc123", captured)
    }

    @Test
    fun `multiple pre-processors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = object : RequestPreProcessor {
            override val order = 1
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                order += "first"
            }
        }
        val second = object : RequestPreProcessor {
            override val order = 2
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                order += "second"
            }
        }
        val m = mediator(preProcessors = listOf(second, first)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `pre-processor throwing aborts pipeline`() = runTest {
        var handlerRan = false
        val pre = object : RequestPreProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                throw IllegalArgumentException("invalid")
            }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                handlerRan = true
                return "ok"
            }
        }
        val m = mediator(preProcessors = listOf(pre)) { register(handler) }
        assertFailsWith<IllegalArgumentException> { m.send(PingQuery("x")) }
        assertFalse(handlerRan)
    }

    // ── RequestPostProcessor ──────────────────────────────────────────────────

    @Test
    fun `post-processor runs after handler`() = runTest {
        val order = mutableListOf<String>()
        val post = object : RequestPostProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                order += "post"
            }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                order += "handler"
                return "ok"
            }
        }
        val m = mediator(postProcessors = listOf(post)) { register(handler) }
        m.send(PingQuery("x"))
        assertEquals(listOf("handler", "post"), order)
    }

    @Test
    fun `post-processor receives handler response`() = runTest {
        var captured: Any? = "not-set"
        val post = object : RequestPostProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                captured = response
            }
        }
        val m = mediator(postProcessors = listOf(post)) { register(PingHandler()) }
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }

    @Test
    fun `post-processor receives original request`() = runTest {
        var capturedRequest: Request<*>? = null
        val post = object : RequestPostProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                capturedRequest = request
            }
        }
        val m = mediator(postProcessors = listOf(post)) { register(PingHandler()) }
        m.send(PingQuery("hello"))
        assertEquals(PingQuery("hello"), capturedRequest)
    }

    @Test
    fun `multiple post-processors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = object : RequestPostProcessor {
            override val order = 1
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                order += "first"
            }
        }
        val second = object : RequestPostProcessor {
            override val order = 2
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                order += "second"
            }
        }
        val m = mediator(postProcessors = listOf(second, first)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `post-processor does not run when handler throws unhandled exception`() = runTest {
        var postRan = false
        val post = object : RequestPostProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                postRan = true
            }
        }
        val failingHandler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery,
            ): String = throw RuntimeException("boom")
        }
        val m = mediator(postProcessors = listOf(post)) { register(failingHandler) }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertFalse(postRan)
    }

    @Test
    fun `post-processor can read context values written by pre-processor`() = runTest {
        var postSawValue: String? = null
        val pre = object : RequestPreProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                requestContext.put("shared", "value")
            }
        }
        val post = object : RequestPostProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                postSawValue = requestContext.getMetaDate("shared")
            }
        }
        val m = mediator(preProcessors = listOf(pre), postProcessors = listOf(post)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals("value", postSawValue)
    }
}
