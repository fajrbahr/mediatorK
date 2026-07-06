@file:Suppress("TooGenericExceptionThrown")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.handler.orElse
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
            mediator {
                add<PingQuery, String>(
                    SucceedingHandler("primary") orElse SucceedingHandler("fallback"),
                )
            }
        assertEquals("primary", m.send(PingQuery("x")))
    }

    @Test
    fun `falls back to second handler when first throws`() = runTest {
        val m = mediator { add<PingQuery, String>(FailingHandler("oops") orElse SucceedingHandler("fallback")) }
        assertEquals("fallback", m.send(PingQuery("x")))
    }

    @Test
    fun `chains three handlers and uses first successful one`() = runTest {
        val third = SucceedingHandler("third")
        val m = mediator {
            add<PingQuery, String>(FailingHandler("1") orElse FailingHandler("2") orElse third)
        }
        assertEquals("third", m.send(PingQuery("x")))
        assertEquals(1, third.callCount)
    }

    @Test
    fun `rethrows last exception when all handlers fail`() = runTest {
        val m = mediator {
            add<PingQuery, String>(FailingHandler("first") orElse FailingHandler("last"))
        }
        val ex = assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals("last", ex.message)
    }

    @Test
    fun `does not call subsequent handlers after first success`() = runTest {
        val second = SucceedingHandler("second")
        val m = mediator { add<PingQuery, String>(SucceedingHandler("primary") orElse second) }
        m.send(PingQuery("x"))
        assertEquals(0, second.callCount)
    }

    @Test
    fun `plus DSL syntax works with otherwise chain`() = runTest {
        val m = mediator { +(FailingHandler("oops") orElse SucceedingHandler("ok")) }
        assertEquals("ok", m.send(PingQuery("x")))
    }
}
