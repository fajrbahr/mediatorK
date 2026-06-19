package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.handler.otherwise
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FallbackRequestHandlerTest {

    private class FailingHandler(private val message: String) : RequestHandler<PingQuery, String> {
        override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
            throw RuntimeException(message)
    }

    private class SucceedingHandler(private val response: String) : RequestHandler<PingQuery, String> {
        var callCount = 0
        override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
            callCount++
            return response
        }
    }

    @Test
    fun `uses primary handler when it succeeds`() = runTest {
        val m =
            mediator { register<PingQuery, String>(SucceedingHandler("primary") otherwise SucceedingHandler("fallback")) }
        assertEquals("primary", m.send(PingQuery("x")))
    }

    @Test
    fun `falls back to second handler when first throws`() = runTest {
        val m = mediator { register<PingQuery, String>(FailingHandler("oops") otherwise SucceedingHandler("fallback")) }
        assertEquals("fallback", m.send(PingQuery("x")))
    }

    @Test
    fun `chains three handlers and uses first successful one`() = runTest {
        val third = SucceedingHandler("third")
        val m = mediator {
            register<PingQuery, String>(FailingHandler("1") otherwise FailingHandler("2") otherwise third)
        }
        assertEquals("third", m.send(PingQuery("x")))
        assertEquals(1, third.callCount)
    }

    @Test
    fun `rethrows last exception when all handlers fail`() = runTest {
        val m = mediator {
            register<PingQuery, String>(FailingHandler("first") otherwise FailingHandler("last"))
        }
        val ex = assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals("last", ex.message)
    }

    @Test
    fun `does not call subsequent handlers after first success`() = runTest {
        val second = SucceedingHandler("second")
        val m = mediator { register<PingQuery, String>(SucceedingHandler("primary") otherwise second) }
        m.send(PingQuery("x"))
        assertEquals(0, second.callCount)
    }

    @Test
    fun `plus DSL syntax works with otherwise chain`() = runTest {
        val m = mediator { +(FailingHandler("oops") otherwise SucceedingHandler("ok")) }
        assertEquals("ok", m.send(PingQuery("x")))
    }
}
