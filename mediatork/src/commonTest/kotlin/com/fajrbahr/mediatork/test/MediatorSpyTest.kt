package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// ── Fixtures ──────────────────────────────────────────────────────────────────

data class GetUserQuery(val id: String) : Request<String>
data class CreateOrderCommand(val id: String) : Request<String>
data class OrderPlacedEvent(val orderId: String) : Notification
data class UserDeletedEvent(val userId: String) : Notification

// ── Tests ─────────────────────────────────────────────────────────────────────

class MediatorSpyTest {

    private fun buildSpy(): MediatorSpy {
        val fake = FakeMediator {
            register(object : RequestHandler<GetUserQuery, String> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: GetUserQuery) =
                    "user:${request.id}"
            })
            register(object : RequestHandler<CreateOrderCommand, String> {
                override suspend fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: CreateOrderCommand
                ) =
                    "order:${request.id}"
            })
            registerNotification(object : NotificationHandler<OrderPlacedEvent> {
                override suspend fun handle(notification: OrderPlacedEvent) = Unit
            })
            registerNotification(object : NotificationHandler<UserDeletedEvent> {
                override suspend fun handle(notification: UserDeletedEvent) = Unit
            })
        }
        return MediatorSpy(fake)
    }

    @Test
    fun `delegates send to the real mediator`() = runTest {
        val spy = buildSpy()
        assertEquals("user:42", spy.send(GetUserQuery("42")))
    }

    @Test
    fun `records sent requests`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.send(GetUserQuery("2"))
        spy.send(CreateOrderCommand("ORD-1"))
        assertEquals(3, spy.sentRequests.size)
    }

    @Test
    fun `sentOf filters by type`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.send(CreateOrderCommand("ORD-1"))
        spy.send(GetUserQuery("2"))
        assertEquals(2, spy.sentOf<GetUserQuery>().size)
        assertEquals(1, spy.sentOf<CreateOrderCommand>().size)
    }

    @Test
    fun `records published notifications`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.publish(UserDeletedEvent("U-1"))
        assertEquals(2, spy.publishedNotifications.size)
    }

    @Test
    fun `publishedOf filters by type`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.publish(OrderPlacedEvent("ORD-2"))
        spy.publish(UserDeletedEvent("U-1"))
        assertEquals(2, spy.publishedOf<OrderPlacedEvent>().size)
        assertEquals(1, spy.publishedOf<UserDeletedEvent>().size)
    }

    @Test
    fun `assertSent passes when request was sent`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.assertSent<GetUserQuery>()
    }

    @Test
    fun `assertSent fails when request was not sent`() = runTest {
        val spy = buildSpy()
        assertFailsWith<AssertionError> { spy.assertSent<GetUserQuery>() }
    }

    @Test
    fun `assertNotSent passes when request was not sent`() = runTest {
        val spy = buildSpy()
        spy.assertNotSent<GetUserQuery>()
    }

    @Test
    fun `assertNotSent fails when request was sent`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        assertFailsWith<AssertionError> { spy.assertNotSent<GetUserQuery>() }
    }

    @Test
    fun `assertPublished passes when notification was published`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.assertPublished<OrderPlacedEvent>()
    }

    @Test
    fun `assertPublished fails when notification was not published`() = runTest {
        val spy = buildSpy()
        assertFailsWith<AssertionError> { spy.assertPublished<OrderPlacedEvent>() }
    }

    @Test
    fun `assertNotPublished passes when notification was not published`() = runTest {
        val spy = buildSpy()
        spy.assertNotPublished<OrderPlacedEvent>()
    }

    @Test
    fun `assertNotPublished fails when notification was published`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        assertFailsWith<AssertionError> { spy.assertNotPublished<OrderPlacedEvent>() }
    }

    @Test
    fun `assertSentCount passes with exact count`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.send(GetUserQuery("2"))
        spy.assertSentCount<GetUserQuery>(2)
    }

    @Test
    fun `assertSentCount fails with wrong count`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        assertFailsWith<AssertionError> { spy.assertSentCount<GetUserQuery>(3) }
    }

    @Test
    fun `assertPublishedCount passes with exact count`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.publish(OrderPlacedEvent("ORD-2"))
        spy.assertPublishedCount<OrderPlacedEvent>(2)
    }

    @Test
    fun `reset clears all recorded calls`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.reset()
        assertEquals(0, spy.sentRequests.size)
        assertEquals(0, spy.publishedNotifications.size)
    }

    @Test
    fun `spy preserves request values`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("user-99"))
        assertEquals("user-99", spy.sentOf<GetUserQuery>().first().id)
    }
}
