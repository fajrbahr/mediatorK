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
}
