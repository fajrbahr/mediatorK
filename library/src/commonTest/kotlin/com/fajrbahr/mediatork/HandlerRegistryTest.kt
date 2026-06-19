package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.handler.RequestExceptionHandler
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HandlerRegistryTest {

    // ── register / resolve ─────────────────────────────────────────────────────

    @Test
    fun `register adds handler that can be resolved`() = runTest {
        val registry = HandlerRegistry()
        registry.register(PingHandler())
        val request = PingQuery("x")
        val handler = registry.resolveHandler(request)
        assertEquals("pong:x", handler.handle(object : Mediator {
            override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult = TODO()
            override suspend fun <T : Notification> publish(notification: T) = TODO()
            override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) = TODO()
            override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): kotlinx.coroutines.flow.Flow<T> = TODO()
        }, RequestContext(), request))
    }

    @Test
    fun `hasHandler returns true after registration`() {
        val registry = HandlerRegistry()
        registry.register(PingHandler())
        assertTrue(registry.hasHandler(PingQuery::class))
    }

    @Test
    fun `hasHandler returns false before registration`() {
        val registry = HandlerRegistry()
        assertFalse(registry.hasHandler(PingQuery::class))
    }

    @Test
    fun `register replaces previously registered handler for same type`() = runTest {
        val registry = HandlerRegistry()
        val first = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) =
                "first"
        }
        val second = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) =
                "second"
        }
        registry.register(first)
        registry.register(second)
        val m = MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
            override fun register(r: HandlerRegistry) {
                r.register(second)
            }
        }))
        assertEquals("second", m.send(PingQuery("x")))
    }

    @Test
    fun `resolveHandler throws MissingHandlerException when absent`() {
        val registry = HandlerRegistry()
        assertFailsWith<MissingHandlerException> { registry.resolveHandler(PingQuery("x")) }
    }

    // ── notification handlers ─────────────────────────────────────────────────

    @Test
    fun `registerNotification adds handler`() {
        val registry = HandlerRegistry()
        val h = RecordingNotificationHandler()
        registry.registerNotification(h)
        val resolved = registry.resolveNotificationHandlers(PingNotification("x"))
        assertTrue(resolved.contains(h))
    }

    @Test
    fun `registerNotification allows multiple handlers for same type`() {
        val registry = HandlerRegistry()
        val h1 = RecordingNotificationHandler()
        val h2 = RecordingNotificationHandler()
        registry.registerNotification(h1)
        registry.registerNotification(h2)
        val resolved = registry.resolveNotificationHandlers(PingNotification("x"))
        assertEquals(2, resolved.size)
    }

    @Test
    fun `resolveNotificationHandlers returns empty list when none registered`() {
        val registry = HandlerRegistry()
        val resolved = registry.resolveNotificationHandlers(PingNotification("x"))
        assertTrue(resolved.isEmpty())
    }

    // ── exception handlers ────────────────────────────────────────────────────

    @Test
    fun `registerExceptionHandler resolves for matching request and exception`() {
        val registry = HandlerRegistry()
        val exHandler = object : RequestExceptionHandler<PingQuery, String, RuntimeException> {
            override suspend fun handle(
                requestContext: RequestContext,
                request: PingQuery,
                exception: RuntimeException
            ) = "handled"
        }
        registry.registerExceptionHandler(PingQuery::class, RuntimeException::class, exHandler)
        val resolved = registry.resolveExceptionHandler(PingQuery("x"), RuntimeException("err"))
        assertNotNull(resolved)
    }

    @Test
    fun `resolveExceptionHandler returns null for unregistered combination`() {
        val registry = HandlerRegistry()
        val resolved = registry.resolveExceptionHandler(PingQuery("x"), RuntimeException("err"))
        assertNull(resolved)
    }

    // ── DSL operators ─────────────────────────────────────────────────────────

    @Test
    fun `unaryPlus registers request handler`() = runTest {
        val m = MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                with(registry) { +PingHandler() }
            }
        }))
        assertEquals("pong:y", m.send(PingQuery("y")))
    }

    @Test
    fun `unaryPlus registers notification handler`() = runTest {
        val h = RecordingNotificationHandler()
        val m = MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                with(registry) { +h }
            }
        }))
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), h.received)
    }

    @Test
    fun `scope block groups registrations`() = runTest {
        val m = MediatorFactory.create(registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) {
                registry.scope {
                    register(PingHandler())
                    register(AddHandler())
                }
            }
        }))
        assertEquals("pong:z", m.send(PingQuery("z")))
        assertEquals(5, m.send(AddCommand(2, 3)))
    }

    // ── chaining ──────────────────────────────────────────────────────────────

    @Test
    fun `register returns registry for chaining`() {
        val registry = HandlerRegistry()
        val returned = registry.register(PingHandler())
        assertSame(registry, returned)
    }

    @Test
    fun `registerNotification returns registry for chaining`() {
        val registry = HandlerRegistry()
        val h = RecordingNotificationHandler()
        val returned = registry.registerNotification(h)
        assertSame(registry, returned)
    }
}
