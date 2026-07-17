package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Request
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ErrorTrackingPipelineBehaviorTest {

    @Test
    fun `callback not called on success`() = runTest {
        var captured: Throwable? = null
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(errorTracking(onError = { _, e -> captured = e }))
        }
        m.send(PingQuery("x"))
        assertNull(captured)
    }

    @Test
    fun `callback called with correct request and exception`() = runTest {
        var capturedRequest: Request<*>? = null
        var capturedError: Throwable? = null
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("bad") }
            behaviors(errorTracking(onError = { req, e -> capturedRequest = req; capturedError = e }))
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals(PingQuery("x"), capturedRequest)
        assertEquals("bad", capturedError?.message)
    }

    @Test
    fun `exception is rethrown after callback`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { throw IllegalStateException("rethrown") }
            behaviors(errorTracking(onError = { _, _ -> }))
        }
        assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `default order is Int_MAX_VALUE`() {
        assertEquals(Int.MAX_VALUE, errorTracking(onError = { _, _ -> }).order)
    }

    @Test
    fun `result is returned unchanged on success`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(errorTracking(onError = { _, _ -> }))
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `callback not called for multiple successful requests`() = runTest {
        var callCount = 0
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(errorTracking(onError = { _, _ -> callCount++ }))
        }
        repeat(3) { m.send(PingQuery("x")) }
        assertEquals(0, callCount)
    }

    @Test
    fun `callback receives the correct exception type`() = runTest {
        var captured: Throwable? = null
        val m = mediatorK {
            handle<PingQuery, String> { throw IllegalArgumentException("bad arg") }
            behaviors(errorTracking(onError = { _, e -> captured = e }))
        }
        runCatching { m.send(PingQuery("x")) }
        assertIs<IllegalArgumentException>(captured)
        assertEquals("bad arg", captured?.message)
    }

    @Test
    fun `only failing requests trigger callback when mixed with successful ones`() = runTest {
        val capturedRequests: MutableList<Request<*>> = mutableListOf()
        val m = mediatorK {
            handle<PingQuery, String> {
                if (it.value == "b") throw RuntimeException("fail on b") else "ok"
            }
            behaviors(errorTracking(onError = { req, _ -> capturedRequests += req }))
        }
        m.send(PingQuery("a"))
        runCatching { m.send(PingQuery("b")) }
        m.send(PingQuery("c"))
        assertEquals(listOf<Request<*>>(PingQuery("b")), capturedRequests)
    }

    @Test
    fun `callback receives AddCommand when AddHandler throws`() = runTest {
        var capturedRequest: Request<*>? = null
        val m = mediatorK {
            handle<AddCommand, Int> { throw RuntimeException("add failed") }
            behaviors(errorTracking(onError = { req, _ -> capturedRequest = req }))
        }
        runCatching { m.send(AddCommand(1, 2)) }
        assertEquals(AddCommand(1, 2), capturedRequest)
    }
}
