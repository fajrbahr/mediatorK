package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestExceptionHandler
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import kotlinx.coroutines.test.runTest
import kotlin.test.*

fun mediator(block: HandlerRegistry.() -> Unit): Mediator =
    MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
        override fun register(registry: HandlerRegistry) {
            registry.block()
        }
    }))

// ── Tests ─────────────────────────────────────────────────────────────────────

class MediatorTest {

    @Test
    fun `send returns handler result`() = runTest {
        val m = mediator { register(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `send routes to correct handler among many`() = runTest {
        val m = mediator {
            register(PingHandler())
            register(AddHandler())
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `send with Request_Unit returns Unit`() = runTest {
        val handler = NoResultHandler()
        val m = mediator { register(handler) }
        m.send(NoResultCommand("id-1"))
        assertEquals("id-1", handler.lastId)
    }

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val m = mediator { }
        assertFailsWith<MissingHandlerException> {
            m.send(PingQuery("x"))
        }
    }

    @Test
    fun `MissingHandlerException message includes request type name`() = runTest {
        val m = mediator { }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("PingQuery"))
    }

    @Test
    fun `publish delivers notification to all registered handlers`() = runTest {
        val h1 = RecordingNotificationHandler()
        val h2 = RecordingNotificationHandler()
        val m = MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry registerNotification h1
                registry registerNotification h2
            }
        }))
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), h1.received)
        assertEquals(listOf("hello"), h2.received)
    }

    @Test
    fun `publish with no handlers throws MissingNotificationHandlerException`() = runTest {
        val m = mediator { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("silent"))
        }
    }

    @Test
    fun `pipeline behavior wraps handler in order`() = runTest {
        val log = mutableListOf<String>()

        val outer = object : PipelineBehavior {
            override val order = -10
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "outer-before"
                return next(request).also { log += "outer-after" }
            }
        }

        val inner = object : PipelineBehavior {
            override val order = 10
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "inner-before"
                return next(request).also { log += "inner-after" }
            }
        }

        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(PingHandler())
                }
            }),
            pipelineBehaviors = listOf(inner, outer),
        )

        m.send(PingQuery("x"))
        assertEquals(listOf("outer-before", "inner-before", "inner-after", "outer-after"), log)
    }

    @Test
    fun `pipeline behavior with appliesTo=false is skipped`() = runTest {
        var ran = false
        val selective = object : PipelineBehavior {
            override fun appliesTo(request: Request<*>) = false
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                ran = true; return next(request)
            }
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(PingHandler())
                }
            }),
            pipelineBehaviors = listOf(selective),
        )
        m.send(PingQuery("x"))
        assertFalse(ran)
    }

    @Test
    fun `pre-processor runs before handler and can populate context`() = runTest {
        var contextValue: String? = null

        val pre = object : RequestPreProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>) {
                requestContext.put("key", "injected")
            }
        }

        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                contextValue = requestContext.getMetaDate("key")
                return "ok"
            }
        }

        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(handler)
                }
            }),
            preProcessors = listOf(pre),
        )
        m.send(PingQuery("x"))
        assertEquals("injected", contextValue)
    }

    @Test
    fun `post-processor runs after handler and receives response`() = runTest {
        var captured: Any? = "not-set"

        val post = object : RequestPostProcessor {
            override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
                captured = response
            }
        }

        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(PingHandler())
                }
            }),
            postProcessors = listOf(post),
        )
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }

    @Test
    fun `exception handler converts exception to response`() = runTest {
        val failingHandler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String =
                throw IllegalStateException("boom")
        }

        val exHandler = object : RequestExceptionHandler<PingQuery, String, IllegalStateException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: IllegalStateException,
            ) = "recovered"
        }

        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.register(failingHandler)
                    registry.registerExceptionHandler(PingQuery::class, IllegalStateException::class, exHandler)
                }
            })
        )

        assertEquals("recovered", m.send(PingQuery("x")))
    }
}
