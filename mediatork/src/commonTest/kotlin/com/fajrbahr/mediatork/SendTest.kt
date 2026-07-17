package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SendTest {

    @Test
    fun `send returns handler result`() = runTest {
        val m = mediatorK { handle<PingQuery, String> { "pong:${it.value}" } }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `send routes to correct handler when multiple are registered`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `send with Request_Unit returns Unit and executes side effect`() = runTest {
        var lastId: String? = null
        val m = mediatorK { handle<NoResultCommand, Unit> { lastId = it.id } }
        m.send(NoResultCommand("id-1"))
        assertEquals("id-1", lastId)
    }

    @Test
    fun `send with zero arguments returns correct sum`() = runTest {
        val m = mediatorK { handle<AddCommand, Int> { it.a + it.b } }
        assertEquals(0, m.send(AddCommand(0, 0)))
    }

    @Test
    fun `send with negative numbers returns correct result`() = runTest {
        val m = mediatorK { handle<AddCommand, Int> { it.a + it.b } }
        assertEquals(-3, m.send(AddCommand(-1, -2)))
    }

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingHandlerException> {
            m.send(PingQuery("x"))
        }
    }

    @Test
    fun `MissingHandlerException message contains request type name`() = runTest {
        val m = mediatorK { }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("PingQuery"), "expected message to mention 'PingQuery', got: ${ex.message}")
    }

    @Test
    fun `MissingHandlerException lists registered types when available`() = runTest {
        val m = mediatorK { handle<AddCommand, Int> { it.a + it.b } }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("AddCommand"), "expected registered types in message, got: ${ex.message}")
    }

    @Test
    fun `MissingHandlerException is a MediatorException subtype`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MediatorException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `handler registered last wins when registered twice`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "first" }
            handle<PingQuery, String> { "second" }
        }
        assertEquals("second", m.send(PingQuery("x")))
    }

    @Test
    fun `handler receives mediator and can dispatch nested request`() = runTest {
        val m = mediatorK {
            handle<EchoQuery, String> { it.text }
            handle<PingQuery, String> { "nested:" + send(EchoQuery(it.value)) }
        }
        assertEquals("nested:hello", m.send(PingQuery("hello")))
    }
}
