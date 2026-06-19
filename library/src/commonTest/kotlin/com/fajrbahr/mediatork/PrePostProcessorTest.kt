package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.PipelineBehavior.Tag
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PrePostProcessorTest {

    // ── Tag.Pre ───────────────────────────────────────────────────────────────

    @Test
    fun `PRE behavior runs before handler`() = runTest {
        val order = mutableListOf<String>()
        val pre = object : PipelineBehavior {
            override val tag = Tag.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { order += "pre"; return next(request) }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                order += "handler"; return "ok"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(pre)) { register(handler) }
        m.send(PingQuery("x"))
        assertEquals(listOf("pre", "handler"), order)
    }

    @Test
    fun `PRE behavior can populate request context for handler`() = runTest {
        var captured: String? = null
        val pre = object : PipelineBehavior {
            override val tag = Tag.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { requestContext.put("token", "abc123"); return next(request) }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                captured = requestContext.getMetaDate("token"); return "ok"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(pre)) { register(handler) }
        m.send(PingQuery("x"))
        assertEquals("abc123", captured)
    }

    @Test
    fun `multiple PRE behaviors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = object : PipelineBehavior {
            override val tag = Tag.Pre
            override val order = 1
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { order += "first"; return next(request) }
        }
        val second = object : PipelineBehavior {
            override val tag = Tag.Pre
            override val order = 2
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { order += "second"; return next(request) }
        }
        val m = mediator(pipelineBehaviors = listOf(second, first)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `PRE behavior throwing aborts pipeline`() = runTest {
        var handlerRan = false
        val pre = object : PipelineBehavior {
            override val tag = Tag.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult = throw IllegalArgumentException("invalid")
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                handlerRan = true; return "ok"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(pre)) { register(handler) }
        assertFailsWith<IllegalArgumentException> { m.send(PingQuery("x")) }
        assertFalse(handlerRan)
    }

    // ── Tag.Post ──────────────────────────────────────────────────────────────

    @Test
    fun `POST behavior runs after handler`() = runTest {
        val order = mutableListOf<String>()
        val post = object : PipelineBehavior {
            override val tag = Tag.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); order += "post"; return r }
        }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                order += "handler"; return "ok"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(post)) { register(handler) }
        m.send(PingQuery("x"))
        assertEquals(listOf("handler", "post"), order)
    }

    @Test
    fun `POST behavior receives handler response`() = runTest {
        var captured: Any? = "not-set"
        val post = object : PipelineBehavior {
            override val tag = Tag.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); captured = r; return r }
        }
        val m = mediator(pipelineBehaviors = listOf(post)) { register(PingHandler()) }
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }

    @Test
    fun `POST behavior receives original request`() = runTest {
        var capturedRequest: Request<*>? = null
        val post = object : PipelineBehavior {
            override val tag = Tag.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); capturedRequest = request; return r }
        }
        val m = mediator(pipelineBehaviors = listOf(post)) { register(PingHandler()) }
        m.send(PingQuery("hello"))
        assertEquals(PingQuery("hello"), capturedRequest)
    }

    @Test
    fun `multiple POST behaviors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = object : PipelineBehavior {
            override val tag = Tag.Post
            override val order = 1
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); order += "first"; return r }
        }
        val second = object : PipelineBehavior {
            override val tag = Tag.Post
            override val order = 2
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); order += "second"; return r }
        }
        val m = mediator(pipelineBehaviors = listOf(second, first)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `POST behavior does not run when handler throws unhandled exception`() = runTest {
        var postRan = false
        val post = object : PipelineBehavior {
            override val tag = Tag.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); postRan = true; return r }
        }
        val failingHandler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                throw RuntimeException("boom")
        }
        val m = mediator(pipelineBehaviors = listOf(post)) { register(failingHandler) }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertFalse(postRan)
    }

    @Test
    fun `POST behavior can read context values written by PRE behavior`() = runTest {
        var postSawValue: String? = null
        val pre = object : PipelineBehavior {
            override val tag = Tag.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { requestContext.put("shared", "value"); return next(request) }
        }
        val post = object : PipelineBehavior {
            override val tag = Tag.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); postSawValue = requestContext.getMetaDate("shared"); return r }
        }
        val m = mediator(pipelineBehaviors = listOf(pre, post)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals("value", postSawValue)
    }
}
