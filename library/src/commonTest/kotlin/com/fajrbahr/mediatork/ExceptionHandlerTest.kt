package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.handler.RequestExceptionHandler
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
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
    fun `PRE behavior exception propagates and is NOT caught by exception handler`() = runTest {
        val pre = object : PipelineBehavior {
            override val tag = PipelineBehavior.Tag.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult = throw IllegalStateException("pre-fail")
        }
        val exHandler = object : RequestExceptionHandler<PingQuery, String, IllegalStateException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: IllegalStateException,
            ) = "should-not-reach"
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("handler-fail")))
                    registry.registerExceptionHandler(PingQuery::class, IllegalStateException::class, exHandler)
                }
            }),
            pipelineBehaviors = listOf(pre)
        )
        // PRE behavior exceptions bypass the exception handler
        assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `POST behavior runs with recovered result after exception handler`() = runTest {
        var postResponse: Any? = "not-set"
        val post = object : PipelineBehavior {
            override val tag = PipelineBehavior.Tag.Post
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { val r = next(request); postResponse = r; return r }
        }
        val exHandler = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException,
            ) = "recovered"
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("boom")))
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, exHandler)
                }
            }),
            pipelineBehaviors = listOf(post)
        )
        m.send(PingQuery("x"))
        assertEquals("recovered", postResponse)
    }

    @Test
    fun `exception handler has access to request context from PRE behavior`() = runTest {
        var contextValue: String? = null
        val pre = object : PipelineBehavior {
            override val tag = PipelineBehavior.Tag.Pre
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult { requestContext.put("trace", "t-99"); return next(request) }
        }
        val exHandler = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ): String { contextValue = requestContext.getMetaDate("trace"); return "handled" }
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingPingHandler(RuntimeException("err")))
                    registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, exHandler)
                }
            }),
            pipelineBehaviors = listOf(pre)
        )
        m.send(PingQuery("x"))
        assertEquals("t-99", contextValue)
    }
}
