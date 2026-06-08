package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SendTest {

    @Test
    fun `send returns handler result`() = runTest {
        val m = mediator { register(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `send routes to correct handler when multiple are registered`() = runTest {
        val m = mediator {
            register(PingHandler())
            register(AddHandler())
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `send with Request_Unit returns Unit and executes side effect`() = runTest {
        val handler = NoResultHandler()
        val m = mediator { register(handler) }
        m.send(NoResultCommand("id-1"))
        assertEquals("id-1", handler.lastId)
    }

    @Test
    fun `send with zero arguments returns correct sum`() = runTest {
        val m = mediator { register(AddHandler()) }
        assertEquals(0, m.send(AddCommand(0, 0)))
    }

    @Test
    fun `send with negative numbers returns correct result`() = runTest {
        val m = mediator { register(AddHandler()) }
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
        val m = mediator { register(AddHandler()) }
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
        val first = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) = "first"
        }
        val second = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) = "second"
        }
        val m = mediator {
            register(first)
            register(second)
        }
        assertEquals("second", m.send(PingQuery("x")))
    }

    @Test
    fun `handler receives mediator and can dispatch nested request`() = runTest {
        val inner = object : RequestHandler<EchoQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: EchoQuery) =
                request.text
        }
        val outer = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                "nested:" + mediator.send(EchoQuery(request.value))
        }
        val m = mediator {
            register(inner)
            register(outer)
        }
        assertEquals("nested:hello", m.send(PingQuery("hello")))
    }
}
