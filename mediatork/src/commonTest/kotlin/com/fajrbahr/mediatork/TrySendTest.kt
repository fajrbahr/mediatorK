package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.handler.trySend
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TrySendTest {

    @Test
    fun `trySend returns success wrapping handler result`() = runTest {
        val m = mediator { register(PingHandler()) }
        val result = m.trySend(PingQuery("hello"))
        assertTrue(result.isSuccess)
        assertEquals("pong:hello", result.getOrNull())
    }

    @Test
    fun `trySend returns failure wrapping handler exception`() = runTest {
        val m = mediator {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                    throw IllegalStateException("boom")
            })
        }
        val result = m.trySend(PingQuery("x"))
        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("boom", result.exceptionOrNull()!!.message)
    }

    @Test
    fun `trySend returns failure when no handler registered`() = runTest {
        val m = mediator { }
        val result = m.trySend(PingQuery("x"))
        assertTrue(result.isFailure)
        assertIs<MissingHandlerException>(result.exceptionOrNull())
    }

    @Test
    fun `trySend does not throw - exception is captured in result`() = runTest {
        val m = mediator {
            register(object : RequestHandler<AddCommand, Int> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: AddCommand): Int =
                    throw RuntimeException("always fails")
            })
        }
        val result = runCatching { m.trySend(AddCommand(1, 2)) }
        assertTrue(result.isSuccess, "trySend itself must not throw")
        assertTrue(result.getOrThrow().isFailure)
    }

    @Test
    fun `trySend routes to correct handler among multiple`() = runTest {
        val m = mediator {
            register(PingHandler())
            register(AddHandler())
        }
        val pingResult = m.trySend(PingQuery("x"))
        val addResult = m.trySend(AddCommand(2, 3))
        assertTrue(pingResult.isSuccess)
        assertEquals("pong:x", pingResult.getOrNull())
        assertTrue(addResult.isSuccess)
        assertEquals(5, addResult.getOrNull())
    }

    @Test
    fun `trySend with Unit handler returns successful Unit result`() = runTest {
        val handler = NoResultHandler()
        val m = mediator { register(handler) }
        val result = m.trySend(NoResultCommand("id-99"))
        assertTrue(result.isSuccess)
        assertEquals("id-99", handler.lastId)
    }

    @Test
    fun `trySend failure contains original exception message`() = runTest {
        val m = mediator {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                    throw IllegalArgumentException("bad input")
            })
        }
        val result = m.trySend(PingQuery("x"))
        assertTrue(result.isFailure)
        assertEquals("bad input", result.exceptionOrNull()?.message)
    }

    @Test
    fun `trySend on same handler multiple times returns independent results`() = runTest {
        val m = mediator { register(AddHandler()) }
        val r1 = m.trySend(AddCommand(1, 1))
        val r2 = m.trySend(AddCommand(10, 10))
        assertEquals(2, r1.getOrNull())
        assertEquals(20, r2.getOrNull())
    }
}
