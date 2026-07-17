package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoggingPipelineBehaviorTest {

    @Test
    fun `logs request name on entry and exit with result`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(logging(logger = log::add))
        }
        m.send(PingQuery("hello"))
        assertEquals(listOf("→ PingQuery", "← PingQuery result=pong:hello"), log)
    }

    @Test
    fun `default order is -100`() {
        assertEquals(-100, logging().order)
    }

    @Test
    fun `logs only entry line when handler throws`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("boom") }
            behaviors(logging(logger = log::add))
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals(listOf("→ PingQuery"), log)
    }

    @Test
    fun `logs correct class name for AddCommand`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(logging(logger = log::add))
        }
        m.send(AddCommand(2, 3))
        assertEquals(listOf("→ AddCommand", "← AddCommand result=5"), log)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(42, logging(order = 42).order)
    }

    @Test
    fun `result is passed through unchanged`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(logging(logger = {}))
        }
        assertEquals("pong:world", m.send(PingQuery("world")))
    }

    @Test
    fun `multiple requests produce separate log pairs`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(logging(logger = log::add))
        }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(
            listOf(
                "→ PingQuery", "← PingQuery result=pong:a",
                "→ PingQuery", "← PingQuery result=pong:b",
            ),
            log,
        )
    }

    @Test
    fun `each log message is a separate logger call`() = runTest {
        var callCount = 0
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(logging(logger = { callCount++ }))
        }
        m.send(PingQuery("x"))
        assertEquals(2, callCount)
    }

    @Test
    fun `entry message contains arrow prefix`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(logging(logger = log::add))
        }
        m.send(PingQuery("x"))
        assertTrue(log.first().startsWith("→"))
    }

    @Test
    fun `exit message contains result value`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(logging(logger = log::add))
        }
        m.send(AddCommand(10, 20))
        assertTrue(log.last().contains("30"))
    }
}
