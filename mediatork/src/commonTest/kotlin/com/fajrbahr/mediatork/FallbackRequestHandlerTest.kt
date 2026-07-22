package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
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
        val registrar = object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.register(SucceedingHandler("primary") otherwise SucceedingHandler("fallback"))
            }
        }
        val m = mediator(registrar = registrar)
        assertEquals("primary", m.send(PingQuery("x")))
    }

    @Test
    fun `falls back to second handler when first throws`() = runTest {
        val registrar = object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.register(FailingHandler("oops") otherwise SucceedingHandler("fallback"))
            }
        }
        val m = mediator(registrar = registrar)
        assertEquals("fallback", m.send(PingQuery("x")))
    }

    @Test
    fun `chains three handlers and uses first successful one`() = runTest {
        val third = SucceedingHandler("third")
        val registrar = object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.register(FailingHandler("1") otherwise FailingHandler("2") otherwise third)
            }
        }
        val m = mediator(registrar = registrar)
        assertEquals("third", m.send(PingQuery("x")))
        assertEquals(1, third.callCount)
    }

    @Test
    fun `rethrows last exception when all handlers fail`() = runTest {
        val registrar = object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.register(FailingHandler("first") otherwise FailingHandler("last"))
            }
        }
        val m = mediator(registrar = registrar)
        val ex = assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals("last", ex.message)
    }

    @Test
    fun `does not call subsequent handlers after first success`() = runTest {
        val second = SucceedingHandler("second")
        val registrar = object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.register(SucceedingHandler("primary") otherwise second)
            }
        }
        val m = mediator(registrar = registrar)
        m.send(PingQuery("x"))
        assertEquals(0, second.callCount)
    }
}
