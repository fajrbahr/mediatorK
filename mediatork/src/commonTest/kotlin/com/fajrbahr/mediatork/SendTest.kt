package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.RequestHandler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SendTest {

    @Test
    fun `send returns handler result`() = runTest {
        val m = mediator { handler(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `send routes to correct handler when multiple are registered`() = runTest {
        val m = mediator {
            handler(PingHandler())
            handler(AddHandler())
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `send with Request_Unit returns Unit and executes side effect`() = runTest {
        val handler = NoResultHandler()
        val m = mediator { handler(handler) }
        m.send(NoResultCommand("id-1"))
        assertEquals("id-1", handler.lastId)
    }

    @Test
    fun `send with zero arguments returns correct sum`() = runTest {
        val m = mediator { handler(AddHandler()) }
        assertEquals(0, m.send(AddCommand(0, 0)))
    }

    @Test
    fun `send with negative numbers returns correct result`() = runTest {
        val m = mediator { handler(AddHandler()) }
        assertEquals(-3, m.send(AddCommand(-1, -2)))
    }

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val m = mediator { }
        assertFailsWith<MissingHandlerException> {
            m.send(PingQuery("x"))
        }
    }

    @Test
    fun `MissingHandlerException message contains request type name`() = runTest {
        val m = mediator { }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("PingQuery"), "expected message to mention 'PingQuery', got: ${ex.message}")
    }

    @Test
    fun `MissingHandlerException lists registered types when available`() = runTest {
        val m = mediator { handler(AddHandler()) }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("AddCommand"), "expected registered types in message, got: ${ex.message}")
    }

    @Test
    fun `MissingHandlerException is a MediatorException subtype`() = runTest {
        val m = mediator { }
        assertFailsWith<MediatorException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `handler registered last wins when registered twice`() = runTest {
        val first = RequestHandler<PingQuery, String> { mediator, requestContext, request -> "first" }
        val second = RequestHandler<PingQuery, String> { mediator, requestContext, request -> "second" }
        val m = mediator {
            handler(first)
            handler(second)
        }
        assertEquals("second", m.send(PingQuery("x")))
    }

    @Test
    fun `handler receives mediator and can dispatch nested request`() = runTest {
        val inner = RequestHandler<EchoQuery, String> { mediator, requestContext, request -> request.text }
        val outer = RequestHandler<PingQuery, String> { mediator, requestContext, request ->
            "nested:" + mediator.send(
                EchoQuery(request.value)
            )
        }
        val m = mediator {
            handler(inner)
            handler(outer)
        }
        assertEquals("nested:hello", m.send(PingQuery("hello")))
    }
}
