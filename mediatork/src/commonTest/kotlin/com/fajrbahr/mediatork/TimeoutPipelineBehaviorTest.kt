package com.fajrbahr.mediatork

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeoutPipelineBehaviorTest {

    @Test
    fun `completes normally when handler finishes within timeout`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(timeout(millis = 5_000))
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `throws TimeoutCancellationException when handler exceeds timeout`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> {
                delay(10_000)
                "too late"
            }
            behaviors(timeout(millis = 100))
        }
        assertFailsWith<TimeoutCancellationException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `requires millis greater than zero`() {
        assertFailsWith<IllegalArgumentException> {
            timeout(millis = 0)
        }
    }

    @Test
    fun `negative millis throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            timeout(millis = -1)
        }
    }

    @Test
    fun `default order is 0`() {
        assertEquals(0, timeout(millis = 1_000).order)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(-50, timeout(millis = 1_000, order = -50).order)
    }

    @Test
    fun `result is passed through unchanged when handler completes in time`() = runTest {
        val m = mediatorK {
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(timeout(millis = 5_000))
        }
        assertEquals(9, m.send(AddCommand(4, 5)))
    }

    @Test
    fun `result passes through for AddCommand within timeout`() = runTest {
        val m = mediatorK {
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(timeout(millis = 5_000))
        }
        assertEquals(15, m.send(AddCommand(7, 8)))
    }

    @Test
    fun `multiple requests all complete when within timeout`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(timeout(millis = 5_000))
        }
        repeat(5) { i ->
            assertEquals("pong:$i", m.send(PingQuery("$i")))
        }
    }
}
