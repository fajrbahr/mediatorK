package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.ErrorTrackingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ErrorTrackingPipelineBehaviorTest {

    @Test
    fun `callback not called on success`() = runTest {
        var captured: Throwable? = null
        val tracker = ErrorTrackingPipelineBehavior { req, e -> captured = e }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertNull(captured)
    }

    @Test
    fun `callback called with correct request and exception`() = runTest {
        var capturedRequest: Request<*>? = null
        var capturedError: Throwable? = null
        val tracker = ErrorTrackingPipelineBehavior { req, e -> capturedRequest = req; capturedError = e }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String =
                throw RuntimeException("bad")
        }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(handler) }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals(PingQuery("x"), capturedRequest)
        assertEquals("bad", capturedError?.message)
    }

    @Test
    fun `exception is rethrown after callback`() = runTest {
        val tracker = ErrorTrackingPipelineBehavior { _, _ -> /* no-op */ }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String =
                throw IllegalStateException("rethrown")
        }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(handler) }
        assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
    }
}
