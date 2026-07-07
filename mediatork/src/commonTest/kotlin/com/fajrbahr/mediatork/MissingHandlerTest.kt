package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.notification.SequentialNotificationPublisher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MissingHandlerTest {

    // ── Requests / Notifications ──────────────────────────────────────────────

    private data object PingRequest : Request<String>
    private data object OtherRequest : Request<Int>
    private data object OrderPlaced : Notification
    private data object UserCreated : Notification

    // ── send() with no handler registered ────────────────────────────────────

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val mediator = MediatorFactory.create()
        assertFailsWith<MissingHandlerException> {
            mediator.send(PingRequest)
        }
    }

    @Test
    fun `MissingHandlerException message contains request type name`() = runTest {
        val mediator = MediatorFactory.create()
        val ex = assertFailsWith<MissingHandlerException> {
            mediator.send(PingRequest)
        }
        assertEquals(ex.message?.contains("PingRequest"), true)
    }

    @Test
    fun `send succeeds after handler is registered`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object : RequestHandler<PingRequest, String> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: PingRequest,
                        ) = "pong"
                    }
                }
            })
        )
        assertEquals("pong", mediator.send(PingRequest))
    }

    @Test
    fun `only unregistered request throws - registered one succeeds`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object : RequestHandler<PingRequest, String> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: PingRequest,
                        ) = "pong"
                    }
                }
            })
        )

        assertEquals("pong", mediator.send(PingRequest))

        assertFailsWith<MissingHandlerException> {
            mediator.send(OtherRequest)
        }
    }

    // ── publish() with no notification handler ────────────────────────────────

    @Test
    fun `publish throws when no notification handler registered`() = runTest {
        val mediator = MediatorFactory.create()
        assertFailsWith<MissingNotificationHandlerException> {
            mediator.publish(OrderPlaced)
        }
    }

    @Test
    fun `notification handler is invoked when registered`() = runTest {
        val received = mutableListOf<Notification>()
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry registerNotification object : NotificationHandler<OrderPlaced> {
                        override suspend fun handle(notification: OrderPlaced) {
                            received += notification
                        }
                    }
                }
            })
        )

        mediator.publish(OrderPlaced)
        assertEquals(1, received.size)
    }

    @Test
    fun `multiple handlers all invoked for same notification`() = runTest {
        val log = mutableListOf<String>()
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry registerNotification object : NotificationHandler<OrderPlaced> {
                        override suspend fun handle(notification: OrderPlaced) {
                            log += "handler-1"
                        }
                    }
                    registry registerNotification object : NotificationHandler<OrderPlaced> {
                        override suspend fun handle(notification: OrderPlaced) {
                            log += "handler-2"
                        }
                    }
                }
            })
        )

        mediator.publish(OrderPlaced)
        assertEquals(2, log.size)
        assertTrue(log.containsAll(listOf("handler-1", "handler-2")))
    }

    @Test
    fun `notification handler not invoked for different notification type`() = runTest {
        val received = mutableListOf<Notification>()
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry registerNotification object : NotificationHandler<OrderPlaced> {
                        override suspend fun handle(notification: OrderPlaced) {
                            received += notification
                        }
                    }
                }
            })
        )

        assertFailsWith<MissingNotificationHandlerException> {
            mediator.publish(UserCreated) // different type — handler must not fire
        }
        assertTrue(received.isEmpty())
    }

    @Test
    fun `handlers for different notifications are independent`() = runTest {
        val log = mutableListOf<String>()
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry registerNotification object : NotificationHandler<OrderPlaced> {
                        override suspend fun handle(notification: OrderPlaced) {
                            log += "order"
                        }
                    }
                    registry registerNotification object : NotificationHandler<UserCreated> {
                        override suspend fun handle(notification: UserCreated) {
                            log += "user"
                        }
                    }
                }
            }),
            notificationPublisher = SequentialNotificationPublisher()
        )

        mediator.publish(OrderPlaced)
        assertEquals(listOf("order"), log)

        mediator.publish(UserCreated)
        assertEquals(listOf("order", "user"), log)
    }

    // ── MissingStreamHandlerException with registered list ────────────────────

    private data class StreamTypeA(val n: Int) : StreamRequest<Int>
    private data class StreamTypeB(val n: Int) : StreamRequest<Int>

    @Test
    fun `MissingStreamHandlerException message includes registered stream type`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry registerStream object : StreamRequestHandler<StreamTypeA, Int> {
                        override fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: StreamTypeA,
                        ): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
                    }
                }
            }),
            verifyHandlers = false,
        )
        val ex = assertFailsWith<MissingStreamHandlerException> { mediator.stream(StreamTypeB(1)) }
        assertTrue(ex.message!!.contains("StreamTypeA"), "expected 'StreamTypeA' in: ${ex.message}")
    }

    // ── SilentMissingRequestHandler ───────────────────────────────────────────

    @Test
    fun `SilentMissingRequestHandler returns default without throwing`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = emptyList(),
            missingRequestHandler = com.fajrbahr.mediatork.handler.SilentMissingRequestHandler("default-value"),
        )
        val result = mediator.send(PingRequest)
        assertEquals("default-value", result)
    }

    @Test
    fun `custom missingRequestHandler is invoked when no handler registered`() = runTest {
        var called = false
        val mediator = MediatorFactory.create(
            registrars = emptyList(),
            missingRequestHandler = object : RequestHandler<Request<Any?>, Any?> {
                override suspend fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: Request<Any?>,
                ): Any? { called = true; return null }
            },
        )
        mediator.send(PingRequest)
        assertTrue(called)
    }

    // ── MediatorFactory.create(registry) overload ─────────────────────────────

    @Test
    fun `create with registry overload builds working mediator`() = runTest {
        val registry = HandlerRegistry()
        registry register object : RequestHandler<PingRequest, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingRequest) = "pong"
        }
        val m = MediatorFactory.create(registry = registry)
        assertEquals("pong", m.send(PingRequest))
    }

    @Test
    fun `create with registry overload respects registered notification handlers`() = runTest {
        val log = mutableListOf<String>()
        val registry = HandlerRegistry()
        registry registerNotification object : NotificationHandler<OrderPlaced> {
            override suspend fun handle(notification: OrderPlaced) { log += "fired" }
        }
        val m = MediatorFactory.create(registry = registry)
        m.publish(OrderPlaced)
        assertEquals(listOf("fired"), log)
    }
}