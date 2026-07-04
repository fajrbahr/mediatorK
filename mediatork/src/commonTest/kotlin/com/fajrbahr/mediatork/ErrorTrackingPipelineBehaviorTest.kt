@file:Suppress("TooGenericExceptionThrown")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.errorTrackingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ErrorTrackingPipelineBehaviorTest {

    @Test
    fun `callback not called on success`() = runTest {
        var captured: Throwable? = null
        val tracker = errorTrackingPipelineBehavior { req, e -> captured = e }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertNull(captured)
    }

    @Test
    fun `callback called with correct request and exception`() = runTest {
        var capturedRequest: Request<*>? = null
        var capturedError: Throwable? = null
        val tracker = errorTrackingPipelineBehavior { req, e -> capturedRequest = req; capturedError = e }
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
        val tracker = errorTrackingPipelineBehavior { _, _ -> /* no-op */ }
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String =
                error("rethrown")
        }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(handler) }
        assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `default order is Int_MAX_VALUE`() {
        assertEquals(Int.MAX_VALUE, errorTrackingPipelineBehavior { _, _ -> }.order)
    }

    @Test
    fun `result is returned unchanged on success`() = runTest {
        val tracker = errorTrackingPipelineBehavior { _, _ -> }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `callback not called for multiple successful requests`() = runTest {
        var callCount = 0
        val tracker = errorTrackingPipelineBehavior { _, _ -> callCount++ }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(PingHandler()) }
        repeat(3) { m.send(PingQuery("x")) }
        assertEquals(0, callCount)
    }

    @Test
    fun `callback receives the correct exception type`() = runTest {
        var captured: Throwable? = null
        val tracker = errorTrackingPipelineBehavior { _, e -> captured = e }
        val m = mediator(pipelineBehaviors = listOf(tracker)) {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: PingQuery
                ): String =
                    throw IllegalArgumentException("bad arg")
            })
        }
        runCatching { m.send(PingQuery("x")) }
        assertIs<IllegalArgumentException>(captured)
        assertEquals("bad arg", captured?.message)
    }

    @Test
    fun `only failing requests trigger callback when mixed with successful ones`() = runTest {
        val capturedRequests: MutableList<Request<*>> = mutableListOf()
        val tracker = errorTrackingPipelineBehavior { req, _ -> capturedRequests += req }
        val failOnB = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String =
                if (request.value == "b") throw RuntimeException("fail on b") else "ok"
        }
        val m = mediator(pipelineBehaviors = listOf(tracker)) { register(failOnB) }
        m.send(PingQuery("a"))
        runCatching { m.send(PingQuery("b")) }
        m.send(PingQuery("c"))
        assertEquals(listOf<Request<*>>(PingQuery("b")), capturedRequests)
    }

    @Test
    fun `callback receives AddCommand when AddHandler throws`() = runTest {
        var capturedRequest: Request<*>? = null
        val tracker = errorTrackingPipelineBehavior { req, _ -> capturedRequest = req }
        val m = mediator(pipelineBehaviors = listOf(tracker)) {
            register(object : RequestHandler<AddCommand, Int> {
                override suspend fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: AddCommand
                ): Int =
                    throw RuntimeException("add failed")
            })
        }
        runCatching { m.send(AddCommand(1, 2)) }
        assertEquals(AddCommand(1, 2), capturedRequest)
    }
}
