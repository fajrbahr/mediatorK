package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.MissingHandlerException
import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FakeMediatorTest {

    @Test
    fun `send dispatches to registered handler`() = runTest {
        val mediator = FakeMediator {
            handler(GetUserHandler())
        }
        assertEquals("user:42", mediator.send(GetUserQuery("42")))
    }

    @Test
    fun `send routes to correct handler among multiple`() = runTest {
        val mediator = FakeMediator {
            handler(GetUserHandler())
            handler(CreateOrderHandler())
        }
        assertEquals("user:1", mediator.send(GetUserQuery("1")))
        assertEquals("order:ORD-1", mediator.send(CreateOrderCommand("ORD-1")))
    }

    @Test
    fun `send throws when no handler registered`() = runTest {
        val mediator = FakeMediator {}
        assertFailsWith<MissingHandlerException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `register adds handler after construction`() = runTest {
        val mediator = FakeMediator {}
        mediator.register<GetUserQuery, String>(GetUserHandler())
        assertEquals("user:99", mediator.send(GetUserQuery("99")))
    }

    @Test
    fun `publish invokes notification handler`() = runTest {
        val handler = OrderPlacedHandler()
        val mediator = FakeMediator {
            registerNotification(handler)
        }
        mediator.publish(OrderPlacedEvent("ORD-1"))
        assertEquals(listOf("ORD-1"), handler.received)
    }

    @Test
    fun `stream dispatches to registered stream handler`() = runTest {
        val mediator = FakeMediator {
            registerStream(StreamItemsHandler())
        }
        val items = mediator.stream(StreamItemsQuery("item")).toList()
        assertEquals(listOf("item-1", "item-2", "item-3"), items)
    }

    @Test
    fun `registerStream adds stream handler after construction`() = runTest {
        val mediator = FakeMediator {}
        mediator.registerStream<StreamItemsQuery, String>(StreamItemsHandler())
        val items = mediator.stream(StreamItemsQuery("late")).toList()
        assertEquals(listOf("late-1", "late-2", "late-3"), items)
    }

    @Test
    fun `accepts registrars list`() = runTest {
        val registrar = object : MediatorRegistrar {
            override fun register(registry: com.fajrbahr.mediatork.HandlerRegistry) {
                registry.register(GetUserHandler())
            }
        }
        val mediator = FakeMediator(registrars = listOf(registrar))
        assertEquals("user:from-registrar", mediator.send(GetUserQuery("from-registrar")))
    }

    @Test
    fun `accepts pipeline behaviors`() = runTest {
        val log = mutableListOf<String>()
        val behavior = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "before"
                val result = next(request)
                log += "after"
                return result
            }
        }
        val mediator = FakeMediator(pipelineBehaviors = listOf(behavior)) {
            handler(GetUserHandler())
        }
        mediator.send(GetUserQuery("1"))
        assertEquals(listOf("before", "after"), log)
    }

    @Test
    fun `unaryPlus DSL registers handlers`() = runTest {
        val mediator = FakeMediator {
            +GetUserHandler()
            +CreateOrderHandler()
        }
        assertEquals("user:a", mediator.send(GetUserQuery("a")))
        assertEquals("order:b", mediator.send(CreateOrderCommand("b")))
    }
}

class FakeHandlerTest {

    @Test
    fun `fakeHandler creates a working request handler`() = runTest {
        val handler = fakeHandler<GetUserQuery, String> { _, _, request ->
            "fake:${request.id}"
        }
        val mediator = FakeMediator { handler(handler) }
        assertEquals("fake:123", mediator.send(GetUserQuery("123")))
    }

    @Test
    fun `fakeHandler receives mediator and context`() = runTest {
        val handler = fakeHandler<GetUserQuery, String> { mediator, ctx, request ->
            ctx.put("seen", true)
            "ok"
        }
        val mediator = FakeMediator { handler(handler) }
        assertEquals("ok", mediator.send(GetUserQuery("1")))
    }

    @Test
    fun `fakeNotificationHandler creates a working notification handler`() = runTest {
        val captured = mutableListOf<String>()
        val handler = fakeNotificationHandler<OrderPlacedEvent> { captured += it.orderId }
        val mediator = FakeMediator {
            registerNotification(handler)
        }
        mediator.publish(OrderPlacedEvent("ORD-1"))
        mediator.publish(OrderPlacedEvent("ORD-2"))
        assertEquals(listOf("ORD-1", "ORD-2"), captured)
    }
}

class CaptureNotificationsTest {

    @Test
    fun `captureNotifications returns published notifications`() = runTest {
        val mediator = FakeMediator {}
        val events = mediator.captureNotifications<OrderPlacedEvent>()
        mediator.publish(OrderPlacedEvent("ORD-1"))
        mediator.publish(OrderPlacedEvent("ORD-2"))
        assertEquals(2, events.size)
        assertEquals("ORD-1", events[0].orderId)
        assertEquals("ORD-2", events[1].orderId)
    }

    @Test
    fun `captureNotifications only captures matching type`() = runTest {
        val mediator = FakeMediator {
            registerNotification(object : NotificationHandler<UserDeletedEvent> {
                override suspend fun handle(notification: UserDeletedEvent) = Unit
            })
        }
        val orderEvents = mediator.captureNotifications<OrderPlacedEvent>()
        mediator.publish(OrderPlacedEvent("ORD-1"))
        mediator.publish(UserDeletedEvent("U-1"))
        assertEquals(1, orderEvents.size)
        assertEquals("ORD-1", orderEvents[0].orderId)
    }

    @Test
    fun `captureNotifications returns empty list when nothing published`() = runTest {
        val mediator = FakeMediator {}
        val events = mediator.captureNotifications<OrderPlacedEvent>()
        assertTrue(events.isEmpty())
    }
}
