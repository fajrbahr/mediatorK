package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestCounterPipelineBehaviorTest {

    @Test
    fun `counts a single request type`() = runTest {
        val counter = requestCounter()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(counter)
        }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(2L, counter.countFor(PingQuery::class))
    }

    @Test
    fun `counts multiple request types independently`() = runTest {
        val counter = requestCounter()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(counter)
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        m.send(AddCommand(3, 4))
        assertEquals(1L, counter.countFor(PingQuery::class))
        assertEquals(2L, counter.countFor(AddCommand::class))
    }

    @Test
    fun `snapshot returns all counts`() = runTest {
        val counter = requestCounter()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(counter)
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        val snap = counter.snapshot()
        assertEquals(1L, snap["PingQuery"])
        assertEquals(1L, snap["AddCommand"])
    }

    @Test
    fun `reset clears all counts`() = runTest {
        val counter = requestCounter()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(counter)
        }
        m.send(PingQuery("x"))
        counter.reset()
        assertEquals(0L, counter.countFor(PingQuery::class))
        assertEquals(emptyMap(), counter.snapshot())
    }

    @Test
    fun `returns zero for unseen request type`() = runTest {
        val counter = requestCounter()
        assertEquals(0L, counter.countFor(PingQuery::class))
    }

    @Test
    fun `snapshot is empty initially`() = runTest {
        val counter = requestCounter()
        assertEquals(emptyMap(), counter.snapshot())
    }

    @Test
    fun `count increments even when handler throws`() = runTest {
        val counter = requestCounter()
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("always fails") }
            behaviors(counter)
        }
        runCatching { m.send(PingQuery("x")) }
        assertEquals(1L, counter.countFor(PingQuery::class))
    }

    @Test
    fun `default order is 0`() {
        assertEquals(0, requestCounter().order)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(5, requestCounter(order = 5).order)
    }

    @Test
    fun `reset leaves snapshot empty`() = runTest {
        val counter = requestCounter()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(counter)
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        counter.reset()
        assertEquals(emptyMap(), counter.snapshot())
    }
}
