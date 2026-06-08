package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.*

// ── Fixtures ─────────────────────────────────────────────────────────────────

data class PingQuery(val value: String) : Request<String>
data class AddCommand(val a: Int, val b: Int) : Request<Int>
data class NoResultCommand(val id: String) : Request.Unit
data class PingNotification(val message: String) : Notification

class PingHandler : RequestHandler<PingQuery, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) =
        "pong:${request.value}"
}

class AddHandler : RequestHandler<AddCommand, Int> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: AddCommand) =
        request.a + request.b
}

class NoResultHandler : RequestHandler<NoResultCommand, Unit> {
    var lastId: String? = null
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: NoResultCommand) {
        lastId = request.id
    }
}

class RecordingNotificationHandler : NotificationHandler<PingNotification> {
    val received = mutableListOf<String>()
    override suspend fun handle(notification: PingNotification) {
        received += notification.message
    }
}

fun mediator(block: HandlerRegistry.() -> Unit): Mediator =
    MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
        override fun register(registry: HandlerRegistry) { registry.block() }
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
    fun `publish with no handlers does not throw`() = runTest {
        val m = mediator { }
        m.publish(PingNotification("silent")) // must not throw
    }

    @Test
    fun `pipeline behavior wraps handler in order`() = runTest {
        val log = mutableListOf<String>()

        val outer = object : PipelineBehavior {
            override val order = -10
            override suspend fun <TReq : Request<TRes>, TRes> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TReq, TRes>,
                request: TReq,
            ): TRes {
                log += "outer-before"
                return next(request).also { log += "outer-after" }
            }
        }

        val inner = object : PipelineBehavior {
            override val order = 10
            override suspend fun <TReq : Request<TRes>, TRes> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TReq, TRes>,
                request: TReq,
            ): TRes {
                log += "inner-before"
                return next(request).also { log += "inner-after" }
            }
        }

        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) { registry.register(PingHandler()) }
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
            override suspend fun <TReq : Request<TRes>, TRes> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TReq, TRes>,
                request: TReq,
            ): TRes { ran = true; return next(request) }
        }
        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) { registry.register(PingHandler()) }
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
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                contextValue = requestContext.getMetaDate("key")
                return "ok"
            }
        }

        val m = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) { registry.register(handler) }
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
                override fun register(registry: HandlerRegistry) { registry.register(PingHandler()) }
            }),
            postProcessors = listOf(post),
        )
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }

    @Test
    fun `exception handler converts exception to response`() = runTest {
        val failingHandler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
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
