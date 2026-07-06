@file:Suppress("TooGenericExceptionThrown")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PrePostProcessorTest {

    // ── Pre-handler behavior (negative order) ───────────────────────────────────

    @Test
    fun `PRE behavior runs before handler`() = runTest {
        val order = mutableListOf<String>()
        val pre = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                order += "pre"; return next(request)
            }
        }
        val handler =
            RequestHandler<PingQuery, String> { mediator, requestContext, request -> order += "handler"; "ok" }
        val m = mediator(pipelineBehaviors = listOf(pre)) { add(handler) }
        m.send(PingQuery("x"))
        assertEquals(listOf("pre", "handler"), order)
    }

    @Test
    fun `PRE behavior can populate request context for handler`() = runTest {
        var captured: String? = null
        val pre = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                requestContext.put("token", "abc123"); return next(request)
            }
        }
        val handler = RequestHandler<PingQuery, String> { mediator, requestContext, request ->
            captured = requestContext.getMetaData("token"); "ok"
        }
        val m = mediator(pipelineBehaviors = listOf(pre)) { add(handler) }
        m.send(PingQuery("x"))
        assertEquals("abc123", captured)
    }

    @Test
    fun `multiple behaviors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                order += "first"; return next(request)
            }
        }
        val second = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                order += "second"; return next(request)
            }
        }
        val m = mediator(pipelineBehaviors = listOf(second, first)) { add(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `PRE behavior throwing aborts pipeline`() = runTest {
        var handlerRan = false
        val pre = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult = throw IllegalArgumentException("invalid")
        }
        val handler = RequestHandler<PingQuery, String> { mediator, requestContext, request -> handlerRan = true; "ok" }
        val m = mediator(pipelineBehaviors = listOf(pre)) { add(handler) }
        assertFailsWith<IllegalArgumentException> { m.send(PingQuery("x")) }
        assertFalse(handlerRan)
    }

    // ── Post-handler behavior (calls next first, then acts) ─────────────────────

    @Test
    fun `POST behavior runs after handler`() = runTest {
        val order = mutableListOf<String>()
        val post = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); order += "post"; return r
            }
        }
        val handler =
            RequestHandler<PingQuery, String> { mediator, requestContext, request -> order += "handler"; "ok" }
        val m = mediator(pipelineBehaviors = listOf(post)) { add(handler) }
        m.send(PingQuery("x"))
        assertEquals(listOf("handler", "post"), order)
    }

    @Test
    fun `POST behavior receives handler response`() = runTest {
        var captured: Any? = "not-set"
        val post = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); captured = r; return r
            }
        }
        val m = mediator(pipelineBehaviors = listOf(post)) { add(PingHandler()) }
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }

    @Test
    fun `POST behavior receives original request`() = runTest {
        var capturedRequest: Request<*>? = null
        val post = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); capturedRequest = request; return r
            }
        }
        val m = mediator(pipelineBehaviors = listOf(post)) { add(PingHandler()) }
        m.send(PingQuery("hello"))
        assertEquals(PingQuery("hello"), capturedRequest)
    }

    @Test
    fun `multiple POST behaviors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); order += "first"; return r
            }
        }
        val second = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); order += "second"; return r
            }
        }
        val m = mediator(pipelineBehaviors = listOf(second, first)) { add(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `POST behavior does not run when handler throws unhandled exception`() = runTest {
        var postRan = false
        val post = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); postRan = true; return r
            }
        }
        val failingHandler =
            RequestHandler<PingQuery, String> { mediator, requestContext, request -> throw RuntimeException("boom") }
        val m = mediator(pipelineBehaviors = listOf(post)) { add(failingHandler) }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertFalse(postRan)
    }

    @Test
    fun `POST behavior can read context values written by PRE behavior`() = runTest {
        var postSawValue: String? = null
        val pre = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                requestContext.put("shared", "value"); return next(request)
            }
        }
        val post = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                val r = next(request); postSawValue = requestContext.getMetaData("shared"); return r
            }
        }
        val m = mediator(pipelineBehaviors = listOf(pre, post)) { add(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals("value", postSawValue)
    }
}
