package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExceptionHandlerTest {

    private fun failingPingHandler(exception: Throwable) = object : RequestHandler<PingQuery, String> {
        override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
            throw exception
    }

    @Test
    fun `exception handler converts exception to response`() = runTest {
        val exHandler = object : RequestExceptionHandler<PingQuery, String, IllegalStateException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: IllegalStateException
            ) =
                "recovered"
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(IllegalStateException("boom")))
                    registry.registerExceptionHandler(PingQuery::class, IllegalStateException::class, exHandler)
                }
            })
        )
        assertEquals("recovered", m.send(PingQuery("x")))
    }

    @Test
    fun `exception handler receives request and context`() = runTest {
        var capturedRequest: PingQuery? = null
        val exHandler = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ): String {
                capturedRequest = request
                return "ok"
            }
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("err")))
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, exHandler)
                }
            })
        )
        m.send(PingQuery("hello"))
        assertEquals(PingQuery("hello"), capturedRequest)
    }

    @Test
    fun `unregistered exception type propagates`() = runTest {
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(IllegalArgumentException("unhandled")))
                }
            })
        )
        assertFailsWith<IllegalArgumentException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `first matching exception handler wins when multiple registered`() = runTest {
        val first = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ) = "first"
        }
        val second = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ) = "second"
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("err")))
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, first)
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, second)
                }
            })
        )
        assertEquals("first", m.send(PingQuery("x")))
    }

    @Test
    fun `exception handler matches subclass of registered exception type`() = runTest {
        val exHandler = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ) =
                "caught-subclass"
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(IllegalStateException("sub")))
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, exHandler)
                }
            })
        )
        assertEquals("caught-subclass", m.send(PingQuery("x")))
    }

    @Test
    fun `exception handler for different request type does not intercept`() = runTest {
        val addExHandler = object : RequestExceptionHandler<AddCommand, Int, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: AddCommand,
                exception: RuntimeException
            ) = -1
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("err")))
                    registry.register(AddHandler())
                    registry.registerExceptionHandler(AddCommand::class, RuntimeException::class, addExHandler)
                }
            })
        )
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `exception handler has access to request context from pre-processor`() = runTest {
        var contextValue: String? = null
        val pre = object : RequestPreProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                requestContext.put("trace", "t-99")
            }
        }
        val exHandler = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ): String {
                contextValue = requestContext.getMetaDate("trace")
                return "handled"
            }
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("err")))
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, exHandler)
                }
            }),
            preProcessors = listOf(pre)
        )
        m.send(PingQuery("x"))
        assertEquals("t-99", contextValue)
    }
}
