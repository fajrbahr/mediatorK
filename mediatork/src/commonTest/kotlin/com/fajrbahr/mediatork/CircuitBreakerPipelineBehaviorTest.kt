package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CircuitBreakerPipelineBehaviorTest {

    @Test
    fun `passes requests when circuit is closed`() = runTest {
        val cb = circuitBreaker(failureThreshold = 3, resetTimeoutMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(cb)
        }
        assertEquals("pong:x", m.send(PingQuery("x")))
        assertEquals(CircuitState.CLOSED, cb.currentState)
    }

    @Test
    fun `opens after failureThreshold consecutive failures`() = runTest {
        val cb = circuitBreaker(failureThreshold = 3, resetTimeoutMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("downstream failure") }
            behaviors(cb)
        }
        repeat(3) {
            try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        }
        assertEquals(CircuitState.OPEN, cb.currentState)
    }

    @Test
    fun `throws CircuitOpenException when open`() = runTest {
        val cb = circuitBreaker(failureThreshold = 1, resetTimeoutMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("downstream failure") }
            behaviors(cb)
        }
        try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        assertFailsWith<CircuitOpenException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `invokes onStateChange callback on transitions`() = runTest {
        val states = mutableListOf<CircuitState>()
        val cb = circuitBreaker(
            failureThreshold = 1,
            resetTimeoutMs = 60_000,
            onStateChange = { states += it }
        )
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("downstream failure") }
            behaviors(cb)
        }
        try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        assertEquals(listOf(CircuitState.OPEN), states)
    }

    @Test
    fun `reset closes the circuit and clears failure count`() = runTest {
        val cb = circuitBreaker(failureThreshold = 1, resetTimeoutMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("downstream failure") }
            behaviors(cb)
        }
        try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        assertEquals(CircuitState.OPEN, cb.currentState)
        cb.reset()
        assertEquals(CircuitState.CLOSED, cb.currentState)
    }

    @Test
    fun `success resets failure count`() = runTest {
        val cb = circuitBreaker(failureThreshold = 3, resetTimeoutMs = 60_000)
        var shouldFail = true
        val m = mediatorK {
            handle<PingQuery, String> {
                if (shouldFail) throw RuntimeException("fail")
                "ok"
            }
            behaviors(cb)
        }
        // 2 failures — not enough to open
        repeat(2) {
            try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        }
        assertEquals(CircuitState.CLOSED, cb.currentState)
        // 1 success — resets counter
        shouldFail = false
        m.send(PingQuery("x"))
        // 2 more failures — counter restarted so circuit stays closed
        shouldFail = true
        repeat(2) {
            try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        }
        assertEquals(CircuitState.CLOSED, cb.currentState)
    }
}
