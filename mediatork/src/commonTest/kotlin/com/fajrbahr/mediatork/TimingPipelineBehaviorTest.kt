package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TimingPipelineBehaviorTest {

    @Test
    fun `callback receives request name and non-negative duration`() = runTest {
        var capturedName: String? = null
        var capturedMs: Long? = null
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(timing(onTiming = { name, ms -> capturedName = name; capturedMs = ms }))
        }
        m.send(PingQuery("x"))
        assertEquals("PingQuery", capturedName)
        assertNotNull(capturedMs)
        assertTrue(capturedMs!! >= 0)
    }

    @Test
    fun `callback is called even when handler throws`() = runTest {
        var called = false
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("boom") }
            behaviors(timing(onTiming = { _, _ -> called = true }))
        }
        try { m.send(PingQuery("x")) } catch (_: RuntimeException) {}
        assertTrue(called)
    }

    @Test
    fun `callback is called for every request`() = runTest {
        var count = 0
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(timing(onTiming = { _, _ -> count++ }))
        }
        repeat(3) { m.send(PingQuery("x")) }
        assertEquals(3, count)
    }

    @Test
    fun `default order is 0`() {
        assertEquals(0, timing(onTiming = { _, _ -> }).order)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(-10, timing(onTiming = { _, _ -> }, order = -10).order)
    }

    @Test
    fun `callback receives AddCommand class name`() = runTest {
        var capturedName: String? = null
        val m = mediatorK {
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(timing(onTiming = { name, _ -> capturedName = name }))
        }
        m.send(AddCommand(1, 2))
        assertEquals("AddCommand", capturedName)
    }

    @Test
    fun `exception is rethrown after timing callback fires`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { throw IllegalStateException("domain error") }
            behaviors(timing(onTiming = { _, _ -> }))
        }
        val ex = assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
        assertEquals("domain error", ex.message)
    }

    @Test
    fun `result passes through unchanged`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(timing(onTiming = { _, _ -> }))
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `two different request types each receive their own class name`() = runTest {
        val names = mutableListOf<String>()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(timing(onTiming = { name, _ -> names += name }))
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        assertEquals(listOf("PingQuery", "AddCommand"), names)
    }
}
